package levar

/**
 * Implicit functions for JSON conversions
 */
package object json {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  import org.joda.time.DateTime
  import org.joda.time.format._
  import org.joda.time.DateTimeZone.UTC

  implicit val JodaDateFormat = new Format[DateTime] {
    private val tsparser = ISODateTimeFormat.dateTimeParser()
    private val reader = Reads.StringReads.map(tsparser.parseDateTime(_).withZone(UTC))
    def reads(js: JsValue): JsResult[DateTime] = reader.reads(js)

    def writes(ts: DateTime): JsValue = JsString(ts.withZone(UTC).toString())
  }

  implicit val CommentFormats: Format[Comment] = (
    (__ \ "username").format[String] and
    (__ \ "comment").format[String] and
    (__ \ "id").formatNullable[String] and
    (__ \ "path").formatNullable[String] and
    (__ \ "subject").formatNullable((
      (__ \ "type").format[String] and
      (__ \ "value").format[JsValue]
    )(Tuple2.apply[String, JsValue], identity[(String, JsValue)])) and
    (__ \ "created_at").formatNullable[DateTime]
  )(Comment.apply, unlift(Comment.unapply))

  implicit val NumStrFormat = new Format[Either[Double, String]] {
    def reads(js: JsValue): JsResult[Either[Double, String]] = js match {
      case JsNumber(n) => JsSuccess(Left(n.toDouble))
      case JsString(s) => JsSuccess(Right(s))
      case _ => JsError("could not parse string or number")
    }
    def writes(v: Either[Double, String]) = v match {
      case Left(n) => JsNumber(n)
      case Right(s) => JsString(s)
    }
  }

  implicit val DatumFormat: Format[Datum] = (
    (__ \ "data").format[JsValue] and
    (__ \ "value").formatNullable[Either[Double, String]] and
    (__ \ "id").formatNullable[String] and
    (__ \ "created_at").formatNullable[DateTime] and
    (__ \ "labels").formatNullable[Seq[String]] and
    (__ \ "comments").formatNullable[ResultSet[Comment]]
  )(Datum.apply _, unlift(Datum.unapply))

  implicit def ResultSetFormat[A](implicit fmt: Format[A]): Format[ResultSet[A]] = (
    (__ \ "items").format[Seq[A]] and
    (__ \ "path").formatNullable[String] and
    (__ \ "total").formatNullable[Int] and
    (__ \ "next_path").formatNullable[String]
  )(ResultSet.apply _, unlift(ResultSet.unapply))

  val DatasetTypeFormat = new Format[Char] {
    def reads(js: JsValue): JsResult[Char] = js match {
      case JsString("classification") => JsSuccess(Dataset.ClassificationType)
      case JsString("regression") => JsSuccess(Dataset.RegressionType)
      case _ => JsError("could not parse classification type")
    }
    def writes(c: Char) = c match {
      case Dataset.ClassificationType => JsString("classification")
      case Dataset.RegressionType => JsString("regression")
      case _ => JsString("unknown")
    }
  }

  implicit lazy val DatasetFormat: Format[Dataset] = (
    (__ \ "id").format[String] and
    (__ \ "type").format(DatasetTypeFormat) and
    (__ \ "schema").format[JsValue] and
    (__ \ "name").formatNullable[String] and
    (__ \ "created_at").formatNullable[DateTime] and
    (__ \ "updated_at").formatNullable[DateTime] and
    (__ \ "size").formatNullable[Int] and
    (__ \ "labels").formatNullable[Seq[String]] and
    (__ \ "comments").formatNullable[ResultSet[Comment]]
  )(Dataset.apply _, unlift(Dataset.unapply))

  implicit lazy val DatasetUpdateFormat: Format[Dataset.Update] = (
    (__ \ "id").formatNullable[String].inmap(Dataset.Update.apply _, unlift(Dataset.Update.unapply)))

  implicit lazy val ExperimentFormat: Format[Experiment] = (
    (__ \ "id").format[String] and
    (__ \ "dataset_ids").formatNullable[Seq[String]] and
    (__ \ "name").formatNullable[String] and
    (__ \ "created_at").formatNullable[DateTime] and
    (__ \ "updated_at").formatNullable[DateTime] and
    (__ \ "size").formatNullable[Int] and
    (__ \ "labels").formatNullable[Seq[String]] and
    (__ \ "comments").formatNullable[ResultSet[Comment]]
  )(Experiment.apply _, unlift(Experiment.unapply))

  implicit lazy val PredictionFormat: Format[Prediction] = (
    (__ \ "value").format[Either[Double, String]] and
    (__ \ "inputs").formatNullable[Datum] and
    (__ \ "data_id").formatNullable[String] and
    (__ \ "score").formatNullable[Double] and
    (__ \ "created_at").formatNullable[DateTime] and
    (__ \ "labels").formatNullable[Seq[String]] and
    (__ \ "comments").formatNullable[ResultSet[Comment]]
  )(Prediction.apply _, unlift(Prediction.unapply))
}
