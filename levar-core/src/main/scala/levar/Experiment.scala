package levar

import org.joda.time.DateTime

/**
 * Experiment data type
 *
 * @param id the user provided identifier, unique within the organization
 * @param datasets the datasets associated with the experiment, for display
 * @param datasetIds the IDs of the datasets associated with the experiment, for creation requests
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
  datasets: Option[Seq[Dataset]] = None,
  datasetIds: Option[Seq[String]] = None,
  name: Option[String] = None,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None,
  size: Option[Int] = None,
  predictionsSample: Option[ResultSet[Prediction]] = None,
  labels: Option[Seq[String]] = None,
  comments: Option[ResultSet[Comment[Experiment]]] = None)
