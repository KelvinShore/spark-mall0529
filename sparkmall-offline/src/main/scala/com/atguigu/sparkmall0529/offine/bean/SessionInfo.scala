package com.atguigu.sparkmall0529.offine.bean

/**
  * Created by kelvin on 2019/5/6.
  */

case class SessionInfo(taskId:String ,
                       sessionId:String ,
                       startTime:String ,
                       stepLength:Int,
                       visitLength:Long,
                       searchKeywords :String ,
                       clickProductIds:String,
                       orderProductIds:String,
                       payProductIds:String
                      ) {

}