package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.BodyParsers.parse
import utils._
import utils.auth._
import levar._
import levar.json._
import db._

object DatasetController extends Controller {
  def search(org: String) = Authenticated { user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        val path = routes.DatasetController.search(org).toString
        val results = db.impl.searchDatasets(org).copy(path = Some(path))
        render {
          case AcceptsText() => Ok(results.toString)
          case Accepts.Html() => Ok(views.html.DatasetApi.search(results))
          case Accepts.Json() => Ok(Json.toJson(results))
        }
      }
    }
  }

  def create(org: String) = Authenticated { user =>
    HasOrgAccess(user, org) {
      Action(BodyParsers.parse.json) { implicit request =>
        try {
          request.body.validate[Dataset].fold(
            errors => BadRequest(JsError.toFlatJson(errors)),
            { ds =>
              try {
                val saved = db.impl.createDataset(org, ds)
                render {
                  case AcceptsText() => Ok(saved.toString)
                  case Accepts.Json() => Ok(Json.toJson(saved))
                }
              } catch {
                case _: DatasetIdAlreadyExists => {
                  val msg = s"Dataset ID already exists: ${ds.id}"
                  render {
                    case AcceptsText() => BadRequest(msg)
                    case Accepts.Json() => BadRequest(Json.toJson(Map("message" -> msg)))
                  }
                }
              }
            }
          )
        } catch {
          case e: IllegalArgumentException => {
            if (e.getMessage == "requirement failed: invalid schema") {
              BadRequest("invalid schema: " + request.body)
            } else if (e.getMessage == "requirement failed: invalid type") {
              BadRequest("invalid type: " + request.body)
            } else {
              throw e
            }
          }
        }
      }
    }
  }

  def details(org: String, id: String) = Authenticated { user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        try {
          val ds = db.impl.getDataset(org, id)
          render {
            case AcceptsText() => Ok(ds.toString)
            case Accepts.Html() => Ok(views.html.DatasetApi.details(ds))
            case Accepts.Json() => Ok(Json.toJson(ds))
          }
        } catch {
          case _: NotFoundInDb => {
            val msg = s"Dataset not found: $id"
            render {
              case AcceptsText() => NotFound(msg)
              case Accepts.Html() => NotFound(views.html.DatasetApi.notfound(id))
              case Accepts.Json() => NotFound(Json.obj("message" -> msg))
            }
          }
        }
      }
    }
  }

  def update(org: String, id: String) = Authenticated { user =>
    HasOrgAccess(user, org) {
      Action(BodyParsers.parse.json) { implicit request =>
        request.body.validate[Dataset.Update].fold(
          errors => BadRequest(JsError.toFlatJson(errors)),
          { dsUpdate =>
            try {
              val updated = db.impl.updateDataset(org, id, dsUpdate)
              render {
                case AcceptsText() => Ok(updated.toString)
                case Accepts.Json() => Ok(Json.toJson(updated))
              }
            } catch {
              case _: DatasetIdAlreadyExists => {
                val msg = s"Dataset ID already exists: ${dsUpdate.id.getOrElse("null")}"
                render {
                  case AcceptsText() => BadRequest(msg)
                  case Accepts.Json() => BadRequest(Json.toJson(Map("message" -> msg)))
                }
              }
              case _: NotFoundInDb => {
                val msg = s"Dataset not found: $id"
                render {
                  case AcceptsText() => NotFound(msg)
                  case Accepts.Json() => NotFound(Json.obj("message" -> msg))
                }
              }
            }
          }
        )
      }
    }
  }

  def delete(org: String, id: String) = TODO
}
