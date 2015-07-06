package levar

import play.api.libs.json._
import org.joda.time.DateTime

/**
 * Data type of items (or rows) in a [[Dataset]]
 *
 * @param data the data blob in the item -- the stuff to predict on
 * @param value the value -- the truth to predict, if available
 * @param id identifier for the item -- can be provided by the user or generated, unique WRT the dataset
 * @param dataset the associated [[Dataset]], for display
 * @param createdAt the date the item was put in the DB
 * @param labels labels applied to the data item
 * @param comments comments made on the data item
 */
case class Datum(
  data: JsValue,
  value: Option[Either[Double, String]],
  id: Option[String],
  dataset: Option[Dataset],
  createdAt: Option[DateTime],
  labels: Option[ResultSet[String]],
  comments: Option[ResultSet[Comment[Datum]]])
