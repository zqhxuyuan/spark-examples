package intro

import org.apache.spark.{SparkConf, SparkContext}

/**
 * No Need to start others, just run it as Scala Application on IDEA. you should setMaster, or else will get
 * Caused by: org.apache.spark.SparkException: A master URL must be set in your configuration at ...
 */
object SimpleApp {

  val conf = new SparkConf().setAppName("Simple Application").setMaster("local")
  val sc = new SparkContext(conf)

  def main(args: Array[String]): Unit = {
    simpleapp
  }

  def simpleapp{
    val logFile = "file:/Users/zhengqh/Soft/spark-1.4.0-bin-hadoop2.6/README.md"
    val logData = sc.textFile(logFile, 2).cache()
    val numAs = logData.filter(line => line.contains("a")).count()
    val numBs = logData.filter(line => line.contains("b")).count()
    println("Lines with a: %s, Lines with b: %s".format(numAs, numBs))
  }

  def dataguru {
    dataguru_parallelize
    dataguru_kv
    dataguru_file
    dataguru_sogou
  }

  //parallelize演示
  def dataguru_parallelize{
    val num=sc.parallelize(1 to 10)
    val doublenum = num.map(_*2)
    val threenum = doublenum.filter(_ % 3 == 0)
    threenum.collect
    threenum.toDebugString

    val num1=sc.parallelize(1 to 10,6)
    val doublenum1 = num1.map(_*2)
    val threenum1 = doublenum1.filter(_ % 3 == 0)
    threenum1.collect
    threenum1.toDebugString

    threenum.cache()
    val fournum = threenum.map(x=>x*x)
    fournum.collect
    fournum.toDebugString
    threenum.unpersist()

    num.reduce (_ + _)
    num.take(5)
    num.first
    num.count
    num.take(5).foreach(println)
  }

  //K-V演示
  def dataguru_kv {
    val kv1=sc.parallelize(List(("A",1),("B",2),("C",3),("A",4),("B",5)))
    kv1.sortByKey().collect //注意sortByKey的小括号不能省
    kv1.groupByKey().collect
    kv1.reduceByKey(_+_).collect

    val kv2=sc.parallelize(List(("A",4),("A",4),("C",3),("A",4),("B",5)))
    kv2.distinct.collect
    kv1.union(kv2).collect

    val kv3=sc.parallelize(List(("A",10),("B",20),("D",30)))
    kv1.join(kv3).collect
    kv1.cogroup(kv3).collect

    val kv4=sc.parallelize(List(List(1,2),List(3,4)))
    kv4.flatMap(x=>x.map(_+1)).collect
  }

  //文件读取演示
  def dataguru_file {
    val rdd1 = sc.textFile("hdfs://hadoop1:8000/dataguru/week2/directory/")
    rdd1.toDebugString
    val words=rdd1.flatMap(_.split(" "))
    val wordscount=words.map(x=>(x,1)).reduceByKey(_+_)
    wordscount.collect
    wordscount.toDebugString

    val rdd2 = sc.textFile("hdfs://hadoop1:8000/dataguru/week2/directory/*.txt")
    rdd2.flatMap(_.split(" ")).map(x=>(x,1)).reduceByKey(_+_).collect

    //gzip压缩的文件
    val rdd3 = sc.textFile("hdfs://hadoop1:8000/dataguru/week2/test.txt.gz")
    rdd3.flatMap(_.split(" ")).map(x=>(x,1)).reduceByKey(_+_).collect
  }

  //日志处理演示
  //http://download.labs.sogou.com/dl/q.html 完整版(2GB)：gz格式
  //访问时间\t用户ID\t[查询词]\t该URL在返回结果中的排名\t用户点击的顺序号\t用户点击的URL
  //SogouQ1.txt、SogouQ2.txt、SogouQ3.txt分别是用head -n 或者tail -n 从SogouQ数据日志文件中截取
  def dataguru_sogou {
    //搜索结果排名第1，但是点击次序排在第2的数据有多少?
    val rdd1 = sc.textFile("hdfs://hadoop1:8000/dataguru/data/SogouQ1.txt")
    val rdd2=rdd1.map(_.split("\t")).filter(_.length==6)
    rdd2.count()
    val rdd3=rdd2.filter(_(3).toInt==1).filter(_(4).toInt==2)
    rdd3.count()
    rdd3.toDebugString

    //session查询次数排行榜
    val rdd4=rdd2.map(x=>(x(1),1)).reduceByKey(_+_).map(x=>(x._2,x._1)).sortByKey(false).map(x=>(x._2,x._1))
    rdd4.toDebugString
    rdd4.saveAsTextFile("hdfs://hadoop1:8000/dataguru/week2/output1")

    //cache()演示
    //检查block命令：bin/hdfs fsck /dataguru/data/SogouQ3.txt -files -blocks -locations
    val rdd5 = sc.textFile("hdfs://hadoop1:8000/dataguru/data/SogouQ3.txt")
    rdd5.cache()
    rdd5.count()
    rdd5.count()  //比较时间

    //join演示
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
    case class Register (d: java.util.Date, uuid: String, cust_id: String, lat: Float,lng: Float)
    case class Click (d: java.util.Date, uuid: String, landing_page: Int)
    val reg = sc.textFile("hdfs://hadoop1:8000/dataguru/week2/join/reg.tsv").map(_.split("\t")).map(r => (r(1), Register(format.parse(r(0)), r(1), r(2), r(3).toFloat, r(4).toFloat)))
    val clk = sc.textFile("hdfs://hadoop1:8000/dataguru/week2/join/clk.tsv").map(_.split("\t")).map(c => (c(1), Click(format.parse(c(0)), c(1), c(2).trim.toInt)))
    reg.join(clk).take(2)
  }
}