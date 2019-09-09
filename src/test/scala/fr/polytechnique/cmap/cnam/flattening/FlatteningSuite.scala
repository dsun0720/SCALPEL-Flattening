package fr.polytechnique.cmap.cnam.flattening

import org.apache.spark.sql._
import fr.polytechnique.cmap.cnam.SharedContext
import fr.polytechnique.cmap.cnam.utilities.DFUtils._

class FlatteningSuite extends SharedContext {

  "saveCSVTablesAsParquet" should "read all dummy csv tables and save as parquet files" in {
    //Given
    val conf = FlatteningConfig.load("", "test")

    val expectedTables: List[String] = conf.tables.map(_.name)
    val expectedDfs: Map[String, DataFrame] = expectedTables.map {
      name =>
        name -> sqlContext.read
          .option("mergeSchema", "true")
          .load("src/test/resources/flattening/parquet-table/single_table/" + name)
          .toDF
    }.toMap

    //When
    val meta = Flattening.saveCSVTablesAsParquet(sqlContext, conf)
    val result = meta.map {
      op =>
        op.outputTable-> sqlContext.read
          .option("mergeSchema", "true")
          .load(op.outputPath + "/" + op.outputTable)
          .toDF
    }.toMap


    //Then
    import fr.polytechnique.cmap.cnam.utilities.DFUtils._
    expectedDfs.foreach {
      case (name, df) =>
        assert(df.sameAs(result(name), true))
    }
  }

  "joinSingleTablesToFlatTable" should "join the single tables correctly" in {

    //Given
    val conf = FlatteningConfig.load("", "test")
    val expectedMCO: DataFrame = sqlContext.read.option("mergeSchema", "true").parquet("src/test/resources/flattening/parquet-table/flat_table/MCO/")
    val expectedDCIR: DataFrame = sqlContext.read.option("mergeSchema", "true").parquet("src/test/resources/flattening/parquet-table/flat_table/by_month/DCIR/")
    val configTest = conf.copy(join = conf.join.map(_.copy(inputPath = Some("src/test/resources/flattening/parquet-table/single_table"))))

    //When
    val meta = Flattening.joinSingleTablesToFlatTable(sqlContext, configTest)
    val result = meta.map {
      op =>
        op.outputTable-> sqlContext.read
          .option("mergeSchema", "true")
          .load(op.outputPath + "/" + op.outputTable)
          .toDF
    }.toMap

    //Then
    assert(expectedMCO sameAs result("MCO"))
    assert(expectedDCIR sameAs result("DCIR"))

  }

}
