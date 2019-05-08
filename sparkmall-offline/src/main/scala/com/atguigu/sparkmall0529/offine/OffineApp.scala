package com.atguigu.sparkmall0529.offine


import com.atguigu.sparkmall0529.common.bean.UserVisitAction
import com.atguigu.sparkmall0529.common.util.ConfigUtil
import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.sparkmall0529.offine.app._
import com.atguigu.sparkmall0529.offine.bean.CategoryCountInfo
import org.apache.commons.configuration2.FileBasedConfiguration
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, sql}
import org.apache.spark.sql.{SaveMode, SparkSession}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Created by kelvin on 2019/5/6.
  */
object OffineApp {

  def main(args: Array[String]): Unit = {

    val sparkConf: SparkConf = new SparkConf().setAppName("offline").setMaster("local[*]")
    val sparkSession = SparkSession.builder().config(sparkConf).enableHiveSupport().getOrCreate()


    val taskId: String = UUID.randomUUID().toString

    val conditionConfig: FileBasedConfiguration = ConfigUtil("conditions.properties").config

    val conditionJsonString: String = conditionConfig.getString("condition.params.json")

    val conditionJsonObj: JSONObject = JSON.parseObject(conditionJsonString)

    println(conditionJsonObj.getString("startDate"))

    //1 根据过滤条件  取出符合的日志RDD集合  成为RDD[UserVisitAction]

    val userActionRDD: RDD[UserVisitAction] = readUserVisitActionRDD(sparkSession,conditionJsonObj)
    userActionRDD.cache()

    //2 以session为key 进行聚合 =>> RDD[session,Iterable[UserVisitAction]]
    val sessionActionsRDD: RDD[(String, Iterable[UserVisitAction])] = userActionRDD.map(userAction => {
      (userAction.session_id, userAction)
    }).groupByKey()

    //需求一:统计出符合筛选条件的session中  访问时长在小于10s  10s以上各个范围内的session数量占比.访问步长小于等于5  和大于等于5次的占比
    SessionStatApp.statSession(sessionActionsRDD,sparkSession,taskId,conditionJsonString)
    println("需要一 完成!!")

    //需求二  按每小时session数量比例随机抽取1000个session
    sessionActionsRDD.count()
    SessionExtractorApp.extractSession(sessionActionsRDD, sparkSession, taskId)
    println("需求二 完成!!")

    //需求三  获取点击 下单和支付数量排名前10的品类
    val categoryTop10: List[CategoryCountInfo] = CategoryTop10App.statCategoryTop10(userActionRDD,sparkSession,taskId)
    println("需求三 完成！")

    // 需求四  Top10热门品类中  Top10 活跃Session统计
    CategorySessionApp.statCategoryTop10Session(categoryTop10,userActionRDD,sparkSession,taskId)
    println("需求四 完成!")

    // 需求五  页面单跳转化率统计
    PageConvertRateApp.calcPageConvertRate(userActionRDD,sparkSession,taskId,conditionJsonObj)
    println("需求五 完成!")

    // 需求六  每天各地区各城市各广告的点击流量实时统计
    AreaTop3ClickCountApp.statAreaTop3ClickCount(sparkSession)
    println("需求六 完成")

  }

    def readUserVisitActionRDD(sparkSession: SparkSession,conditionJsonObj:JSONObject):RDD[UserVisitAction]={

      var sql = " select v.* from user_visit_action v join user_info u on v.user_id=u.user_id where 1=1  "

      if (conditionJsonObj.getString("startDate") != null && conditionJsonObj.getString("startDate").length > 0) {
        sql += " and   date>= '" + conditionJsonObj.getString("startDate") + "'"
      }
      if (conditionJsonObj.getString("endDate") != null && conditionJsonObj.getString("endDate").length > 0) {
        sql += " and  date <='" + conditionJsonObj.getString("endDate") + "'"
      }

      if (conditionJsonObj.getString("startAge") != null && conditionJsonObj.getString("startAge").length > 0) {
        sql += " and  u.age >=" + conditionJsonObj.getString("startAge")
      }
      if (conditionJsonObj.getString("endAge") != null && conditionJsonObj.getString("endAge").length > 0) {
        sql += " and  u.age <=" + conditionJsonObj.getString("endAge")
      }
      println(sql)
      sparkSession.sql("use sparkmall2018");

      import sparkSession.implicits._
      sparkSession.sql(sql+ " limit 50").show
      sparkSession.sql(sql).as[UserVisitAction].rdd
    }




}
