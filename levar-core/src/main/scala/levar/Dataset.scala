package levar

import play.api.libs.json._
import org.joda.time.DateTime

object Dataset {

  /** Data type for classification datasets */
  val ClassificationType = 'c'

  /** Data type for regression datasets */
  val RegressionType = 'r'

  val TypeName = Map(ClassificationType -> "classification", RegressionType -> "regression")
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
 * @param itemSample a sample of items for initialization or display
 * @param experimentSample a sample of experiments for display
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
  itemsSample: Option[ResultSet[Datum]] = None,
  experimentSample: Option[ResultSet[Experiment]] = None,
  labels: Option[Seq[String]] = None,
  comments: Option[ResultSet[Comment[Dataset]]] = None)
