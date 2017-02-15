package fr.polytechnique.cmap.cnam

import java.io.File
import java.util.{Locale, TimeZone}
import org.apache.commons.io.FileUtils
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Suite}

/**
  * Created by sathiya on 15/02/17.
  */
abstract class SharedContext extends FlatSpecLike with BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite =>

  Logger.getRootLogger.setLevel(Level.WARN)
  Logger.getLogger("org").setLevel(Level.WARN)
  Logger.getLogger("akka").setLevel(Level.WARN)
  Logger.getLogger("/executors").setLevel(Level.FATAL)

  Locale.setDefault(Locale.US)
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  private var _spark: SparkSession = _

  protected def spark: SparkSession = _spark
  protected def sqlContext: SQLContext = _spark.sqlContext
  protected def sc: SparkContext = _spark.sparkContext

  protected override def beforeAll(): Unit = {
    if(_spark == null) {
      _spark = SparkSession
        .builder()
        .appName("Tests")
        .master("local[4]")
        .config("spark.sql.testkey", "true")
        .getOrCreate()
    }

    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    FileUtils.deleteDirectory(new File("target/test/output"))
    super.beforeEach()
  }

  override def afterAll() {
    try {
      if(_spark != null) {
        _spark.stop()
        _spark = null
      }
    } finally {
      FileUtils.deleteDirectory(new File("target/test/output"))
      super.afterAll()
    }
  }
}
