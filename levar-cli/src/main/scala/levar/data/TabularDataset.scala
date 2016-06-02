package levar.data

import levar.{Dataset, Datum}
import play.api.libs.json._
import scala.collection.mutable.Buffer

class TabularDataset(name: String,
                     valueCol: Int,
                     dtype: Dataset.DatasetType,
                     dataCols: Seq[(Int, String, Dataset.DataFieldType)],
                     rows: Iterator[Array[String]]) {

  def asDataset: Dataset = {
    val validatorFields = dataCols.map {
      case (_, field, fieldType) => (field, fieldType)
    }
    new Dataset(name, dtype, Dataset.DataValidator(validatorFields: _*))
  }

  def data: Iterator[Datum] = rows map { row =>
    val value = dtype match {
      case Dataset.RegressionType => {
          Left(row(valueCol).toDouble)
        }
      case Dataset.ClassificationType => {
          Right(row(valueCol))
        }
    }
    val jsData: Seq[(String, JsValue)] = {
      for ((idx, field, fieldType) <- dataCols) yield {
        val jsval = fieldType match {
          case Dataset.StringField => JsString(row(idx))
          case Dataset.NumberField => JsNumber(row(idx).toDouble)
        }
        field -> jsval
      }
    }
    Datum(JsObject(jsData), value = Some(value))
  }
}
