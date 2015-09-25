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
  case class Update(
    id: Option[String] = None,
    data: Option[Seq[Datum]] = None)

  sealed trait DataFieldType {
    def name: String
  }
  case object StringField extends DataFieldType { val name = "text" }
  case object NumberField extends DataFieldType { val name = "numeric" }

  /** Class of data validators */
  case class DataValidator(fields: (String, DataFieldType)*)

  case class RegressionSummaryStats(
    minVal: Double,
    maxVal: Double,
    mean: Double,
    stddev: Double,
    median: Double,
    p10: Double,
    p90: Double)
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
 * @param classes the classes (for a classification dataset)
 * @param classCounts the classes and counts of the classes (for a classification dataset)
 * @param summaryStats summary statistics for
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
  classes: Option[Set[String]] = None,
  classCounts: Option[Map[String, Int]] = None,
  summaryStats: Option[Dataset.RegressionSummaryStats] = None,
  labels: Option[Seq[String]] = None,
  comments: Option[ResultSet[Comment]] = None)
