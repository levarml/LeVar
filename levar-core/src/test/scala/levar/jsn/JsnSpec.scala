package levar.jsn

import org.scalatest._
import play.api.libs.json._
import levar._

class JsnSpec extends FlatSpec {

  def assertCreate[A](expected: String, input: A)(implicit wrtr: Writes[A]) {
    assert(Json.parse(expected) == Json.toJson(input))
  }

  "Comment JSON" should "create basic JSON" in {
    assertCreate(
      """{"username":"john", "comment": "Hello world"}""",
      Comment("john", "Hello world"))
  }

  it should "handle IDs" in {
    assertCreate(
      """{"id": "hello-world", "username": "john", "comment": "Hello world"}""",
      Comment("john", "Hello world", id = Some("hello-world")))
  }

  it should "invalidate "
}
