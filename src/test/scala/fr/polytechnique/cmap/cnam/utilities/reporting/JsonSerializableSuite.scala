// License: BSD 3 clause

package fr.polytechnique.cmap.cnam.utilities.reporting

import java.sql.Timestamp
import java.util.{Locale, TimeZone}
import org.scalatest.FlatSpec

class JsonSerializableSuite extends FlatSpec {

  Locale.setDefault(Locale.US)
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  def makeTS(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): Timestamp = {
    Timestamp.valueOf(f"$year%04d-$month%02d-$day%02d $hour%02d:$minute%02d:$second%02d")
  }

  val input: TestItemList = {
    val item1 = TestItem(1, 1.0, "one", Some("one"), makeTS(2011, 1, 1))
    val item2 = TestItem(2, 2.0, "two", None, makeTS(2022, 2, 2))
    TestItemList(List(item1, item2))
  }

  case class TestItem(
    anInteger: Int,
    aDouble: Double,
    aString: String,
    anOption: Option[String],
    aDate: java.util.Date) extends JsonSerializable

  case class TestItemList(aList: List[TestItem]) extends JsonSerializable

  "toJsonString" should "produce a nice pretty string" in {

    // Given
    val expected =
      """|{
         |  "a_list" : [ {
         |    "an_integer" : 1,
         |    "a_double" : 1.0,
         |    "a_string" : "one",
         |    "an_option" : "one",
         |    "a_date" : "2011-01-01T00:00:00Z"
         |  }, {
         |    "an_integer" : 2,
         |    "a_double" : 2.0,
         |    "a_string" : "two",
         |    "a_date" : "2022-02-02T00:00:00Z"
         |  } ]
         |}""".stripMargin

    // When
    val result = input.toJsonString(pretify = true)

    // Then
    assert(result == expected)
  }

  it should "produce a compact string if prettify=false" in {

    // Given
    val expected =
      """{""" +
        """"a_list":[""" +
        """{""" +
        """"an_integer":1,""" +
        """"a_double":1.0,""" +
        """"a_string":"one",""" +
        """"an_option":"one",""" +
        """"a_date":"2011-01-01T00:00:00Z"""" +
        """},""" +
        """{""" +
        """"an_integer":2,""" +
        """"a_double":2.0,""" +
        """"a_string":"two",""" +
        """"a_date":"2022-02-02T00:00:00Z"""" +
        """}""" +
        """]""" +
        """}"""

    // When
    val result = input.toJsonString(pretify = false)

    // Then
    assert(result == expected)
  }
}
