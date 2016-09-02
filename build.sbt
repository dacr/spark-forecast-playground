name := "spark-forecast-playground"

version := "0.1"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-unchecked", "-deprecation" , "-feature", "-language:implicitConversions")

libraryDependencies ++= Seq(
   "org.apache.spark" %% "spark-core" % "1.6.1",
   "org.apache.spark" %% "spark-sql" % "1.6.1",
   "org.apache.spark" %% "spark-hive" % "1.6.1",
   "org.apache.spark" %% "spark-mllib" % "1.6.1",
   "com.databricks" %% "spark-csv" % "1.4.0",
   "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

initialCommands in console := """
  |import dummy._
  |import dummy.Dummy._
  |import org.apache.spark._
  |import org.apache.spark.SparkContext._
  |import org.apache.spark.sql._
  |import org.apache.spark.sql.{ functions => F }
  |import org.apache.spark.sql.hive.HiveContext
  |import org.apache.log4j._
  |PropertyConfigurator.configure("src/main/resources/log4j.properties")
  |val conf =
  |  new SparkConf()
  |    .setAppName("Simple Application")
  |    .setMaster("local")
  |implicit val sc = new SparkContext(conf)
  |implicit val sqlc = new SQLContext(sc)
  |implicit val hivec = new HiveContext(sc)
  |val sqlContext=sqlc
  |import sqlc.implicits._
  |""".stripMargin

