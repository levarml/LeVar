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
  import scala.collection.mutable.Buffer
  import Dataset._

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

  implicit val DatasetTypeFormat = new Format[DatasetType] {
    def reads(js: JsValue): JsResult[DatasetType] = js match {
      case JsString("classification") => JsSuccess(ClassificationType)
      case JsString("regression") => JsSuccess(RegressionType)
      case _ => JsError("could not parse classification type: " + js.toString)
    }
    def writes(c: DatasetType) = c match {
      case ClassificationType => JsString("classification")
      case RegressionType => JsString("regression")
    }
  }

  implicit val DataValidatorFormat = new Format[DataValidator] {
    def reads(js: JsValue): JsResult[DataValidator] = js match {
      case JsObject(things) => {
        things.find(_._1 == "properties").map(_._2) match {
          case Some(JsObject(fieldsJs)) => {
            val errs = Buffer.empty[String]
            val fields = Buffer.empty[(String, DataFieldType)]
            fieldsJs foreach {
              case (key, dtype) =>
                dtype match {
                  case JsObject(fieldData) => {
                    fieldData.find(_._1 == "type").map(_._2) match {
                      case Some(JsString("string")) => fields += ((key, StringField))
                      case Some(JsString("number")) => fields += ((key, NumberField))
                      case _ => errs += s"unrecognized field type for $key, ${dtype.toString}"
                    }
                  }
                  case _ => errs += ("need to include \"type\" field for " + key)
                }
            }
            if (errs.isEmpty) {
              JsSuccess(DataValidator(fields: _*))
            } else {
              JsError(errs.mkString("; "))
            }
          }
          case _ => JsError("require \"properties\" field")
        }
      }
      case _ => JsError("require JSON object")
    }

    def writes(v: DataValidator): JsValue = {
      val fields: Seq[(String, JsValue)] = v.fields map {
        case (key, fieldType) =>
          fieldType match {
            case StringField => (key, Json.obj("type" -> "string"))
            case NumberField => (key, Json.obj("type" -> "number"))
          }
      }
      Json.obj("properties" -> Map(fields: _*))
    }
  }

  implicit lazy val RegressionSummaryStats: Format[Dataset.RegressionSummaryStats] = (
    (__ \ "min").format[Double] and
    (__ \ "max").format[Double] and
    (__ \ "mean").format[Double] and
    (__ \ "stddev").format[Double] and
    (__ \ "median").format[Double] and
    (__ \ "percentile_10").format[Double] and
    (__ \ "percentile_90").format[Double]
  )(Dataset.RegressionSummaryStats.apply _, unlift(Dataset.RegressionSummaryStats.unapply))

  implicit lazy val DatasetFormat: Format[Dataset] = (
    (__ \ "id").format[String] and
    (__ \ "type").format[DatasetType] and
    (__ \ "schema").format[DataValidator] and
    (__ \ "name").formatNullable[String] and
    (__ \ "created_at").formatNullable[DateTime] and
    (__ \ "updated_at").formatNullable[DateTime] and
    (__ \ "size").formatNullable[Int] and
    (__ \ "classes").formatNullable[Set[String]] and
    (__ \ "class_counts").formatNullable[Map[String, Int]] and
    (__ \ "summary_stats").formatNullable[Dataset.RegressionSummaryStats] and
    (__ \ "labels").formatNullable[Seq[String]] and
    (__ \ "comments").formatNullable[ResultSet[Comment]]
  )(Dataset.apply _, unlift(Dataset.unapply))

  implicit lazy val DatasetUpdateFormat: Format[Update] = (
    (__ \ "id").formatNullable[String] and
    (__ \ "data").formatNullable[Seq[Datum]]
  )(Update.apply _, unlift(Update.unapply))

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
