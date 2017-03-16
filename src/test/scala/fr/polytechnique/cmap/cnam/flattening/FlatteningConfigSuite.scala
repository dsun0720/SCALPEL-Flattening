package fr.polytechnique.cmap.cnam.flattening

import fr.polytechnique.cmap.cnam.SharedContext

/**
  * Created by sathiya on 21/02/17.
  */
class FlatteningConfigSuite extends SharedContext {


  "getPartitionList" should "correctly retrieves the corresponding table configuration" in {
    // Given
    val inputConfs = FlatteningConfig.tablesConfigList

    val expected = List(
      ConfigPartition("IR_BEN_R", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/DCIR/IR_BEN_R.csv"),
        "target/test/output/IR_BEN_R"),
      ConfigPartition("IR_IMB_R", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/DCIR/IR_IMB_R.csv"),
        "target/test/output/IR_IMB_R"),
      ConfigPartition("ER_PRS_F", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/DCIR/ER_PRS_F.csv"),
        "target/test/output/ER_PRS_F"),
      ConfigPartition("ER_PHA_F", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/DCIR/ER_PHA_F.csv"),
        "target/test/output/ER_PHA_F"),
      ConfigPartition("ER_CAM_F", "dd/MM/yyyy", List("src/test/resources/flattening/csv-table/DCIR/ER_CAM_F.csv"),
        "target/test/output/ER_CAM_F")
    )
    // When
    val result = FlatteningConfig.getPartitionList(inputConfs)

    // Then
    assert(result == expected)

  }
}
