package levar

/**
 * Implicit functions for JSON conversions
 */
package object jsn {

  import play.api.libs.json._
  import org.joda.time.DateTimeZone.UTC
  import levar.Dataset.TypeName
  import scala.collection.mutable.Buffer

  private def JObjFields(fields: (String, Json.JsValueWrapper)*) =
    Buffer[(String, Json.JsValueWrapper)](fields: _*)

  implicit def CommentWrites[A](implicit wrtr: Writes[A]): Writes[Comment[A]] = Writes { comment: Comment[A] =>
    val fields = JObjFields(
      "username" -> comment.username,
      "comment" -> comment.comment)

    for (id <- comment.id)
      fields += "id" -> id

    for (path <- comment.path)
      fields += "path" -> path

    for (subject <- comment.subject) {
      fields += "subject" -> Json.obj(
        "type" -> subject.getClass.getSimpleName.toLowerCase,
        "value" -> subject)
    }

    for (ts <- comment.createdAt)
      fields += "created_at" -> ts.withZone(UTC).toString()

    Json.obj(fields: _*)
  }

  implicit val DatumWrites: Writes[Datum] = Writes { datum: Datum =>
    val fields = JObjFields("data" -> datum.data)

    for (value <- datum.value) {
      value match {
        case Left(v) => fields += "value" -> v
        case Right(v) => fields += "value" -> v
      }
    }

    for (id <- datum.id)
      fields += "id" -> id

    for (comments <- datum.comments)
      fields += "comments" -> comments

    for (labels <- datum.labels)
      fields += "labels" -> labels

    for (ts <- datum.createdAt)
      fields += "created_at" -> ts.withZone(UTC).toString()

    Json.obj(fields: _*)
  }

  implicit val DatasetWrites: Writes[Dataset] = Writes { dataset: Dataset =>
    val fields = JObjFields(
      "id" -> dataset.id,
      "type" -> TypeName(dataset.dtype),
      "schema" -> dataset.schema)

    for (name <- dataset.name)
      fields += "name" -> name

    for (ts <- dataset.createdAt)
      fields += "created_at" -> ts.toString()

    for (ts <- dataset.updatedAt)
      fields += "updated_at" -> ts.toString()

    for (num <- dataset.size)
      fields += "size" -> num

    for (items <- dataset.itemsSample)
      fields += "data" -> items

    for (experiments <- dataset.experimentSample)
      fields += "experiments" -> experiments

    for (labels <- dataset.labels)
      fields += "labels" -> labels

    for (comments <- dataset.comments)
      fields += "comments" -> comments

    Json.obj(fields: _*)
  }

  implicit val ExperimentWrites: Writes[Experiment] = Writes { experiment: Experiment =>
    val fields = JObjFields("id" -> experiment.id)

    for (datasets <- experiment.datasets)
      fields += "datasets" -> datasets

    for (ids <- experiment.datasetIds)
      fields += "dataset_ids" -> ids

    for (name <- experiment.name)
      fields += "name" -> name

    for (ts <- experiment.createdAt)
      fields += "created_at" -> ts.withZone(UTC).toString()

    for (ts <- experiment.updatedAt)
      fields += "updated_at" -> ts.withZone(UTC).toString()

    for (size <- experiment.size)
      fields += "size" -> size

    for (predictions <- experiment.predictionsSample)
      fields += "predictions" -> predictions

    for (labels <- experiment.labels)
      fields += "labels" -> labels

    for (comments <- experiment.comments)
      fields += "comments" -> comments

    Json.obj(fields: _*)
  }

  implicit val PredictionWrites: Writes[Prediction] = Writes { prediction: Prediction =>
    val fields = JObjFields()

    prediction.value match {
      case Left(v) => fields += "value" -> v
      case Right(v) => fields += "value" -> v
    }

    for (datum <- prediction.datum)
      fields += "inputs" -> datum

    for (datumId <- prediction.datumId)
      fields += "data_id" -> datumId

    for (experiment <- prediction.experiment)
      fields += "experiment" -> experiment

    for (score <- prediction.score)
      fields += "score" -> score

    for (ts <- prediction.createdAt)
      fields += "created_at" -> ts.withZone(UTC).toString()

    for (labels <- prediction.labels)
      fields += "labels" -> labels

    for (comments <- prediction.comments)
      fields += "comments" -> comments

    Json.obj(fields: _*)
  }

  implicit def ResultSetWrites[A](implicit wrtr: Writes[A]): Writes[ResultSet[A]] = Writes { resultSet: ResultSet[A] =>
    val fields = JObjFields(
      "items" -> resultSet.items,
      "size" -> resultSet.items.size,
      "path" -> resultSet.path)

    for (num <- resultSet.total)
      fields += "total" -> num

    for (path <- resultSet.nextPath)
      fields += "next_path" -> path

    Json.obj(fields: _*)
  }
}
