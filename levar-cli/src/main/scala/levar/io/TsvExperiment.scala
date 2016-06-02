package levar.io

import scala.io._
import levar.data.{TabularExperiment, LevarDataError}
import levar.Dataset.{DatasetType, ClassificationType, RegressionType}

object TsvExperiment {

  def fromSource(
      dtype: DatasetType, name: String, src: Source): TabularExperiment = {
    fromLines(dtype, name, src.getLines)
  }

  def fromLines(dtype: DatasetType,
                name: String,
                lines: Iterator[String]): TabularExperiment = {
    fromArrays(dtype, name, lines.map(_.split("\\t")))
  }

  def fromArrays(dtype: DatasetType,
                 name: String,
                 arrays: Iterator[Array[String]]): TabularExperiment = {
    val header: Array[String] = {
      val headerSet = arrays.take(1).toList
      if (headerSet.isEmpty) {
        throw new LevarDataError("Data collection is empty")
      }
      headerSet.head
    }

    val idCol = header.indexWhere(_.equalsIgnoreCase("id"))
    if (idCol < 0) {
      throw new LevarDataError("Must provide ID column")
    }

    val predCol = dtype match {
      case RegressionType => {
          val c = header.indexWhere(_.equalsIgnoreCase("score"))
          if (c < 0) {
            throw new LevarDataError("Must provide a 'score' column")
          }
          c
        }
      case ClassificationType => {
          val c = header.indexWhere(_.equalsIgnoreCase("class"))
          if (c < 0) {
            throw new LevarDataError("Must provide a 'class' column")
          }
          c
        }
    }

    new TabularExperiment(dtype, name, idCol, predCol, arrays)
  }
}
