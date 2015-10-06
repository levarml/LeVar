package levar

import org.joda.time.DateTime

/**
 * Experiment data type
 *
 * @param id the user provided identifier, unique within the organization
 * @param datasetId the ID of the datasets associated with the experiment
 * @param name optional user provided name of the experiment
 * @param createdAt the date the experiment was stored in the DB
 * @param updatedAd the date the experiment was updated last
 * @param size the number of [[Prediction]]s in the experiment
 * @param itemsSample a sample of the [[Prediction]]s for display
 * @param labels the labels in the experiment
 * @param comments the comments made of this experiment
 */
case class Experiment(
  id: String,
  datasetId: Option[String] = None,
  name: Option[String] = None,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None,
  size: Option[Int] = None,
  labels: Option[Seq[String]] = None,
  comments: Option[ResultSet[Comment]] = None)

object Experiment {

  case class Update(id: Option[String] = None, data: Option[Seq[Prediction]] = None)
}
