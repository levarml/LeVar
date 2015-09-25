package utils

import play.api.libs.json._

trait JsonLogging {

  private def msg(things: Seq[(String, String)]) = {
    val ctx = getClass.getSimpleName.replaceAll("([a-z])([A-Z])", "$1_$2").filter(_ != '$').toLowerCase
    val items = Seq("context" -> JsString(ctx)) ++ (things.map { case (k, v) => (k, JsString(v)) })
    JsObject(items).toString
  }

  def info(k: String) { play.api.Logger.info(k) }
  def error(k: String) { play.api.Logger.error(k) }
  def warn(k: String) { play.api.Logger.warn(k) }

  def info(things: (String, String)*) { info(msg(things)) }
  def error(things: (String, String)*) { error(msg(things)) }
  def warn(things: (String, String)*) { warn(msg(things)) }

  def infoU(things: (String, String)*)(implicit user: String, org: String) { info(things ++ Seq("user" -> user, "org" -> org): _*) }
  def errorU(things: (String, String)*)(implicit user: String, org: String) { error(things ++ Seq("user" -> user, "org" -> org): _*) }
  def warnU(things: (String, String)*)(implicit user: String, org: String) { warn(things ++ Seq("user" -> user, "org" -> org): _*) }
}
