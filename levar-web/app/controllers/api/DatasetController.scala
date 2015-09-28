package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.BodyParsers.parse
import utils._
import utils.auth._
import utils.JsonLogging
import levar._
import levar.json._
import levar.Format
import db._

object DatasetController extends Controller with JsonLogging {
  def search(org: String) = Authenticated { implicit user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        infoU("status" -> "request", "action" -> "search_for_datasets")
        val path = routes.DatasetController.search(org).toString
        val results = db.impl.searchDatasets(org).copy(path = Some(path))
        infoU("status" -> "response", "action" -> "search_for_datasets", "result" -> s"${results.size} results")
        render {
          case AcceptsText() => Ok(Format.datasetRStoString(results) + "\n")
          case Accepts.Html() => Ok(views.html.DatasetApi.search(results))
          case Accepts.Json() => Ok(Json.toJson(results))
        }
      }
    }
  }

  def create(org: String) = Authenticated { implicit user =>
    HasOrgAccess(user, org) {
      Action(BodyParsers.parse.json) { implicit request =>
        infoU("status" -> "request", "action" -> "create")
        try {
          request.body.validate[Dataset].fold(
            errors => BadRequest(JsError.toFlatJson(errors)),
            { ds =>
              try {
                db.impl.createDataset(org, ds)
                infoU("status" -> "success", "action" -> "create", "dataset" -> s"$org/${ds.id}")
                val saved = db.impl.getDataset(org, ds.id)
                render {
                  case AcceptsText() => Ok(Format.datasetToString(saved) + "\n")
                  case Accepts.Html() => Ok(views.html.DatasetApi.details(saved))
                  case Accepts.Json() => Ok(Json.toJson(saved))
                }
              } catch {
                case _: DatasetIdAlreadyExists => {
                  val msg = s"Dataset ID already exists: $org/${ds.id}"
                  infoU("status" -> "error", "message" -> msg)
                  BadRequest(msg)
                }
              }
            }
          )
        } catch {
          case e: IllegalArgumentException => {
            if (e.getMessage == "requirement failed: invalid schema") {
              infoU("status" -> "error", "message" -> e.getMessage)
              BadRequest("invalid schema: " + request.body)
            } else if (e.getMessage == "requirement failed: invalid type") {
              infoU("status" -> "error", "message" -> e.getMessage)
              BadRequest("invalid type: " + request.body)
            } else {
              throw e
            }
          }
        }
      }
    }
  }

  def details(org: String, id: String) = Authenticated { implicit user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        infoU("status" -> "request", "action" -> "details", "dataset" -> id)
        try {
          val ds = db.impl.getDataset(org, id)
          infoU("status" -> "found", "action" -> "details", "dataset" -> id)
          render {
            case AcceptsText() => Ok(Format.datasetToString(ds) + "\n")
            case Accepts.Html() => Ok(views.html.DatasetApi.details(ds))
            case Accepts.Json() => Ok(Json.toJson(ds))
          }
        } catch {
          case _: NotFoundInDb => {
            infoU("status" -> "not_found", "action" -> "details", "dataset" -> id)
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

  def update(org: String, id: String) = Authenticated { implicit user =>
    HasOrgAccess(user, org) {
      Action(BodyParsers.parse.json) { implicit request =>
        infoU("status" -> "request", "action" -> "update", "dataset" -> id)
        request.body.validate[Dataset.Update].fold(
          errors => BadRequest(JsError.toFlatJson(errors)),
          { dsUpdate =>
            try {
              db.impl.updateDataset(org, id, dsUpdate)
              val saved = db.impl.getDataset(org, id)
              render {
                case AcceptsText() => Ok(Format.datasetToString(saved) + "\n")
                case Accepts.Html() => Ok(views.html.DatasetApi.details(saved))
                case Accepts.Json() => Ok(Json.toJson(saved))
              }
            } catch {
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

  def delete(org: String, id: String) = Authenticated { implicit user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        infoU("status" -> "request", "action" -> "delete", "org" -> org, "dataset" -> id)
        try {
          val savedBeforeDelete = db.impl.getDataset(org, id)
          db.impl.deleteDataset(org, id)
          infoU("status" -> "success", "action" -> "deelete", "org" -> org, "dataset" -> id)
          render {
            case AcceptsText() => Ok(Format.datasetToString(savedBeforeDelete) + "\n")
            case Accepts.Html() => Ok(views.html.DatasetApi.details(savedBeforeDelete))
            case Accepts.Json() => Ok(Json.toJson(savedBeforeDelete))
          }
        } catch {
          case _: NotFoundInDb => {
            val msg = s"Dataset not found: $id"
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
