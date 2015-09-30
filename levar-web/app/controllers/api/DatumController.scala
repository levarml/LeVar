package controllers.api

import play.api.mvc._
import play.api.libs.json._
import utils._
import utils.auth._
import utils.JsonLogging
import levar._
import levar.json._
import levar.Format
import db._

object DatumController extends Controller with JsonLogging {

  private val dbase = db.impl

  def details(org: String, datasetId: String, dataid: String) = TODO

  def search(org: String, datasetId: String, after: Option[String] = None, gold: Option[Int] = None) = Authenticated { implicit user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        try {
          val results = dbase.searchData(org, datasetId, after, withVal = gold == Some(1))
          ResultSet(Seq.empty[Datum])
          render {
            case AcceptsText() => Ok(Format.datumRStoString(results) + "\n")
            case Accepts.Html() => Ok(views.html.DatumApi.search(results))
            case Accepts.Json() => Ok(Json.toJson(results))
          }
        } catch {
          case _: NotFoundInDb => {
            val msg = s"Dataset not found: $org/$datasetId"
            render {
              case AcceptsText() => NotFound(msg)
              case Accepts.Json() => NotFound(Json.obj("message" -> msg))
            }
          }
        }
      }
    }
  }
}
