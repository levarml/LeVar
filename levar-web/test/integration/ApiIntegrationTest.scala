package integration

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.json._
import levar._
import levar.json._

class ApiIntegrationTest extends PlaySpec with BeforeAndAfterEach {

  override def beforeEach() {
    db.impl.setUp()
  }

  override def afterEach() {
    db.impl.tearDown()
  }

  "GET /api/ping" must {
    "work" in {
      running(new TestServer(3333, new FakeApplication())) {
        val response = await(WS.url("http://localhost:3333/api/ping").get())
        assert(response.status == 200)
        assert(response.body.trim == "OK")
      }
    }
  }

  "GET /api/test-org/datasets" must {
    "send back a 401 if no credentials provided" in {
      running(new TestServer(3333, new FakeApplication())) {
        val response = await(WS.url("http://localhost:3333/api/test-org/datasets").get())
        assert(response.status == 401)
      }
    }

    "send back a 404 if the org isn't a thing" in {
      console.createUser("test-user", "test-pass")
      running(new TestServer(3333, new FakeApplication())) {
        val response = await(WS.url("http://localhost:3333/api/test-org/datasets").withAuth("test-user", "test-pass", BASIC).get())
        assert(response.status == 404)
      }
    }

    "send back empty result set if there's no datasets" in {
      console.createUser("test-user", "test-pass")
      console.createOrg("test-org", Seq("test-user"))
      running(new TestServer(3333, new FakeApplication())) {
        val response = await(WS.url("http://localhost:3333/api/test-org/datasets").withHeaders("Accept" -> "application/json").withAuth("test-user", "test-pass", BASIC).get())
        assert(response.status == 200)
        response.json.asOpt[ResultSet[Dataset]] match {
          case Some(rs) => assert(rs.items.isEmpty)
          case None => fail("did not process JSON: " + response.body)
        }
      }
    }
  }
}
