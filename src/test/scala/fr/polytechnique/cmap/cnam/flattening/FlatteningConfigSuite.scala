package fr.polytechnique.cmap.cnam.flattening

import java.nio.file.Paths
import com.typesafe.config.ConfigFactory
import pureconfig._
import fr.polytechnique.cmap.cnam.SharedContext
import fr.polytechnique.cmap.cnam.config.ConfigLoader
import fr.polytechnique.cmap.cnam.flattening.FlatteningConfig.{JoinTableConfig, PartitionConfig, TableConfig}

/**
  * Created by sathiya on 21/02/17.
  */
class FlatteningConfigSuite extends SharedContext with ConfigLoader {

  "load" should "loads the correct config file" in {
    val defaultConf = FlatteningConfig.load("", "test")
    val expected = defaultConf.copy(singleTablePath = "/path/base/output", schemaFilePath = List("/path/schema"),
      join = List(JoinTableConfig(name = "MCO", inputPath = "/path/to/input", joinKeys = List("ETA_NUM", "RSA_NUM"), mainTableName = "MCO_C",
        tablesToJoin = List("MCO_B", "MCO_A", "MCO_D", "MCO_UM"), flatOutputPath = "/path/to/flat_table")))
    val stringConfig =
      """
        |single_table_path = "/path/base/output"
        |
        |schema_file_path = [
        |  "/path/schema"
        |]
        |
        |join = [
        |  {
        |    name = "MCO"
        |    input_path = "/path/to/input"
        |    join_keys = ["ETA_NUM", "RSA_NUM"]
        |    main_table_name = "MCO_C"
        |    tables_to_join = ["MCO_B", "MCO_A", "MCO_D", "MCO_UM"]
        |    flat_output_path = "/path/to/flat_table"
        |  }
        |]
      """.trim.stripMargin

    val tempPath = "target/flattening.conf"
    pureconfig.saveConfigAsPropertyFile(ConfigFactory.parseString(stringConfig), Paths.get(tempPath), true)
    //when
    val result = FlatteningConfig.load(tempPath, "test")
    //then
    assert(result == expected)
  }


  "partitions" should "correctly retrieves the corresponding table configuration" in {
    // Given
    val conf = FlatteningConfig.load("", "test")

    val expected = List(
      ConfigPartition("IR_BEN_R", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/DCIR/IR_BEN_R.csv"),
        "target/test/output/IR_BEN_R", None),
      ConfigPartition("IR_IMB_R", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/DCIR/IR_IMB_R.csv"),
        "target/test/output/IR_IMB_R", None),
      ConfigPartition("ER_PRS_F", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/DCIR/ER_PRS_F.csv"),
        "target/test/output/ER_PRS_F/year=2006", Some("FLX_DIS_DTD")),
      ConfigPartition("ER_PHA_F", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/DCIR/ER_PHA_F.csv"),
        "target/test/output/ER_PHA_F/year=2006", None),
      ConfigPartition("ER_CAM_F", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/DCIR/ER_CAM_F.csv"),
        "target/test/output/ER_CAM_F/year=2006", None),
      ConfigPartition("MCO_UM", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO08UM.csv"),
        "target/test/output/MCO_UM/year=2008", None),
      ConfigPartition("MCO_A", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO06A.csv"),
        "target/test/output/MCO_A/year=2006", None),
      ConfigPartition("MCO_A", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO07A.csv"),
        "target/test/output/MCO_A/year=2007", None),
      ConfigPartition("MCO_A", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO08A.csv"),
        "target/test/output/MCO_A/year=2008", None),
      ConfigPartition("MCO_B", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO06B.csv"),
        "target/test/output/MCO_B/year=2006", None),
      ConfigPartition("MCO_B", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO07B.csv"),
        "target/test/output/MCO_B/year=2007", None),
      ConfigPartition("MCO_B", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO08B.csv"),
        "target/test/output/MCO_B/year=2008", None),
      ConfigPartition("MCO_C", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO06C.csv"),
        "target/test/output/MCO_C/year=2006", Some("NUM_ENQ")),
      ConfigPartition("MCO_C", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO07C.csv"),
        "target/test/output/MCO_C/year=2007", Some("NUM_ENQ")),
      ConfigPartition("MCO_C", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO08C.csv"),
        "target/test/output/MCO_C/year=2008", Some("NUM_ENQ")),
      ConfigPartition("MCO_D", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO06D.csv"),
        "target/test/output/MCO_D/year=2006", None),
      ConfigPartition("MCO_D", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO07D.csv"),
        "target/test/output/MCO_D/year=2007", None),
      ConfigPartition("MCO_D", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/PMSI/T_MCO08D.csv"),
        "target/test/output/MCO_D/year=2008", None)

    )
    // When
    val result = conf.partitions

    // Then
    assert(result.toSet == expected.toSet)

  }

  "partitions" should "return the correct Partition config given the two config" in {
    // Given
    val tableConfig = loadConfig[TableConfig](ConfigFactory.parseString(
      """
      {
        name = IR_BEN_R
        partition_strategy = "none"
        partitions = [{path = [/shared/Observapur/raw_data/IR_BEN_R.CSV]}]
      }
      """.stripMargin)
    ).right.get
    val partitionConfig =
      loadConfig[PartitionConfig](ConfigFactory.parseString("{path = [/shared/Observapur/raw_data/IR_BEN_R.CSV]}"))
        .right.get

    val expected = ConfigPartition("IR_BEN_R", "dd/MM/yyyy", List("/shared/Observapur/raw_data/IR_BEN_R.CSV"),
      "target/test/output/IR_BEN_R", None)

    // When
    val result = FlatteningConfig.toConfigPartition("target/test/output", tableConfig, partitionConfig)

    // Then
    assert(result == expected)
  }

  "SinglePartitionConfig.outputPath" should "return correct path given year strategy" in {
    // Given
    val partitionConfig = loadConfig[PartitionConfig](ConfigFactory.parseString(
      """
        {path = [/shared/Observapur/raw_data/IR_BEN_R.CSV]
        year=2010}
      """.stripMargin)
    ).right.get

    val strategyName = "year"
    val tableName = "IR_BEN_R"

    val expected = "target/test/output/IR_BEN_R/year=2010"

    // When
    val result = partitionConfig.outputPath("target/test/output", strategyName, tableName)

    // Then
    assert(result == expected)
  }

  it should "return correct path given no strategy" in {
    // Given
    val partitionConfig = loadConfig[PartitionConfig](ConfigFactory.parseString(
      """
        {path = [/shared/Observapur/raw_data/IR_BEN_R.CSV]}
      """.stripMargin)
    ).right.get

    val strategyName = "whatever"
    val tableName = "IR_BEN_R"

    val expected = "target/test/output/IR_BEN_R"

    // When
    val result = partitionConfig.outputPath("target/test/output", strategyName, tableName)

    // Then
    assert(result == expected)
  }
}
