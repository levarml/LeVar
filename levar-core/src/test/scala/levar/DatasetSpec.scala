package levar

import org.scalatest._
import play.api.libs.json.Json
import play.api.libs.json.Json.{ obj => j }

class DatasetSpec extends FlatSpec {

  "Dataset.validSchema" should "pass a valid schema" in {
    assert {
      Dataset.validSchema(
        j("properties" -> j("text" -> j("type" -> "string"), "count" -> j("type" -> "number"))))
    }
  }

  it should "not pass a schema with missing 'properties'" in {
    assert { !Dataset.validSchema(j("foo" -> "bar")) }
  }

  it should "not pass a non-object" in {
    assert { !Dataset.validSchema(Json.toJson(2)) }
    assert { !Dataset.validSchema(Json.toJson("hello")) }
  }

  it should "not pass an object with empty properties" in {
    assert { !Dataset.validSchema(j("properties" -> j())) }
  }

  it should "not pass an object with properties missing a 'type'" in {
    assert { !Dataset.validSchema(j("properties" -> j("foo" -> j()))) }
    assert { !Dataset.validSchema(j("properties" -> j("foo" -> j("bar" -> "baz")))) }
  }

  it should "not pass an object whose property types aren't 'string' or 'number'" in {
    assert {
      !Dataset.validSchema(j("properties" -> j("text" -> j("type" -> "list"))))
    }
  }
}