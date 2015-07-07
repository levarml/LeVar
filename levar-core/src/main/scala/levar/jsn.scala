package levar

/**
 * Implicit functions for JSON conversions
 */
package object jsn {

  import play.api.libs.json._
  import org.joda.time.DateTimeZone.UTC
  import levar.Dataset.TypeName

  implicit def CommentWrites[A](implicit wrtr: Writes[A]): Writes[Comment[A]] = Writes { comment: Comment[A] =>
    var obj = Json.obj("username" -> comment.username, "comment" -> comment.comment)
    comment.id.foreach { id =>
      obj ++= Json.obj("id" -> id)
    }
    comment.path.foreach { path =>
      obj ++= Json.obj("path" -> path)
    }
    comment.subject.foreach { subject =>
      obj ++= Json.obj("subject" ->
        Json.obj(
          "type" -> subject.getClass.getSimpleName.toLowerCase,
          "value" -> Json.toJson(subject)))
    }
    comment.createdAt.foreach { timestamp =>
      obj ++= Json.obj("created_at" -> timestamp.withZone(UTC).toString)
    }
    obj
  }

  implicit val DatumWrites: Writes[Datum] = Writes { datum: Datum =>
    var obj = Json.obj("data" -> datum.data)
    datum.value.foreach { value =>
      value match {
        case Left(v) => obj ++= Json.obj("value" -> v)
        case Right(v) => obj ++= Json.obj("value" -> v)
      }
    }
    datum.id.foreach { id => obj ++= Json.obj("id" -> id) }
    datum.comments.foreach { comments => obj ++= Json.obj("comments" -> comments) }
    datum.labels.foreach { labels => obj ++= Json.obj("labels" -> labels) }
    obj
  }

  implicit val DatasetWrites: Writes[Dataset] = Writes { dataset: Dataset =>
    var obj = Json.obj(
      "id" -> dataset.id,
      "type" -> TypeName(dataset.dtype),
      "schema" -> dataset.schema)
    obj
  }

  implicit val ExperimentWrites: Writes[Experiment] = Writes { experiment: Experiment =>
    var obj = Json.obj("id" -> experiment.id)
    obj
  }

  implicit def ResultSetWrites[A](implicit wrtr: Writes[A]): Writes[ResultSet[A]] = Writes { resultSet: ResultSet[A] =>
    var obj = Json.obj(
      "items" -> resultSet.items,
      "size" -> resultSet.items.size,
      "path" -> resultSet.path)
    resultSet.total.foreach { total => obj ++= Json.obj("total" -> total) }
    resultSet.nextPath.foreach { nextPath => obj ++= Json.obj("next_path" -> nextPath) }
    obj
  }
}
