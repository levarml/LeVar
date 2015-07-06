package levar

import play.api.libs.json._
import org.joda.time.DateTime

object Dataset {

  /** Data type for classification datasets */
  val ClassificationType = 'c'

  /** Data type for regression datasets */
  val RegressionType = 'r'
}

/**
 * Dataset data type
 *
 * @param id unique (within an organization) user-provided identifier
 * @param dtype type of dataset -- see [[Dataset.ClassificationType]] and [[Dataset.RegressionType]]
 * @param schema JSON schema for the [[DatasetItem]] data
 * @param name optional user-provided dataset name
 * @param createdAt date the dataset was created in the DB
 * @param updatedAt date the dataset was updated last in the DB
 * @param size number of items [[DatasetItems]] in the dataset
 * @param itemSample a sample of items for initialization or display
 * @param experimentSample a sample of experiments for display
 * @param labels labels applied to the data set
 * @param comments comments made on the data set
 */
case class Dataset(
  id: String,
  dtype: Char,
  schema: JsValue,
  name: Option[String],
  createdAt: Option[DateTime],
  updatedAt: Option[DateTime],
  size: Option[Int],
  itemsSample: Option[ResultSet[Datum]],
  experimentSample: Option[ResultSet[Experiment]],
  labels: Option[ResultSet[String]],
  comments: Option[ResultSet[Comment[Dataset]]])
