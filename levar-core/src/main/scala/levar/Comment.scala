package levar

import org.joda.time.DateTime
import play.api.libs.json.JsValue

/**
 * Arbitrary comment datatype
 *
 * @tparam A the type of thing the comment is about
 * @param username the identifier of the user who made the comment
 * @param comment the content of the comment
 * @param id unique identifier for display and lookup, created by DB
 * @param path the URL path to view the comment
 * @param subject the thing being commented on, for display
 * @param createdAt the date the comment was put in the DB
 */
case class Comment(
  username: String,
  comment: String,
  id: Option[String] = None,
  path: Option[String] = None,
  subject: Option[(String, JsValue)] = None,
  createdAt: Option[DateTime] = None)
