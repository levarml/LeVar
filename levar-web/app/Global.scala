import play.api._

import utils.JsonLogging

object Global extends GlobalSettings with JsonLogging {

  override def onStart(app: Application) {
    info("status" -> "starting", "action" -> "loading_db")
    db.impl.setUp()
    info("status" -> "done", "action" -> "loading_db")
  }

  override def onStop(app: Application) {
    info("status" -> "shutting_down")
  }
}
