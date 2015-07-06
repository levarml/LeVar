package controllers.api

import play.api.mvc._
import play.api.libs.json._

object CommentController extends Controller {
  def searchByDataset(org: String, id: String) = TODO
  def addForDataset(org: String, id: String) = TODO
  def viewForDataset(org: String, id: String, cid: String) = TODO
  def removeFromDataset(org: String, id: String, cid: String) = TODO
  def searchByDatum(org: String, setId: String, id: String) = TODO
  def addForDatum(org: String, setId: String, id: String) = TODO
  def viewForDatum(org: String, setId: String, id: String, cid: String) = TODO
  def removeFromDatum(org: String, setId: String, id: String, cid: String) = TODO
  def searchByExperiment(org: String, id: String) = TODO
  def addForExperiment(org: String, id: String) = TODO
  def viewForExperiment(org: String, id: String, cid: String) = TODO
  def removeFromExperiment(org: String, id: String, cid: String) = TODO
  def searchByPrediction(org: String, expId: String, id: String) = TODO
  def addForPrediction(org: String, expId: String, id: String) = TODO
  def viewForPrediction(org: String, expId: String, id: String, cid: String) = TODO
  def removeFromPrediction(org: String, expId: String, id: String, cid: String) = TODO
}
