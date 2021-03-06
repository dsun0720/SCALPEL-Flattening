// License: BSD 3 clause

package fr.polytechnique.cmap.cnam

import java.io.File
import java.util.{Locale, TimeZone}
import org.apache.commons.io.FileUtils
import org.apache.log4j.{Level, Logger}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Suite}
import org.apache.spark.SparkContext
import org.apache.spark.sql._
import fr.polytechnique.cmap.cnam.utilities.DFUtils

abstract class SharedContext extends FlatSpecLike with BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite =>

  Logger.getRootLogger.setLevel(Level.WARN)
  Logger.getLogger("org").setLevel(Level.WARN)
  Logger.getLogger("akka").setLevel(Level.WARN)
  Logger.getLogger("/executors").setLevel(Level.FATAL)

  Locale.setDefault(Locale.US)
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  private var _spark: SparkSession = _
  protected val debug: Boolean = false

  protected def spark: SparkSession = _spark

  protected def sqlContext: SQLContext = _spark.sqlContext

  protected def sc: SparkContext = _spark.sparkContext

  def assertDFs(ds1: DataFrame, ds2: DataFrame, debug: Boolean = this.debug): Unit =
    assertDSs[Row](ds1, ds2, debug)

  def assertDSs[A](ds1: Dataset[A], ds2: Dataset[A], debug: Boolean = this.debug): Unit = {
    val df1 = ds1.toDF
    val df2 = ds2.toDF
    try {

      df1.persist()
      df2.persist()

      if (debug) {
        df1.printSchema()
        df2.printSchema()
        df1.show(100, false)
        df2.show(100, false)
      }

      import DFUtils._
      assert(df1 sameAs df2)

    } finally {
      df1.unpersist()
      df2.unpersist()
    }
  }

  protected override def beforeAll(): Unit = {
    if (_spark == null) {
      _spark = SparkSession
        .builder()
        .appName("Tests")
        .master("local[4]")
        .config("spark.sql.testkey", "true")
        .config("spark.default.parallelism", 2)
        .config("spark.sql.shuffle.partitions", 2)
        .config("spark.sql.orc.impl", "native")
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
      if (_spark != null) {
        _spark.stop()
        _spark = null
      }
    } finally {
      FileUtils.deleteDirectory(new File("target/test/output"))
      super.afterAll()
    }
  }
}
