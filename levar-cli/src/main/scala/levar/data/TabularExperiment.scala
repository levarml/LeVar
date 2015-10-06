package levar.data

import levar.Experiment
import levar.Prediction
import levar.Dataset.{ DatasetType, ClassificationType, RegressionType }

class TabularExperiment(dtype: DatasetType, name: String, idCol: Int, predCol: Int, arrays: Iterator[Array[String]]) {
  require(idCol >= 0)
  require(predCol >= 0)
  require(idCol != predCol)

  def asExperiment = Experiment(name)

  def data = dtype match {
    case RegressionType => arrays.map { a => Prediction(a(idCol), Left(a(predCol).toDouble)) }
    case ClassificationType => arrays.map { a => Prediction(a(idCol), Right(a(predCol))) }
  }
}
