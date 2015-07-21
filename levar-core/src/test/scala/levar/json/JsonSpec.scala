package levar.json

import org.scalatest._
import play.api.libs.json._
import levar._
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC

class JsonSpec extends FlatSpec {

  def assertWrites[A](expected: String, input: A)(implicit writer: Writes[A]) {
    assert(Json.parse(expected) == Json.toJson(input))
  }

  def assertReads[A](expected: A, input: String)(implicit reader: Reads[A]) {
    assert(JsSuccess(expected) == Json.parse(input).validate[A])
  }

  def assertReadsFail[A](input: String)(implicit reader: Reads[A]) {
    val s = if (input.size < 10) input else (input.take(7) + "...")
    Json.parse(input).validate[A] match {
      case _: JsError => info(s"correctly failed $s")
      case _: JsSuccess[A] => info(s"correctly parsed $s")
    }
  }

  "Comment JSON serialization" should "create basic JSON" in {
    assertWrites(
      """{"username":"john", "comment": "Hello world"}""",
      Comment("john", "Hello world"))
  }

  it should "handle IDs" in {
    assertWrites(
      """{"id": "2938abb3", "username": "john", "comment": "Hello world"}""",
      Comment("john", "Hello world", id = Some("2938abb3")))
  }

  it should "handle paths" in {
    assertWrites(
      """|{
         |  "username": "john",
         |  "comment": "Hello world!",
         |  "path":"/api/foo/experiment/1234/comments/abcdef"
         |}
         |""".stripMargin,
      Comment(
        "john",
        "Hello world!",
        path = Some("/api/foo/experiment/1234/comments/abcdef")))
  }

  it should "handle subjects" in {
    assertWrites(
      """|{
         |  "username": "john",
         |  "comment": "Hello world!",
         |  "subject": {"type": "experiment", "value": {"id": "silly-experiment"}}
         |}
         |""".stripMargin,
      Comment(
        "john",
        "Hello world!",
        subject = Some(("experiment", Json.parse("""{"id": "silly-experiment"}""")))))
  }

  it should "handle created dates" in {
    val timestamp = DateTime.now.withZone(UTC)
    val timestampIso = timestamp.toString()
    assertWrites(
      s"""{"username": "john", "comment": "Hello world!", "created_at": "${timestampIso}"}""",
      Comment("john", "Hello world!", createdAt = Some(timestamp)))
  }

  "Comment JSON deserialization" should "parse basic JSON" in {
    assertReads[Comment](
      Comment("john", "Yo"),
      """{"username": "john", "comment": "Yo"}""")
  }

  it should "fail on missing fields" in {
    assertReadsFail[Comment]("{}")
    assertReadsFail[Comment]("""{"username": "john"}""")
  }

  it should "handle IDs" in {
    assertReads(
      Comment("john", "Hello world", id = Some("2938abb3")),
      """{"id": "2938abb3", "username": "john", "comment": "Hello world"}""")
  }

  it should "handle paths" in {
    assertReads(
      Comment(
        "john",
        "Hello world!",
        path = Some("/api/foo/experiment/1234/comments/abcdef")),
      """|{
         |  "username": "john",
         |  "comment": "Hello world!",
         |  "path":"/api/foo/experiment/1234/comments/abcdef"
         |}
         |""".stripMargin)
  }

  it should "handle subjects" in {
    assertReads(
      Comment(
        "john",
        "Hello world!",
        subject = Some(("experiment", Json.parse("""{"id": "silly-experiment"}""")))),
      """|{
         |  "username": "john",
         |  "comment": "Hello world!",
         |  "subject": {"type": "experiment", "value": {"id": "silly-experiment"}}
         |}
         |""".stripMargin)
  }

  it should "handle created dates" in {
    val timestamp = DateTime.now.withZone(UTC)
    val timestampIso = timestamp.toString()
    assertReads(
      Comment("john", "Hello world!", createdAt = Some(timestamp)),
      s"""{"username": "john", "comment": "Hello world!", "created_at": "${timestampIso}"}""")
  }

