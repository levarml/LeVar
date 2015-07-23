package levar

import org.joda.time.DateTime

object Dataset {

  /** Marker for types of datasets */
  sealed trait DatasetType {
    def name: String
    def code: Char
  }

  /** Marker for classification datasets */
  case object ClassificationType extends DatasetType {
    val name = "classification"
    val code = 'c'
  }

  /** Marker for regression datasets */
  case object RegressionType extends DatasetType {
    val name = "regression"
    val code = 'r'
  }

  /** Class of updates a client can make to a dataset */
  case class Update(id: Option[String] = None)

  sealed trait DataFieldType
  case object StringField extends DataFieldType
  case object NumberField extends DataFieldType

  /** Class of data validators */
  case class DataValidator(fields: (String, DataFieldType)*)
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
  dtype: Dataset.DatasetType,
  schema: Dataset.DataValidator,
  name: Option[String] = None,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None,
  size: Option[Int] = None,
  labels: Option[Seq[String]] = None,
  comments: Option[ResultSet[Comment]] = None)
