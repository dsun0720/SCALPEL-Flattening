package fr.polytechnique.cmap.cnam.flattening

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, SQLContext}
import fr.polytechnique.cmap.cnam.flattening.FlatteningConfig.JoinTableConfig
import fr.polytechnique.cmap.cnam.utilities.DFUtils._

class FlatTable(sqlContext: SQLContext, config: JoinTableConfig) {

  val inputBasePath: String = config.inputPath.get
  val mainTable: Table = Table.build(sqlContext, inputBasePath, config.mainTableName)
  val tablesToJoin: List[Table] = config.tablesToJoin.map(
    tableName =>
      Table.build(sqlContext, inputBasePath, tableName)
  )
  val outputBasePath: String = config.flatOutputPath.get
  val foreignKeys: List[String] = config.joinKeys
  val tableName: String = config.name
  val monthlyPartitionColumn: Option[String] = config.monthlyPartitionColumn
  val saveMode: String = config.flatTableSaveMode
  val refs: List[(Table, FlatteningConfig.Reference)] = config.refsToJoin.map {
    refConfig => (Table.build(sqlContext, refConfig.inputPath.get, refConfig.name), refConfig)
  }

  def flatTablePerYear: Array[Int] = mainTable.getYears.filter {
    year => config.onlyOutput.isEmpty || config.onlyOutput.exists(_.year == year)
  }

  def joinRefs(table: Table): Table = {
    refs.foldLeft(table) {
      case (flatTable, (refTable, refConfig)) =>
        val refDF = refTable.annotate(List.empty[String])
        val cols = refConfig.joinKeysTuples.map {
          case (ftKey, refKey) => flatTable.df.col(ftKey) === refDF.col(refKey)
        }.reduce(_ && _)
        new Table(flatTable.name, flatTable.df.join(refDF, cols, "left"))
    }
  }

  def joinByYear(year: Int): Table = {
    val name = s"$tableName/year=$year"
    val joinedDF = tablesToJoin
      .map(table => table.filterByYearAndAnnotate(year, foreignKeys))
      .foldLeft(mainTable.filterByYear(year))(joinFunction)

    new Table(name, joinedDF)
  }

  def joinByYearAndDate(year: Int, month: Int, monthCol: String): Table = {
    val name = s"$tableName/year=$year/month=$month"
    val joinedDF = tablesToJoin
      .map(table => table.filterByYearMonthAndAnnotate(year, month, foreignKeys, monthCol))
      .foldLeft(mainTable.filterByYearAndMonth(year, month, monthCol))(joinFunction)

    new Table(name, joinedDF)
  }

  val joinFunction: (DataFrame, DataFrame) => DataFrame =
    (accumulator, tableToJoin) => accumulator.join(tableToJoin, foreignKeys, "left_outer")

  def logger: Logger = Logger.getLogger(getClass)

  def writeTable(table: Table): Unit = {

    Logger.getRootLogger.setLevel(Level.ERROR)
    val t0 = System.nanoTime()
    table.df.writeParquet(outputBasePath + "/" + table.name)(saveMode)
    val t1 = System.nanoTime()
    logger.info(s"   writing duration ${table.name} ${(t1 - t0) / Math.pow(10, 9)} sec")
  }

  /*
   * if monthlyPartitionColumn is set, the data will be partitioned in year and month
   * if not, the data will be partitioned in year
   * We can output the data in specifying its months and years
   */
  def writeAsParquet(): Unit = {
    flatTablePerYear
      .foreach {
        year =>
          if (monthlyPartitionColumn.isDefined) {
            logger.info("Join by year and month: " + year)
            val filteredMonths = config.onlyOutput.find(_.year == year) match {
              case None => List.empty[Int]
              case Some(thisYear) => thisYear.months
            }
            Range(1, 13).filter(filteredMonths.isEmpty || filteredMonths.contains(_)).foreach {
              month =>
                logger.info("Month: " + month)
                val joinedTable = joinRefs(joinByYearAndDate(year, month, monthlyPartitionColumn.get))
                writeTable(joinedTable)
            }
          }
          else {
            logger.info("Join by year : " + year)
            val joinedTable = joinRefs(joinByYear(year))
            writeTable(joinedTable)
          }
      }
  }

}