  "Datum JSON serialization" should "create basic JSON" in {
    assertWrites(
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}}""",
      Datum(Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987))
    )
  }

  it should "handle real values" in {
    assertWrites(
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}, "value": 0.56}""",
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        value = Some(Left(0.56))))
  }

  it should "handle categorical values" in {
    assertWrites(
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}, "value": "win"}""",
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        value = Some(Right("win"))))
  }

  it should "handle IDs" in {
    assertWrites(
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}, "id": "datum-1"}""",
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        id = Some("datum-1")))
  }

  it should "handle created at" in {
    assertWrites(
      """|{
         |  "data": {
         |    "text": "Hello world!",
         |    "score": 123.4,
         |    "count": 987
         |  },
         |  "created_at": "2005-03-26T12:00:00.000Z"
         |}
         |""".stripMargin,
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        createdAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))))

  }

  it should "handle labels" in {
    assertWrites(
      """|{
         |  "id": "hello",
         |  "data": {"text": "Hello world!", "score": 123.4, "count": 987},
         |  "labels": ["big", "data"]
         |}
         |""".stripMargin,
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        id = Some("hello"),
        labels = Some(Seq("big", "data"))))
  }

  it should "handle comments" in {
    assertWrites(
      """|{
         |  "id": "hello",
         |  "data": {"text": "Hello world!", "score": 123.4, "count": 987},
         |  "comments": {
         |    "items": [{"username": "john", "comment": "this is cool!"}]
         |  }
         |}
         |""".stripMargin,
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        id = Some("hello"),
        comments = Some(ResultSet(Seq(Comment("john", "this is cool!"))))))
  }

  "Datum JSON deserialization" should "create basic JSON" in {
    assertReads(
      Datum(Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987)),
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}}""")
  }

  it should "handle real values" in {
    assertReads(
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        value = Some(Left(0.56))),
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}, "value": 0.56}""")
  }

  it should "handle categorical values" in {
    assertReads(
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        value = Some(Right("win"))),
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}, "value": "win"}""")
  }

  it should "handle IDs" in {
    assertReads(
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        id = Some("datum-1")),
      """{"data": {"text": "Hello world!", "score": 123.4, "count": 987}, "id": "datum-1"}""")
  }

  it should "handle created at" in {
    assertReads(
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        createdAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))),
      """|{
         |  "data": {
         |    "text": "Hello world!",
         |    "score": 123.4,
         |    "count": 987
         |  },
         |  "created_at": "2005-03-26T12:00:00.000Z"
         |}
         |""".stripMargin)
  }

  it should "handle labels" in {
    assertReads(
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        id = Some("hello"),
        labels = Some(Seq("big", "data"))),
      """|{
         |  "id": "hello",
         |  "data": {"text": "Hello world!", "score": 123.4, "count": 987},
         |  "labels": ["big", "data"]
         |}
         |""".stripMargin)
  }

  it should "handle comments" in {
    assertReads(
      Datum(
        Json.obj("text" -> "Hello world!", "score" -> 123.4, "count" -> 987),
        id = Some("hello"),
        comments = Some(ResultSet(Seq(Comment("john", "this is cool!"))))),
      """|{
         |  "id": "hello",
         |  "data": {"text": "Hello world!", "score": 123.4, "count": 987},
         |  "comments": {
         |    "items": [{"username": "john", "comment": "this is cool!"}]
         |  }
         |}
         |""".stripMargin)
  }

  "Result set JSON serialization" should "create basic JSON" in {
    assertWrites("""{"items": ["big", "data"]}""", ResultSet(Seq("big", "data")))
  }

  it should "handle paths" in {
    assertWrites(
      """{"items": ["big", "data"], "path": "/api/foo/dataset/dataset-1/labels"}""",
      ResultSet(Seq("big", "data"), Some("/api/foo/dataset/dataset-1/labels")))
  }

  it should "handle total" in {
    assertWrites(
      """|{
         |  "items": ["big", "data"],
         |  "total": 1001
         |}
         |""".stripMargin,
      ResultSet(Seq("big", "data"), total = Some(1001)))
  }

  it should "handle next_path" in {
    assertWrites(
      """|{
         |  "items": ["big", "data"],
         |  "total": 1001,
         |  "next_path": "/api/foo/dataset/dataset-1/labels?after=123412345"
         |}
         |""".stripMargin,
      ResultSet(
        Seq("big", "data"),
        total = Some(1001),
        nextPath = Some("/api/foo/dataset/dataset-1/labels?after=123412345")))
  }

  "Result set JSON deserialization" should "create basic JSON" in {
    assertReads(ResultSet(Seq("big", "data")), """{"items": ["big", "data"]}""")
  }

  it should "handle paths" in {
    assertReads(
      ResultSet(Seq("big", "data"), Some("/api/foo/dataset/dataset-1/labels")),
      """{"items": ["big", "data"], "path": "/api/foo/dataset/dataset-1/labels"}""")
  }

  it should "handle total" in {
    assertReads(
      ResultSet(Seq("big", "data"), total = Some(1001)),
      """|{
         |  "items": ["big", "data"],
         |  "total": 1001
         |}
         |""".stripMargin)
  }

  it should "handle next_path" in {
    assertReads(
      ResultSet(
        Seq("big", "data"),
        total = Some(1001),
        nextPath = Some("/api/foo/dataset/dataset-1/labels?after=123412345")),
      """|{
         |  "items": ["big", "data"],
         |  "total": 1001,
         |  "next_path": "/api/foo/dataset/dataset-1/labels?after=123412345"
         |}
         |""".stripMargin)
  }

  "Dataset JSON serialization" should "create basic JSON" in {
    assertWrites(
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

  it should "handle name" in {
    assertWrites(
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "name": "Hello dataset"
         |}
         |""".stripMargin,
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        name = Some("Hello dataset")))
  }

  it should "handle created date" in {
    assertWrites(
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "created_at": "2005-03-26T12:00:00.000Z"
         |}
         |""".stripMargin,
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        createdAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))))
  }

  it should "hanlde updated date" in {
    assertWrites(
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "updated_at": "2005-03-26T12:00:00.000Z"
         |}
         |""".stripMargin,
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        updatedAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))))
  }

  it should "handle size" in {
    assertWrites(
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "size": 100
         |}
         |""".stripMargin,
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        size = Some(100)))
  }

  it should "handle labels" in {
    assertWrites(
      """|{
       |  "id": "hello-dataset",
       |  "type": "classification",
       |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
       |  "labels": ["big", "data"]
       |}
       |""".stripMargin,
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        labels = Some(Seq("big", "data"))))
  }

  it should "handle comments" in {
    assertWrites(
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "comments": {
         |    "items": [
         |      {"username": "john", "comment": "This is cool"},
         |      {"username": "mary", "comment": "Hello world"}
         |    ]
         |  }
         |}
         |""".stripMargin,
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        comments = Some(ResultSet(
          Seq(Comment("john", "This is cool"), Comment("mary", "Hello world"))))))

  }

  "Dataset JSON deserialization" should "create basic JSON" in {
    assertReads(
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number")))),
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}}
         |}
         |""".stripMargin)
  }

  it should "handle name" in {
    assertReads(
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        name = Some("Hello dataset")),
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "name": "Hello dataset"
         |}
         |""".stripMargin)
  }

  it should "handle created date" in {
    assertReads(
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        createdAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))),
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "created_at": "2005-03-26T12:00:00.000Z"
         |}
         |""".stripMargin)
  }

  it should "hanlde updated date" in {
    assertReads(
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        updatedAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))),
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "updated_at": "2005-03-26T12:00:00.000Z"
         |}
         |""".stripMargin)
  }

  it should "handle size" in {
    assertReads(
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        size = Some(100)),
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "size": 100
         |}
         |""".stripMargin)
  }

  it should "handle labels" in {
    assertReads(
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        labels = Some(Seq("big", "data"))),
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "labels": ["big", "data"]
         |}
         |""".stripMargin)
  }

  it should "handle comments" in {
    assertReads(
      Dataset(
        "hello-dataset",
        'c',
        Json.obj(
          "properties" -> Json.obj("text" -> Json.obj("type" -> "string"),
            "score" -> Json.obj("type" -> "number"))),
        comments = Some(ResultSet(
          Seq(Comment("john", "This is cool"), Comment("mary", "Hello world"))))),
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "comments": {
         |    "items": [
         |      {"username": "john", "comment": "This is cool"},
         |      {"username": "mary", "comment": "Hello world"}
         |    ]
         |  }
         |}
         |""".stripMargin)
  }

  "Experiment JSON serialization" should "create basic JSON" in {
    assertWrites(
      """{"id": "hello-world"}""",
      Experiment("hello-world"))
  }

  it should "handle datasetIds" in {
    assertWrites(
      """{"id": "hello-world", "dataset_ids": ["foo", "bar", "baz"]}""",
      Experiment("hello-world", datasetIds = Some(Seq("foo", "bar", "baz"))))
  }

  it should "handle name" in {
    assertWrites(
      """{"id": "hello-world", "name": "Hello world!"}""",
      Experiment("hello-world", name = Some("Hello world!")))
  }

  it should "handle createdAt" in {
    assertWrites(
      """{"id": "hello-world", "created_at": "2005-03-26T12:00:00.000Z"}""",
      Experiment("hello-world", createdAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))))
  }

  it should "handle updatedAt" in {
    assertWrites(
      """{"id": "hello-world", "updated_at": "2005-03-26T12:00:00.000Z"}""",
      Experiment("hello-world", updatedAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))))
  }

  it should "handle size" in {
    assertWrites(
      """{"id": "hello-world", "size": 1000}""",
      Experiment("hello-world", size = Some(1000)))
  }

  it should "handle labels" in {
    assertWrites(
      """{"id": "hello-world", "labels": ["big", "data"]}""",
      Experiment("hello-world", labels = Some(Seq("big", "data"))))
  }

  it should "handle comments" in {
    assertWrites(
      """|{
         |  "id": "hello-world",
         |  "comments": {
         |    "items": [
         |      {"username": "john", "comment": "yo"},
         |      {"username": "mary", "comment": "lo"}
         |    ]
         |  }
         |}
         |""".stripMargin,
      Experiment(
        "hello-world",
        comments = Some(ResultSet(
          Seq(Comment("john", "yo"), Comment("mary", "lo"))))))
  }

  "Experiment JSON deserialization" should "create basic JSON" in {
    assertReads(
      Experiment("hello-world"),
      """{"id": "hello-world"}""")
  }

  it should "handle datasetIds" in {
    assertReads(
      Experiment("hello-world", datasetIds = Some(Seq("foo", "bar", "baz"))),
      """{"id": "hello-world", "dataset_ids": ["foo", "bar", "baz"]}""")
  }

  it should "handle name" in {
    assertReads(
      Experiment("hello-world", name = Some("Hello world!")),
      """{"id": "hello-world", "name": "Hello world!"}""")
  }

  it should "handle createdAt" in {
    assertReads(
      Experiment("hello-world", createdAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))),
      """{"id": "hello-world", "created_at": "2005-03-26T12:00:00.000Z"}""")
  }

  it should "handle updatedAt" in {
    assertReads(
      Experiment("hello-world", updatedAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))),
      """{"id": "hello-world", "updated_at": "2005-03-26T12:00:00.000Z"}""")
  }

  it should "handle size" in {
    assertReads(
      Experiment("hello-world", size = Some(1000)),
      """{"id": "hello-world", "size": 1000}""")
  }

  it should "handle labels" in {
    assertReads(
      Experiment("hello-world", labels = Some(Seq("big", "data"))),
      """{"id": "hello-world", "labels": ["big", "data"]}""")
  }

  it should "handle comments" in {
    assertReads(
      Experiment(
        "hello-world",
        comments = Some(ResultSet(
          Seq(Comment("john", "yo"), Comment("mary", "lo"))))),
      """|{
         |  "id": "hello-world",
         |  "comments": {
         |    "items": [
         |      {"username": "john", "comment": "yo"},
         |      {"username": "mary", "comment": "lo"}
         |    ]
         |  }
         |}
         |""".stripMargin)
  }

  "Prediction JSON serialization" should "handle categorical predictions" in {
    assertWrites("""{"value": "yes"}""", Prediction(Right("yes")))
  }

  it should "handle real valued predictions" in {
    assertWrites("""{"value": 0.456}""", Prediction(Left(0.456)))
  }

  it should "handle inputs" in {
    assertWrites(
      """{"inputs": {"data": {"text": "Yo"}}, "value": 0.456}""",
      Prediction(Left(0.456), datum = Some(Datum(Json.obj("text" -> "Yo")))))
  }

  it should "handle data_id" in {
    assertWrites(
      """{"data_id": "item-123", "value": "yes"}""",
      Prediction(Right("yes"), datumId = Some("item-123")))
  }

  it should "handle score" in {
    assertWrites(
      """{"value": 0.234, "score": 0.998}""",
      Prediction(Left(0.234), score = Some(0.998)))
  }

  it should "handle createdAt" in {
    assertWrites(
      """{"value": "yes", "created_at": "2005-03-26T12:00:00.000Z"}""",
      Prediction(Right("yes"), createdAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))))
  }

  it should "handle labels" in {
    assertWrites(
      """{"value": "yes", "labels": ["big", "data"]}""",
      Prediction(Right("yes"), labels = Some(Seq("big", "data"))))
  }

  it should "handle comments" in {
    assertWrites(
      """|{
         |  "value": "yes",
         |  "comments": {
         |    "items": [
         |      {"username": "john", "comment": "yo"},
         |      {"username": "mary", "comment": "lo"}
         |    ]
         |  }
         |}
         |""".stripMargin,
      Prediction(
        Right("yes"),
        comments = Some(ResultSet(
          Seq(Comment("john", "yo"), Comment("mary", "lo"))))))

  }

  "Prediction JSON deserialization" should "handle categorical predictions" in {
    assertReads(Prediction(Right("yes")), """{"value": "yes"}""")
  }

  it should "handle real valued predictions" in {
    assertReads(Prediction(Left(0.456)), """{"value": 0.456}""")
  }

  it should "handle inputs" in {
    assertReads(
      Prediction(Left(0.456), datum = Some(Datum(Json.obj("text" -> "Yo")))),
      """{"inputs": {"data": {"text": "Yo"}}, "value": 0.456}""")
  }

  it should "handle data_id" in {
    assertReads(
      Prediction(Right("yes"), datumId = Some("item-123")),
      """{"data_id": "item-123", "value": "yes"}""")
  }

  it should "handle score" in {
    assertReads(
      Prediction(Left(0.234), score = Some(0.998)),
      """{"value": 0.234, "score": 0.998}""")
  }

  it should "handle createdAt" in {
    assertReads(
      Prediction(Right("yes"), createdAt = Some(new DateTime(2005, 3, 26, 12, 0, 0, 0, UTC))),
      """{"value": "yes", "created_at": "2005-03-26T12:00:00.000Z"}""")
  }

  it should "handle labels" in {
    assertReads(
      Prediction(Right("yes"), labels = Some(Seq("big", "data"))),
      """{"value": "yes", "labels": ["big", "data"]}""")
  }

  it should "handle comments" in {
    assertReads(
      Prediction(
        Right("yes"),
        comments = Some(ResultSet(
          Seq(Comment("john", "yo"), Comment("mary", "lo"))))),
      """|{
         |  "value": "yes",
         |  "comments": {
         |    "items": [
         |      {"username": "john", "comment": "yo"},
         |      {"username": "mary", "comment": "lo"}
         |    ]
         |  }
         |}
         |""".stripMargin)
  }
}
