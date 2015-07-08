package controllers.api

import play.api.mvc._
import play.api.libs.json._

object LabelController extends Controller {
  def addForDataset(org: String, id: String) = TODO
  def removeFromDataset(org: String, id: String, label: String) = TODO
  def addForDatum(org: String, datasetId: String, id: String) = TODO
  def removeFromDatum(org: String, datasetId: String, id: String, label: String) = TODO
  def addForExperiment(org: String, id: String) = TODO
  def removeFromExperiment(org: String, experimentId: String, label: String) = TODO
  def addForPrediction(org: String, experimentId: String, id: String) = TODO
  def removeFromPrediction(org: String, experimentId: String, id: String, label: String) = TODO
}
