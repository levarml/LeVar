package controllers.api

import play.api.mvc._
import play.api.libs.json._

object PingController extends Controller {

  val ping = Action { implicit request =>
    render {
      case Accepts.Html() => Ok(<html><body>OK</body></html>)
      case Accepts.Xml() => Ok(<msg>OK</msg>)
      case Accepts.Json() => Ok(Json.toJson(Map("msg" -> "OK")))
    }
  }
}
