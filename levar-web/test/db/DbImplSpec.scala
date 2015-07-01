package db

import org.scalatest._

class DbImplSpec extends FlatSpec with BeforeAndAfterEach {

  override def beforeEach() {
    impl.setUp
  }

  override def afterEach() {
    impl.tearDown
  }

  "The DB" should "setUp and tearDown without breaking" in {
    info("seemed to work OK")
  }

  it should "run setUp twice without freaking out" in {
    impl.setUp
    info("seemed to work OK")
  }

  "impl.getAuth" should "not allow any old user to authenticate" in {
    assert(impl.getAuth("user-name", "my-password") == None)
  }

  "impl.addAuth" should "create a new user" in {
    info("adding user-name")
    impl.addAuth("user-name", "my-password")

    info("checking user-name")
    impl.getAuth("user-name", "my-password") match {
      case Some(_) => info("user-name authorized")
      case None => fail("did not create user")
    }
  }

  it should "not allow any user to connect after creating a user" in {
    info("adding user-name")
    impl.addAuth("user-name", "my-password")

    info("checking user-name with incorrct password")
    impl.getAuth("user-name", "my-password2") match {
      case Some(_) => fail("allowed invalid access")
      case None => info("user-name unauthorized")
    }
  }

  "impl.delAuth" should "throw an exception if there's no user" in {
    var caught = false
    try {
      info("trying to delete non-existent user")
      impl.delAuth("user-name")
    } catch {
      case e: UserNotFoundException => {
        info("caught UserNotFoundException")
        caught = true
      }
    }
    if (!caught)
      fail("delAuth did not throw exception")
  }

  it should "remove a user" in {
    impl.addAuth("user-name", "my-password")

    info("looking up user-name")
    impl.getAuth("user-name", "my-password") match {
      case Some(_) => info("user-name created")
      case None => fail("did not create user")
    }

    info("deleting user-name")
    impl.delAuth("user-name")

    info("checking user-name is gone")
    impl.getAuth("user-name", "my-password") match {
      case Some(_) => fail("did not delete user")
      case None => info("user-name unauthorized")
    }
  }

  it should "not remove *every* user" in {
    info("creating user-name-1")
    impl.addAuth("user-name-1", "my-password")

    info("creating user-name-2")
    impl.addAuth("user-name-2", "my-password")

    info("checking user-name-1 was created")
    impl.getAuth("user-name-1", "my-password") match {
      case Some(_) => info("user-name-1 authorized")
      case None => fail("did not create user")
    }

    info("checking user-name-2 was created")
    impl.getAuth("user-name-2", "my-password") match {
      case Some(_) => info("user-name-2 authorized")
      case None => fail("did not create user")
    }

    info("removing user-name-1")
    impl.delAuth("user-name-1")

    impl.getAuth("user-name-1", "my-password") match {
      case Some(_) => fail("did not delete user")
      case None => info("user-name-1 removed")
    }

    impl.getAuth("user-name-2", "my-password") match {
      case Some(_) => info("user-name-2 is still there")
      case None => fail("did not create user")
    }
  }

  "impl.renameAuth" should "rename a user" in {
    info("adding user-name")
    impl.addAuth("user-name", "my-password")

    impl.getAuth("user-name", "my-password") match {
      case Some(_) => info("user-name was created")
      case None => fail("did not create user")
    }

    info("renaming user-name to john-lennon")
    impl.renameAuth("user-name", "john-lennon")
    impl.getAuth("john-lennon", "my-password") match {
      case Some(_) => info("user-name renamed to john-lennon")
      case None => fail("did not rename user")
    }
  }

  it should "not rename *every* user" in {
    info("adding user-name-1")
    impl.addAuth("user-name-1", "my-password")

    impl.getAuth("user-name-1", "my-password") match {
      case Some(_) => info("user-name-1 was created")
      case None => fail("did not create user")
    }

    info("adding user-name-2")
    impl.addAuth("user-name-2", "my-password-2")

    impl.getAuth("user-name-2", "my-password-2") match {
      case Some(_) => info("user-name-2 was created")
      case None => fail("did not create user")
    }

    info("renaming user-name-1 to john-lennon")
    impl.renameAuth("user-name-1", "john-lennon")

    impl.getAuth("john-lennon", "my-password") match {
      case Some(_) => info("user-name-1 renamed to john-lennon")
      case None => fail("did not rename user")
    }

    impl.getAuth("user-name-2", "my-password-2") match {
      case Some(_) => info("user-name-2 is still there")
      case None => fail("user got deleted somehow")
    }

    impl.getAuth("user-name-2", "my-password") match {
      case Some(_) => fail("user-name-2 can be accessed incorrectly")
      case None => info("user-name-2's password doesn't somehow work for john-lennon")
    }
  }
}
