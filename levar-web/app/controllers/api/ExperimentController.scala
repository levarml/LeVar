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

object ExperimentController extends Controller with JsonLogging {

  private val dbase = db.impl

  def searchByOrg(org: String) = Authenticated { user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        val results = dbase.searchExperiments(org)
        render {
          case AcceptsText() => Ok(Format.experimentRStoString(results) + "\n")
          case Accepts.Html() => Ok(views.html.ExperimentApi.search(results))
          case Accepts.Json() => Ok(Json.toJson(results))
        }
      }
    }
  }

  def searchByDataset(org: String, datasetId: String) = Authenticated { user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        try {
          val results = dbase.searchExperiments(org, datasetId)
          render {
            case AcceptsText() => Ok(Format.experimentRStoString(results) + "\n")
            case Accepts.Html() => Ok(views.html.ExperimentApi.search(results))
            case Accepts.Json() => Ok(Json.toJson(results))
          }
        } catch {
          case _: NotFoundInDb => {
            val msg = s"Dataset not found: $org/$datasetId"
            render {
              case AcceptsText() => NotFound(msg + "\n")
              case Accepts.Json() => NotFound(Json.obj("message" -> msg))
            }
          }
        }
      }
    }
  }

  def create(org: String, datasetId: String) = Authenticated { implicit user =>
    HasOrgAccess(user, org) {
      Action(parse.json) { implicit request =>
        request.body.validate[Experiment].fold(
          errors => BadRequest(JsError.toFlatJson(errors)),
          { exp =>
            try {
              dbase.createExperiment(org, datasetId, exp)
              infoU("status" -> "success", "action" -> "create", "experiment" -> s"$org/$datasetId/${exp.id}")
              val saved = db.impl.getExperiment(org, datasetId, exp.id)
              render {
                case AcceptsText() => Ok(Format.experimentToString(saved) + "\n")
                case Accepts.Html() => Ok(views.html.ExperimentApi.details(saved))
                case Accepts.Json() => Ok(Json.toJson(saved))
              }
            } catch {
              case _: NotFoundInDb => {
                val msg = s"Dataset $org/$datasetId not found"
                infoU("status" -> "error", "message" -> msg)
                NotFound(msg + "\n")
              }
              case _: ExperimentIdAlreadyExists => {
                val msg = s"Experiment ID already exists: $org/$datasetId/${exp.id}"
                infoU("status" -> "error", "message" -> msg)
                BadRequest(msg + "\n")
              }
            }
          }
        )
      }
    }
  }

  def details(org: String, datasetId: String, id: String) = Authenticated { implicit user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        try {
          val saved = dbase.getExperiment(org, datasetId, id)
          render {
            case AcceptsText() => Ok(Format.experimentToString(saved) + "\n")
            case Accepts.Html() => Ok(views.html.ExperimentApi.details(saved))
            case Accepts.Json() => Ok(Json.toJson(saved))
          }
        } catch {
          case _: NotFoundInDb => {
            val msg = s"Experiment $org/$datasetId/$id not found"
            infoU("status" -> "error", "message" -> msg)
            NotFound(msg + "\n")
          }
        }
      }
    }
  }

  def update(org: String, datasetId: String, id: String) = Authenticated { implicit user =>
    HasOrgAccess(user, org) {
      Action(parse.json) { implicit request =>
        infoU("status" -> "request", "action" -> "update", "dataset" -> datasetId, "experiment" -> id)
        request.body.validate[Experiment.Update].fold(
          errors => BadRequest(JsError.toFlatJson(errors)),
          { expUpdate =>
            try {
              dbase.updateExperiment(org, datasetId, id, expUpdate)
              val saved = db.impl.getExperiment(org, datasetId, id)
              render {
                case AcceptsText() => Ok(Format.experimentToString(saved) + "\n")
                case Accepts.Html() => Ok(views.html.ExperimentApi.details(saved))
                case Accepts.Json() => Ok(Json.toJson(saved))
              }
            } catch {
              case _: NotFoundInDb => {
                val msg = s"Experiment not found: $org/$datasetId/$id"
                render {
                  case AcceptsText() => NotFound(msg + "\n")
                  case Accepts.Json() => NotFound(Json.obj("message" -> msg))
                }
              }
            }
          }
        )
      }
    }
  }

  def delete(org: String, datasetId: String, experimentId: String) = Authenticated { implicit user =>
    HasOrgAccess(user, org) {
      Action { implicit request =>
        infoU("status" -> "request", "action" -> "delete", "org" -> org, "dataset" -> datasetId, "experiment" -> experimentId)
        try {
          val savedBeforeDelete = dbase.getExperiment(org, datasetId, experimentId)
          dbase.deleteExperiment(org, datasetId, experimentId)
          infoU("status" -> "success", "action" -> "delete", "org" -> org, "dataset" -> datasetId, "experiment" -> experimentId)
          render {
            case AcceptsText() => Ok(Format.experimentToString(savedBeforeDelete) + "\n")
            case Accepts.Html() => Ok(views.html.ExperimentApi.details(savedBeforeDelete))
            case Accepts.Json() => Ok(Json.toJson(savedBeforeDelete))
          }
        } catch {
          case _: NotFoundInDb => {
            val msg = s"Experiment not found: $org/$datasetId/$experimentId"
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
