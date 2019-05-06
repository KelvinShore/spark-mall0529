package com.atguigu.sparkmall0529.mock.util

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

object RandomNum {

  def apply(fromNum:Int,toNum:Int): Int =  {
    fromNum+ new Random().nextInt(toNum-fromNum+1)
  }

  //产生多个随机数组成字符串
  def multi(fromNum:Int,toNum:Int,amount:Int,delimiter:String,canRepeat:Boolean) ={
    if(canRepeat){
      val hitNums=new ListBuffer[Int]()
      while (  hitNums.size<amount ) {
        val randomNum = fromNum+ new Random().nextInt(toNum-fromNum+1)
        hitNums+=randomNum
      }
      hitNums.mkString(delimiter)
    }else{
      val hitNums=new mutable.HashSet[Int]()
      while (  hitNums.size<amount ) {
        val randomNum = fromNum+ new Random().nextInt(toNum-fromNum+1)
        hitNums+=randomNum
      }
      hitNums.mkString(delimiter)
    }
  }


}
