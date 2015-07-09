package levar.json

import org.scalatest._
import play.api.libs.json._
import levar._
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC

class JsnSpec extends FlatSpec {

  def assertWrites[A](expected: String, input: A)(implicit wrtr: Writes[A]) {
    assert(Json.parse(expected) == Json.toJson(input))
  }

  "Comment JSON serialization" should "create basic JSON" in {
    assertWrites(
      """{"username":"john", "comment": "Hello world"}""",
      Comment[String]("john", "Hello world"))
  }

  it should "handle IDs" in {
    assertWrites(
      """{"id": "2938abb3", "username": "john", "comment": "Hello world"}""",
      Comment[String]("john", "Hello world", id = Some("2938abb3")))
  }

  it should "handle paths" in {
    assertWrites(
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
    assertWrites(
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
    assertWrites(
      s"""{"username": "john", "comment": "Hello world!", "created_at": "${timestampIso}"}""",
      Comment[String]("john", "Hello world!", createdAt = Some(timestamp)))
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
    assertWrites(
      """{"items": ["big", "data"], "size": 2, "path": "/api/foo/dataset/dataset-1/labels"}""",
      ResultSet(Seq("big", "data"), "/api/foo/dataset/dataset-1/labels"))
  }

  it should "handle total" in {
    assertWrites(
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
    assertWrites(
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

  it should "handle items" in {
    assertWrites(
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "data": {
         |    "size": 2,
         |    "path": "/api/foo-org/dataset/bar/data",
         |    "items": [
         |      {"data": {"text": "Hello world", "score": 0.4}},
         |      {"data": {"text": "So long, folks", "score": 0.3}}
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
        itemsSample = Some(ResultSet(
          Seq(
            Datum(Json.obj("text" -> "Hello world", "score" -> 0.4)),
            Datum(Json.obj("text" -> "So long, folks", "score" -> 0.3))),
          "/api/foo-org/dataset/bar/data"))))
  }

  it should "handle experiments" in {
    assertWrites(
      """|{
         |  "id": "hello-dataset",
         |  "type": "classification",
         |  "schema": {"properties": {"text": {"type": "string"}, "score": {"type": "number"}}},
         |  "experiments": {
         |    "size": 2,
         |    "path": "/api/foo-org/dataset/bar/experiments",
         |    "items": [
         |      {"id": "hello-world-1"},
         |      {"id": "hello-world-2"}
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
        experimentSample = Some(ResultSet(
          Seq(Experiment("hello-world-1"), Experiment("hello-world-2")),
          "/api/foo-org/dataset/bar/experiments"))))
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
         |    "size": 2,
         |    "path": "/api/foo-org/dataset/bar/comments",
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
          Seq(Comment("john", "This is cool"), Comment("mary", "Hello world")),
          "/api/foo-org/dataset/bar/comments"))))
  }

  "Experiment JSON serialization" should "create basic JSON" in {
    assertWrites(
      """{"id": "hello-world"}""",
      Experiment("hello-world"))
  }

  it should "handle datasets" in {
    assertWrites(
      """|{
         |  "id": "hello-world",
         |  "datasets": [
         |    {
         |      "id": "hello-dataset",
         |      "type": "classification",
         |      "schema": {"properties": {"text": {"type": "string"}}}
         |     }
         |  ]
         |}""".stripMargin,
      Experiment("hello-world",
        datasets = Some(Seq(Dataset(
          "hello-dataset",
          'c',
          Json.obj("properties" ->
            Json.obj("text" ->
              Json.obj("type" -> "string"))))))))
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

  it should "handle predictions" in {
    assertWrites(
      """|{
         |  "id": "hello-world",
         |  "predictions": {
         |    "size": 2,
         |    "items": [{"value": "yes"}, {"value": "no"}],
         |    "path": "/api/foo-org/experiments/hello-world/predictions"
         |  }
         |}
         |""".stripMargin,
      Experiment(
        "hello-world",
        predictionsSample = Some(ResultSet(
          Seq(Prediction(Right("yes")), Prediction(Right("no"))),
          "/api/foo-org/experiments/hello-world/predictions"))))
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
         |    "size": 2,
         |    "items": [
         |      {"username": "john", "comment": "yo"},
         |      {"username": "mary", "comment": "lo"}
         |    ],
         |    "path": "/api/foo-org/experiments/hello-world/comments"
         |  }
         |}
         |""".stripMargin,
      Experiment(
        "hello-world",
        comments = Some(ResultSet(
          Seq(Comment("john", "yo"), Comment("mary", "lo")),
          "/api/foo-org/experiments/hello-world/comments"))))
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

  it should "handle experiment" in {
    assertWrites(
      """{"value": "yes", "experiment": {"id": "hello-world"}}""",
      Prediction(Right("yes"), experiment = Some(Experiment("hello-world"))))
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
         |    "size": 2,
         |    "items": [{"username": "john", "comment": "yo"}, {"username": "mary", "comment": "lo"}],
         |    "path": "/api/foo-org/experiment/hello-world/1234abcde/comments"
         |  }
         |}
         |""".stripMargin,
      Prediction(
        Right("yes"),
        comments = Some(ResultSet(
          Seq(Comment("john", "yo"), Comment("mary", "lo")),
          "/api/foo-org/experiment/hello-world/1234abcde/comments"))))

  }
}
