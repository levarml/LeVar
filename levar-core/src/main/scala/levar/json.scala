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
  import Dataset.{ Update => DatasetUpdate, DatasetType, ClassificationType, RegressionType, DataValidator, NumberField, StringField, DataFieldType }
  import Experiment.{ Update => ExperimentUpdate }

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

  implicit lazy val DatasetUpdateFormat: Format[DatasetUpdate] = (
    (__ \ "id").formatNullable[String] and
    (__ \ "data").formatNullable[Seq[Datum]]
  )(DatasetUpdate.apply _, unlift(DatasetUpdate.unapply))

  implicit lazy val ExperimentFormat: Format[Experiment] = (
    (__ \ "id").format[String] and
    (__ \ "dataset_id").formatNullable[String] and
    (__ \ "dataset_type").formatNullable[Dataset.DatasetType] and
    (__ \ "name").formatNullable[String] and
    (__ \ "created_at").formatNullable[DateTime] and
    (__ \ "updated_at").formatNullable[DateTime] and
    (__ \ "size").formatNullable[Int] and
    (__ \ "dataset_size").formatNullable[Int] and
    (__ \ "classification_results").formatNullable[Experiment.ClassificationResults] and
    (__ \ "regression_results").formatNullable[Experiment.RegressionResults] and
    (__ \ "labels").formatNullable[Seq[String]] and
    (__ \ "comments").formatNullable[ResultSet[Comment]]
  )(Experiment.apply _, unlift(Experiment.unapply))

  implicit lazy val PredictionFormat: Format[Prediction] = (
    (__ \ "data_id").format[String] and
    (__ \ "value").format[Either[Double, String]] and
    (__ \ "inputs").formatNullable[Datum] and
    (__ \ "score").formatNullable[Double] and
    (__ \ "id").formatNullable[String] and
    (__ \ "created_at").formatNullable[DateTime] and
    (__ \ "labels").formatNullable[Seq[String]] and
    (__ \ "comments").formatNullable[ResultSet[Comment]]
  )(Prediction.apply _, unlift(Prediction.unapply))

  implicit lazy val ExperimentUpdateFormat: Format[ExperimentUpdate] = (
    (__ \ "id").formatNullable[String] and
    (__ \ "predictions").formatNullable[Seq[Prediction]]
  )(ExperimentUpdate.apply _, unlift(ExperimentUpdate.unapply))

  implicit def Tuple3Format[A, B, C](implicit fmtA: Format[A], fmtB: Format[B], fmtC: Format[C]): Format[(A, B, C)] = {
    val reads = Reads[(A, B, C)] { js =>
      val JsArray(ls) = js
      JsSuccess((ls(0).as[A], ls(1).as[B], ls(2).as[C]))
    }
    val writes = Writes[(A, B, C)]((x: (A, B, C)) => { JsArray(Seq(Json.toJson(x._1), Json.toJson(x._2), Json.toJson(x._3))) })
    Format(reads, writes)
  }

  implicit lazy val ExperimentClassificationResults: Format[Experiment.ClassificationResults] = (
    (__ \ "labels").format[Seq[String]] and
    (__ \ "class_counts").format[Seq[(String, String, Int)]]
  )(Experiment.ClassificationResults.apply _, unlift(Experiment.ClassificationResults.unapply))

  implicit lazy val ExperimentRegressionResults: Format[Experiment.RegressionResults] = (
    (__ \ "rmse").format[Double] and
    (__ \ "mean_abs_err").format[Double] and
    (__ \ "median_abs_err").format[Double] and
    (__ \ "10_percentile_abs_err").format[Double] and
    (__ \ "90_percentile_abs_err").format[Double]
  )(Experiment.RegressionResults.apply _, unlift(Experiment.RegressionResults.unapply))
}
