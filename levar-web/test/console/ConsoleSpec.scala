package console

import org.scalatest._

class ConsoleSpec extends FlatSpec with BeforeAndAfterEach {

  override def beforeEach() {
    db.impl.setUp
  }

  override def afterEach() {
    db.impl.tearDown
  }

  "createUser" should "create a user" in {
    info("creating user-name")
    createUser("user-name", "my-password")
    info("verifying user-name")
    assert(db.impl.getAuth("user-name", "my-password"), "did not create user")
  }

  "deleteUser" should "delete a user" in {
    info("creating user-name")
    createUser("user-name", "my-password")
    info("verifying user-name")
    assert(db.impl.getAuth("user-name", "my-password"), "did not create user")
    info("removing user-name")
    dropUser("user-name")
    info("verifying user-name was dropped")
    assert(!db.impl.getAuth("user-name", "my-password"), "did not remove user")
  }
}
