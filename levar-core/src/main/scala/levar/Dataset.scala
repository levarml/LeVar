package levar

import play.api.libs.json._
import org.joda.time.DateTime

object Dataset {

  /** Data type for classification datasets */
  val ClassificationType = 'c'

  /** Data type for regression datasets */
  val RegressionType = 'r'

  val TypeName = Map(ClassificationType -> "classification", RegressionType -> "regression")

  case class Update(id: Option[String])

  /**
   * Validate the schema of a dataset.
   *
   * We're not implementing all of JSON schema here, or buying into
   * a library which does. Just checking for the fields and values
   * we'll need to validate data points later on.
   */
  def validSchema(schema: JsValue): Boolean = {
    schema match {
      case JsObject(fields) => {
        fields.find(_._1 == "properties").map(_._2) match {
          case Some(JsObject(props)) => {
            props.nonEmpty && {
              props.map(_._2).forall { spec =>
                spec match {
                  case JsObject(details) => {
                    details.find(_._1 == "type").map(_._2) match {
                      case Some(JsString(v)) => v == "string" || v == "number"
                      case _ => false
                    }
                  }
                  case _ => false
                }
              }
            }
          }
          case _ => false
        }
      }
      case _ => false
    }
  }
}

/**
 * Dataset data type
 *
 * @param id unique (within an organization) user-provided identifier
 * @param dtype type of dataset -- see [[Dataset.ClassificationType]] and [[Dataset.RegressionType]]
 * @param schema JSON schema for the [[Datum]] data
 * @param name optional user-provided dataset name
 * @param createdAt date the dataset was created in the DB
 * @param updatedAt date the dataset was updated last in the DB
 * @param size number of items [[Datum]]s in the dataset
 * @param labels labels applied to the data set
 * @param comments comments made on the data set
 */
case class Dataset(
    id: String,
    dtype: Char,
    schema: JsValue,
    name: Option[String] = None,
    createdAt: Option[DateTime] = None,
    updatedAt: Option[DateTime] = None,
    size: Option[Int] = None,
    labels: Option[Seq[String]] = None,
    comments: Option[ResultSet[Comment]] = None) {

  import Dataset._

  require(dtype == ClassificationType || dtype == RegressionType, "invalid type")
  require(validSchema(schema), "invalid schema")

  def typeName = TypeName(dtype)
}
