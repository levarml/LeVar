package controllers.api

import play.api.mvc._
import play.api.libs.json._

object CommentController extends Controller {
  def searchByDataset(org: String, id: String) = TODO
  def addForDataset(org: String, id: String) = TODO
  def viewForDataset(org: String, id: String, commentId: String) = TODO
  def removeFromDataset(org: String, id: String, commentId: String) = TODO
  def searchByDatum(org: String, datasetId: String, id: String) = TODO
  def addForDatum(org: String, datasetId: String, id: String) = TODO
  def viewForDatum(org: String, datasetId: String, id: String, commentId: String) = TODO
  def removeFromDatum(org: String, datasetId: String, id: String, commentId: String) = TODO
  def searchByExperiment(org: String, datasetId: String, experimentId: String) = TODO
  def addForExperiment(org: String, datasetId: String, experimentId: String) = TODO
  def viewForExperiment(org: String, datasetId: String, experimentId: String, commentId: String) = TODO
  def removeFromExperiment(org: String, datasetId: String, experimentId: String, commentId: String) = TODO
  def searchByPrediction(org: String, datasetId: String, experimentId: String, id: String) = TODO
  def addForPrediction(org: String, datasetId: String, experimentId: String, id: String) = TODO
  def viewForPrediction(org: String, datasetId: String, experimentId: String, id: String, commentId: String) = TODO
  def removeFromPrediction(org: String, datasetId: String, experimentId: String, id: String, commentId: String) = TODO
}
