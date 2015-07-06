package controllers.api

import play.api.mvc._
import play.api.libs.json._
import utils.AcceptsText

object PingController extends Controller {

  val ping = Action { implicit request =>
    render {
      case AcceptsText() => Ok("OK\n")
      case Accepts.Html() => Ok(<html><body>OK</body></html>).as("text/html")
      case Accepts.Xml() => Ok(<msg>OK</msg>)
      case Accepts.Json() => Ok(Json.toJson(Map("msg" -> "OK")))
    }
  }
}
