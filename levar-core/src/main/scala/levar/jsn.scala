package levar

/**
 * Implicit functions for JSON conversions
 */
package object jsn {

  import play.api.libs.json._

  implicit val CommentWrites: Writes[Comment[_]] = Writes { comment: Comment[_] =>
    var obj = Json.obj("username" -> comment.username, "comment" -> comment.comment)
    comment.id.foreach { id =>
      obj ++= Json.obj("id" -> JsString(id))
    }
    obj
  }
}
