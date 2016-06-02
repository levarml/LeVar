package levar.io

import levar.client._
import com.typesafe.config.ConfigFactory
import java.io._
import scala.io.Source
import play.api.libs.json._
import play.api.libs.functional.syntax._

object ClientConfigIo {

  lazy val config = ConfigFactory.load

  implicit val ClientConfigFormats: Format[ClientConfig] = (
      (__ \ "username").format[String] and
      (__ \ "password").format[String] and
      (__ \ "url").format[String] and
      (__ \ "org").format[String]
  )(ClientConfig.apply, unlift(ClientConfig.unapply))

  def loadClient: LevarClient = loadClientConfig match {
    case Some(config) => new LevarClient(config)
    case None => throw new MissingClientConfig
  }

  def loadClientConfig: Option[ClientConfig] = {
    val settingsDir = config.getString("levar.settings_dir")
    val f = new File(settingsDir, "config.json")
    if (f.exists) {
      Json.parse(Source.fromFile(f).getLines.mkString).asOpt[ClientConfig]
    } else {
      None
    }
  }

  def saveClientConfig(cfg: ClientConfig) {

    val settingsDir = new File(config.getString("levar.settings_dir"))
    if (!settingsDir.exists()) {
      if (!settingsDir.mkdirs()) {
        throw new IOException(s"cannot create directory $settingsDir")
      }
    } else if (!settingsDir.isDirectory) {
      throw new IOException(s"$settingsDir must be a directory")
    }

    val f = new File(settingsDir, "config.json")
    if (f.exists) {
      if (!f.delete()) {
        throw new IOException(s"could not overwrite $f")
      }
    }

    val pw = new PrintWriter(f, "UTF-8")
    try {
      pw.println(Json.stringify(Json.toJson(cfg)))
    } finally {
      pw.close()
    }

    f.setReadable(false, false)
    f.setReadable(true, true)
    f.setWritable(false)
    f.setExecutable(false)
  }
}
