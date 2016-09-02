/*
 * Copyright 2016 David Crosson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dummy

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.{ functions => F }
import org.apache.spark.sql.hive.HiveContext

object Dummy {
  val userName = util.Properties.userName
  val message = s"Hello ${userName}"

  def main(args: Array[String]) {
    println(message)
    val conf =
      new SparkConf()
        .setAppName("Simple Application")
        .setMaster("local")
        .set("spark.ui.showConsoleProgress", "false")
    implicit val sc = new SparkContext(conf)
    implicit val sqlc = new SQLContext(sc)
    implicit val hivec = new HiveContext(sc)


    val pwd=new java.io.File(".").getAbsolutePath

    val series_raw = sqlc
      .read
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .option("delimiter", ";")
      .load(s"$pwd/generated-file-1.csv")

    import sqlc.implicits._

    import org.apache.spark.sql.functions.unix_timestamp

    val ts = unix_timestamp($"DateTime", "yyyy/MM/dd HH:mm:ss.S").cast("timestamp")

    val series =
      series_raw
        .withColumnRenamed("rps apaches hit rate", "apachesHitRate")
        .withColumnRenamed("tomcats tomcats hit rate", "tomcatsHitRate")
        .withColumn("timestamp", ts)
        .drop("DateTime")
        .withColumn("apachesHitRate", $"apachesHitRate".cast("double"))
        .withColumn("tomcatsHitRate", $"tomcatsHitRate".cast("double"))
        .withColumn("year", F.year($"timestamp"))
        .withColumn("month", F.month($"timestamp"))
        .withColumn("day", F.dayofmonth($"timestamp"))
        .withColumn("hour", F.hour($"timestamp"))
        .withColumn("minute", F.minute($"timestamp"))
        .drop("timestamp")

    series.printSchema()

    series.show(10)

    import org.apache.spark.ml.feature.VectorAssembler

    val vect_assembler = new VectorAssembler()
//      .setInputCols(Array("day", "hour", "minute")) // Attention risque de Plantage de la regression si pas de variance sur une feature avec le LinearRegression
      .setInputCols(Array("year", "month", "day", "hour", "minute")) 
      .setOutputCol("features")

    val df_train = series.filter($"day" <= 22)
    val df_test = series.filter($"day" > 22)

    val df_train_transformed = vect_assembler.transform(df_train)
    val df_test_transformed = vect_assembler.transform(df_test)

    import org.apache.spark.ml.regression._
    
    val lr =
      //new IsotonicRegression()
      //new LinearRegression()
      new GBTRegressor() // Gradient boosting - GOOD BEHAVIOR WITH THIS ONE
        .setLabelCol("apachesHitRate")
        .setFeaturesCol("features")

    val model = lr.fit(df_train_transformed)
    
    val df_test_predict = model.transform(df_test_transformed)
    
    df_test_predict.show(100)
    
    
    import org.apache.spark.ml.evaluation.RegressionEvaluator
    
    val evaluator = 
      new RegressionEvaluator()
        .setLabelCol(lr.getLabelCol)
        .setPredictionCol(lr.getPredictionCol)
        .setMetricName("r2") // La valeur d'évaluation dépendant de l'algo d'évaluation choisi, r2, mse, rmse, ...

    val evaluation = evaluator.evaluate(df_test_predict)
    println(s" evaluation $evaluation")

    
    
    sc.stop()
  }

}
