package levar

import org.joda.time._
import org.joda.time.format.DateTimeFormat

object Format {
  def datefmt(d: DateTime): String =
    DateTimeFormat.forPattern("yyyy-MM-dd").print(d.withZone(DateTimeZone.getDefault))

  def datetimefmt(d: DateTime): String =
    DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").print(d.withZone(DateTimeZone.getDefault))

  def datasetRStoString(datasetRS: ResultSet[Dataset]): String = {
    if (datasetRS.nonEmpty) {
      val sb = new StringBuilder()
      sb ++=
        """|Dataset                    | Updated    | Type       |    Items
           |---------------------------|------------|------------|----------""".stripMargin
      for (dataset <- datasetRS) {
        sb ++= "\n"
        val displayName = if (dataset.id.size > 26) {
          dataset.id.take(23) + "..."
        } else {
          dataset.id + (" " * (26 - dataset.id.size))
        }
        val date = dataset.updatedAt match {
          case Some(d) => datefmt(d)
          case None => " " * 10
        }
        val dtype = {
          val t = dataset.dtype match {
            case Dataset.ClassificationType => "classify"
            case Dataset.RegressionType => "regression"
          }
          t + (" " * (10 - t.size))
        }
        val n = dataset.size.getOrElse(0)
        sb ++= f"$displayName | $date | $dtype | $n%8d"
      }
      sb.toString
    } else {
      s"No datasets"
    }
  }

  def datumRStoString(datumRS: ResultSet[Datum]): String = {
    // TODO change
    s"Reults: ${datumRS.total.getOrElse(datumRS.items.size)}"
  }

  private def fieldfmt(field: String, n: Int = 20) = {
    if (field.size > n) {
      field.take(n - 3) + "..."
    } else {
      field ++ (" " * (n - field.size))
    }
  }

  def commentToString(comment: Comment): String = {
    val cdatefmt = comment.createdAt.map(datetimefmt).getOrElse("no date")
    val commentfmt = comment.comment.replaceAll("\n", "  \n")
    s"""
    |- ${comment.username}
    |  $cdatefmt
    |  $commentfmt""".stripMargin

  }

  def datasetToString(dataset: Dataset): String = {
    val sb = new StringBuilder()
    sb ++= s"Dataset: ${dataset.id}"
    sb ++= s"\nType:    ${dataset.dtype.name}"
    sb ++= s"\nItems:   ${dataset.size.map(_.toString).getOrElse("N/A")}"
    sb ++= s"\nCreated: ${dataset.createdAt.map(datetimefmt).getOrElse("N/A")}"
    sb ++= s"\nUpdated: ${dataset.updatedAt.map(datetimefmt).getOrElse("N/A")}"
    sb ++= s"\nSchema:"
    for ((field, fieldType) <- dataset.schema.fields) {
      sb ++= f"\n- ${fieldfmt(field)} ${fieldType.name}"
    }
    for (classCounts <- dataset.classCounts) {
      sb ++= "\nClasses:"
      for ((cls, count) <- classCounts) {
        sb ++= f"\n- ${fieldfmt(cls)} $count"
      }
    }
    for (summaryStats <- dataset.summaryStats) {
      sb ++= f"""
      |Score distribution:
      |- min value            ${summaryStats.minVal}%.2f
      |- max value            ${summaryStats.maxVal}%.2f
      |- mean                 ${summaryStats.mean}%.2f
      |- stddev               ${summaryStats.stddev}%.2f
      |- median               ${summaryStats.median}%.2f
      |- 10th percentile      ${summaryStats.p10}%.2f
      |- 90th percentile      ${summaryStats.p90}%.2f""".stripMargin
    }
    for (tags <- dataset.labels) {
      sb ++= s"\nTags: ${tags.mkString(",")}"
    }
    for (comments <- dataset.comments) {
      sb ++= "\nComments"
      for (comment <- comments) {
        sb ++= "\n"
        sb ++= commentToString(comment)
      }
    }
    sb.toString
  }
}
