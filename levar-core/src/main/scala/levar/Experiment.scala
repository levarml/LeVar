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
    comments: Option[ResultSet[Comment]] = None) {

  def displayName = datasetId match {
    case Some(ds) => s"$ds/$id"
    case None => id
  }
}

object Experiment {

  case class Update(id: Option[String] = None, data: Option[Seq[Prediction]] = None)

  case class ClassificationResults(classes: Seq[String], classCounts: Seq[(String, String, Int)]) {

    lazy val totalCorrect: Int = classCounts.filter(x => x._1 == x._2).map(_._3).sum

    private lazy val m: Map[(String, String), Int] = classCounts.map(x => (x._1, x._2) -> x._3).toMap

    lazy val total: Int = classes.map(goldSum(_)).sum

    lazy val overallAccuracy: Double = totalCorrect.toDouble / total

    def num(gold: String, pred: String): Int = m.getOrElse((gold, pred), 0)

    def goldSum(gold: String): Int = classes.map(num(gold, _)).sum

    def predSum(pred: String): Int = classes.map(num(_, pred)).sum

    def precision(cls: String): Double = num(cls, cls).toDouble / predSum(cls)

    def recall(cls: String): Double = num(cls, cls).toDouble / goldSum(cls)

    def f1(cls: String): Double = {
      val p = precision(cls)
      val r = recall(cls)
      2 * p * r / (p + r)
    }
  }
}
