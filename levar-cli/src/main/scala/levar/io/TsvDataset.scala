package levar.io

import levar.data.{ TabularDataset, LevarDataError }
import levar.Dataset.{ RegressionType, ClassificationType, NumberField, StringField }
import scala.io.Source

object TsvDataset {

  def fromSource(name: String, src: Source): TabularDataset = {
    fromLines(name, src.getLines)
  }

  def fromLines(name: String, lines: Iterator[String]): TabularDataset = {
    fromArrays(name, lines.map(_.split("\\t")))
  }

  def fromArrays(name: String, arrays: Iterator[Array[String]]): TabularDataset = {
    val header: Array[String] = {
      val headerSet = arrays.take(1).toList
      if (headerSet.isEmpty) {
        throw new LevarDataError("Data collection is empty")
      }
      headerSet.head
    }

    if (header.indexWhere(_.equalsIgnoreCase("id")) >= 0) {
      throw new LevarDataError("Provided ID columns net yet supported")
    }

    val (dtype, valCol) = {
      val scoreIdx = header.indexWhere(_.equalsIgnoreCase("score"))
      val classIdx = header.indexWhere(_.equalsIgnoreCase("class"))
      if (scoreIdx >= 0 && classIdx < 0) {
        (RegressionType, scoreIdx)
      } else if (scoreIdx < 0 && classIdx >= 0) {
        (ClassificationType, classIdx)
      } else if (scoreIdx >= 0 && classIdx >= 0) {
        throw new LevarDataError("Cannot have both 'class' and 'score' fields, indeterminate dataset type")
      } else {
        throw new LevarDataError("Must have either 'class' or 'score' field, indeterminate dataset type")
      }
    }

    val buf = arrays.take(1000).toVector
    val dataFields = for ((f, i) <- header.zipWithIndex if i != valCol) yield {
      try {
        buf.map(_(i).toDouble)
        (i, f, NumberField)
      } catch {
        case _: NumberFormatException => (i, f, StringField)
      }
    }

    new TabularDataset(name, valCol, dtype, dataFields, buf.toIterator ++ arrays)
  }
}
