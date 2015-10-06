package controllers.api

import play.api.mvc._
import play.api.libs.json._

object LabelController extends Controller {
  def addForDataset(org: String, id: String) = TODO
  def removeFromDataset(org: String, id: String, label: String) = TODO
  def addForDatum(org: String, datasetId: String, id: String) = TODO
  def removeFromDatum(org: String, datasetId: String, id: String, label: String) = TODO
  def addForExperiment(org: String, datasetId: String, experimentId: String) = TODO
  def removeFromExperiment(org: String, datasetId: String, experimentId: String, label: String) = TODO
  def addForPrediction(org: String, datasetId: String, experimentId: String, predictionId: String) = TODO
  def removeFromPrediction(org: String, datasetId: String, experimentId: String, predictionId: String, label: String) = TODO
}
