package levar.jsn

import org.scalatest._
import play.api.libs.json._
import levar._
import org.joda.time._
import org.joda.time.DateTimeZone._

class JsnSpec extends FlatSpec {

  def assertCreate[A](expected: String, input: A)(implicit wrtr: Writes[A]) {
    assert(Json.parse(expected) == Json.toJson(input))
  }

  "Comment JSON serialization" should "create basic JSON" in {
    assertCreate(
      """{"username":"john", "comment": "Hello world"}""",
      Comment[String]("john", "Hello world"))
  }

  it should "handle IDs" in {
    assertCreate(
      """{"id": "2938abb3", "username": "john", "comment": "Hello world"}""",
      Comment[String]("john", "Hello world", id = Some("2938abb3")))
  }

  it should "handle paths" in {
    assertCreate(
      """|{
         |  "username": "john",
         |  "comment": "Hello world!",
         |  "path":"/api/foo/experiment/1234/comments/abcdef"
         |}
         |""".stripMargin,
      Comment[String](
        "john",
        "Hello world!",
        path = Some("/api/foo/experiment/1234/comments/abcdef")))
  }

  it should "handle subjects" in {
    assertCreate(
      """|{
         |  "username": "john",
         |  "comment": "Hello world!",
         |  "subject": {"type": "experiment", "value": {"id": "silly-experiment"}}
         |}
         |""".stripMargin,
      Comment("john", "Hello world!", subject = Some(Experiment("silly-experiment"))))
  }

  it should "handle created dates" in {
    val timestamp = DateTime.now
    val timestampIso = timestamp.withZone(UTC).toString()
    assertCreate(
      s"""{"username": "john", "comment": "Hello world!", "created_at": "${timestampIso}"}""",
      Comment[String]("john", "Hello world!", createdAt = Some(timestamp)))
  }

  "Datum JSON serialization" should "create basic JSON" in {
    assertCreate(
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}}""",
      Datum(Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987))
    )
  }

  it should "handle real values" in {
    assertCreate(
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}, "value": 0.56}""",
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        value = Some(Left(0.56))))
  }

  it should "handle categorical values" in {
    assertCreate(
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}, "value": "win"}""",
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        value = Some(Right("win"))))
  }

  it should "handle IDs" in {
    assertCreate(
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}, "id": "datum-1"}""",
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        id = Some("datum-1")))
  }

  it should "handle labels" in {
    assertCreate(
      """|{
         |  "id": "hello",
         |  "data": {"text": "Hello world!", "score": 123.4, "count": 987},
         |  "labels": {
         |    "items": ["big", "data"],
         |    "size": 2,
         |    "path": "/api/foo/dataset/dataset-1/hello/labels"
         |  }
         |}
         |""".stripMargin,
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        id = Some("hello"),
        labels = Some(ResultSet(Seq("big", "data"), "/api/foo/dataset/dataset-1/hello/labels"))))
  }

  it should "handle comments" in {
    assertCreate(
      """|{
         |  "id": "hello",
         |  "data": {"text": "Hello world!", "score": 123.4, "count": 987},
         |  "comments": {
         |    "items": [{"username": "john", "comment": "this is cool!"}],
         |    "size": 1,
         |    "path": "/api/foo/dataset/dataset-1/hello/comments"
         |  }
         |}
         |""".stripMargin,
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        id = Some("hello"),
        comments = Some(ResultSet(Seq(
          Comment("john", "this is cool!")), "/api/foo/dataset/dataset-1/hello/comments"))))
  }

  "Result set JSON serialization" should "create basic JSON" in {
    assertCreate(
      """{"items": ["big", "data"], "size": 2, "path": "/api/foo/dataset/dataset-1/labels"}""",
      ResultSet(Seq("big", "data"), "/api/foo/dataset/dataset-1/labels"))
  }

  it should "handle total" in {
    assertCreate(
      """|{
         |  "items": ["big", "data"],
         |  "size": 2,
         |  "total": 1001,
         |  "path": "/api/foo/dataset/dataset-1/labels"
         |}
         |""".stripMargin,
      ResultSet(Seq("big", "data"), "/api/foo/dataset/dataset-1/labels", total = Some(1001)))
  }

  it should "handle next_path" in {
    assertCreate(
      """|{
         |  "items": ["big", "data"],
         |  "size": 2,
         |  "total": 1001,
         |  "path": "/api/foo/dataset/dataset-1/labels",
         |  "next_path": "/api/foo/dataset/dataset-1/labels?after=123412345"
         |}
         |""".stripMargin,
      ResultSet(
        Seq("big", "data"),
        "/api/foo/dataset/dataset-1/labels",
        total = Some(1001),
        nextPath = Some("/api/foo/dataset/dataset-1/labels?after=123412345")))
  }

  "Dataset JSON serialization" should "create basic JSON" in {
    assertCreate(
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}}
         |}
         |""".stripMargin,
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number")))))
  }

  it should "handle name" in {}

  it should "handle created date" in {}

  it should "hanlde updated date" in {}

  it should "handle size" in {}

  it should "handle items" in {}

  it should "handle experiments" in {}

  it should "handle labels" in {}

  it should "handle comments" in {}

  "Experiment JSON serialization" should "create basic JSON" in {
    assertCreate(
      """{"id": "hello-world"}""",
      Experiment("hello-world"))
  }
}
