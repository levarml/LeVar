package levar.client

import levar._
import levar.json._
import play.api.Play.current
import play.api.libs.ws._
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import play.api.libs.json._
import scala.concurrent.Future
import scala.concurrent.duration.Duration.Inf
import org.joda.time.DateTime

class LevarClient(val config: ClientConfig) {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  // import scala.concurrent.ExecutionContext.Implicits.global

  private val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  private val client = new play.api.libs.ws.ning.NingWSClient(builder.build())

  private def post[A](path: String, body: A)(implicit f: Writes[A]): Future[WSResponse] = {
    client.url(config.url + path)
      .withAuth(config.username, config.password, WSAuthScheme.BASIC)
      .post(Json.toJson(body))
  }

  private def get[A](path: String, query: (String, String)*)(implicit f: Reads[A]): Future[A] = {
    client.url(config.url + path)
      .withAuth(config.username, config.password, WSAuthScheme.BASIC)
      .withQueryString(query: _*)
      .withHeaders("Accept" -> "application/json")
      .get()
      .map(_.json.as[A])
  }

  def searchDatasets(org: String): Future[ResultSet[Dataset]] =
    get[ResultSet[Dataset]](s"/api/$org/datasets")
}
