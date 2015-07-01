package console

import org.scalatest._
import java.util.UUID

class ConsoleSpec extends FlatSpec with BeforeAndAfterEach {

  override def beforeEach() {
    db.impl.setUp
  }

  override def afterEach() {
    db.impl.tearDown
  }

  "createUser" should "create a user" in {
    createUser("user-name", "my-password")
    db.impl.getAuth("user-name", "my-password") match {
      case Some(_) =>
      case None => fail("did not create user")
    }
  }

  "deleteUser" should "delete a user"
}
