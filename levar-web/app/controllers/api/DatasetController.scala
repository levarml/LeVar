package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.BodyParsers.parse
import utils._
import utils.auth._

object DatasetController extends Controller {
  def search(org: String) = Authenticated { user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        render {
          case AcceptsText() => Ok("")
          case Accepts.Html() => Ok(<html><body><p>Datasets:</p></body></html>).as("text/html")
          case Accepts.Json() => Ok(Json.toJson(Map("datasets" -> List.empty[String])))
        }
      }
    }
  }

  def create(org: String) = TODO
  def details(org: String, id: String) = TODO
  def update(org: String, id: String) = TODO
  def delete(org: String, id: String) = TODO
}
