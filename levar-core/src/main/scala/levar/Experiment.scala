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
  datasetType: Option[Dataset.DatasetType] = None,
  name: Option[String] = None,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None,
  size: Option[Int] = None,
  datasetSize: Option[Int] = None,
  classificationResults: Option[Experiment.ClassificationResults] = None,
  labels: Option[Seq[String]] = None,
  comments: Option[ResultSet[Comment]] = None)

object Experiment {

  case class Update(id: Option[String] = None, data: Option[Seq[Prediction]] = None)

  case class ClassificationResults(classes: Seq[String], classCounts: Seq[(String, String, Int)]) {

    @transient private lazy val m = classCounts.map(x => (x._1, x._2) -> x._3).toMap

    def num(gold: String, pred: String): Int = m((gold, pred))

    @transient lazy val total: Int = classes.map(colSum(_)).sum

    def colSum(gold: String) = classes.map(num(gold, _)).sum

    def rowSum(pred: String) = classes.map(num(_, pred)).sum
  }

}
