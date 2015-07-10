package levar

import org.joda.time.DateTime

/**
 * Data type for predictions (ie rows) in an [[Experiment]]
 *
 * @param value the prediction value itself
 * @param datum the object input to the prediction, for display
 * @param datumId the ID of the object input to the prediction, for uploading
 * @param experiment the experiment this prediction is part of, for display
 * @param score the score of the prediction made, if applicable
 * @param createdAt the date the prediction was saved in the DB
 * @param labels the set of labels applied to the prediction
 * @param comments the set of comments made of the prediction
 */
case class Prediction(
  value: Either[Double, String],
  datum: Option[Datum] = None,
  datumId: Option[String] = None,
  score: Option[Double] = None,
  createdAt: Option[DateTime] = None,
  labels: Option[Seq[String]] = None,
  comments: Option[ResultSet[Comment]] = None)
