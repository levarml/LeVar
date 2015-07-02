package db

import org.scalatest._
import util._

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
    assert(!impl.getAuth("user-name", "my-password"), "unauthorized access with 0 users")
  }

  "impl.addAuth" should "create a new user" in {
    info("adding user-name")
    impl.addAuth("user-name", "my-password")

    info("checking user-name")
    assert(impl.getAuth("user-name", "my-password"), "did not create user")
  }

  it should "not allow any user to connect after creating a user" in {
    info("adding user-name")
    impl.addAuth("user-name", "my-password")

    info("checking user-name with incorrct password")
    assert(!impl.getAuth("user-name", "my-password2"), "allowed invalid access")
  }

  it should "not create duplicate users" in {
    info("adding user-name")
    impl.addAuth("user-name", "my-password")

    info("checking user-name")
    assert(impl.getAuth("user-name", "my-password"), "did not create user")

    info("adding user-name again")
    var caught = false
    try {
      impl.addAuth("user-name", "my-password-2")
    } catch {
      case _: CannotCreateUserException => {
        info("caught error trying to create duplicate user")
        caught = true
      }
    }
    if (!caught) {
      fail("did not block creation of duplicate key")
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
    assert(impl.getAuth("user-name", "my-password"), "did not create user-name")

    info("deleting user-name")
    impl.delAuth("user-name")

    info("checking user-name is gone")
    assert(!impl.getAuth("user-name", "my-password"), "did not delete user")
  }

  it should "not remove *every* user" in {
    info("creating user-name-1")
    impl.addAuth("user-name-1", "my-password")

    info("creating user-name-2")
    impl.addAuth("user-name-2", "my-password")

    info("checking user-name-1 was created")
    assert(impl.getAuth("user-name-1", "my-password"), "did not create user-name-1")

    info("checking user-name-2 was created")
    assert(impl.getAuth("user-name-2", "my-password"), "did not create user-name-2")

    info("removing user-name-1")
    impl.delAuth("user-name-1")

    assert(!impl.getAuth("user-name-1", "my-password"), "did not delete user-name-1")

    assert(impl.getAuth("user-name-2", "my-password"), "deleted user-name-2")
  }

  "impl.renameAuth" should "rename a user" in {
    info("adding user-name")
    impl.addAuth("user-name", "my-password")

    assert(impl.getAuth("user-name", "my-password"), "did not create user")

    info("renaming user-name to john-lennon")
    impl.renameAuth("user-name", "john-lennon")
    assert(impl.getAuth("john-lennon", "my-password"), "did not rename user-name-1 to john-lennon")
  }

  it should "not rename *every* user" in {
    info("adding user-name-1")
    impl.addAuth("user-name-1", "my-password")
    assert(impl.getAuth("user-name-1", "my-password"), "did not create user-name-1")
    info("adding user-name-2")
    impl.addAuth("user-name-2", "my-password-2")
    assert(impl.getAuth("user-name-2", "my-password-2"), "user-name-2 not created")

    info("renaming user-name-1 to john-lennon")
    impl.renameAuth("user-name-1", "john-lennon")

    assert(impl.getAuth("john-lennon", "my-password"), "did not rename user-name-1 to john-lennong")

    assert(impl.getAuth("user-name-2", "my-password-2"), "user-name-2 got renamed somehow")

    assert(!impl.getAuth("user-name-2", "my-password"), "user-name-2 can be accessed incorrectly")
  }

  "impl.addOrg" should "create a new organization" in {
    info("adding org1")
    db.impl.addOrg("org1")
    info("verifying org1")
    assert(impl.listOrgs == List("org1"))
  }

  it should "not unexpectedly add users" in {
    info("adding org1")
    db.impl.addOrg("org1")
    info("verifying org1")
    assert(impl.listOrgs == List("org1"))

    info("adding user-name-1")
    impl.addAuth("user-name-1", "my-password")
    assert(impl.getAuth("user-name-1", "my-password"), "did not create user-name-1")

    info("adding user-name-2")
    impl.addAuth("user-name-2", "my-password-2")
    assert(impl.getAuth("user-name-2", "my-password-2"), "user-name-2 not created")

    info("verifying org1 is still empty")
    assert(impl.listOrgUsers("org1").toSet == Set.empty)
  }

  it should "block an invalid org name" in {
    var caught = false

    try {
      info("adding 'org 1'")
      db.impl.addOrg("org 1")
    } catch {
      case _: InvalidIdentifierException => {
        info("caught invalid identifier")
        caught = true
      }
    }

    if (!caught) {
      fail("'org 1' incorrectly added to DB")
    }
  }

  it should "add users when given users" in {
    info("adding user-name-1")
    impl.addAuth("user-name-1", "my-password")
    assert(impl.getAuth("user-name-1", "my-password"), "did not create user-name-1")

    info("adding user-name-2")
    impl.addAuth("user-name-2", "my-password-2")
    assert(impl.getAuth("user-name-2", "my-password-2"), "user-name-2 not created")

    info("adding user-name-3")
    impl.addAuth("user-name-3", "my-password-2")
    assert(impl.getAuth("user-name-3", "my-password-2"), "user-name-3 not created")

    info("adding org1 with user-name-1 and user-name-2")
    db.impl.addOrg("org1", Seq("user-name-1", "user-name-2"))
    info("verifying org1")
    assert(impl.listOrgs == List("org1"))

    info("adding org2 to make sure users aren't added to the wrong org")
    db.impl.addOrg("org2")
    info("verifying org2")
    assert(impl.listOrgs.toSet == Set("org1", "org2"))

    info("checking user-name-1 and user-name-2 are in org1")
    assert(impl.listOrgUsers("org1").toSet == Set("user-name-1", "user-name-2"))

    info("checking the orgs for user-name-1")
    assert(impl.listUserOrgs("user-name-1") == Seq("org1"))
  }

  it should "not add the same org twice" in {
    info("adding org1")
    db.impl.addOrg("org1")
    info("verifying org1")
    assert(impl.listOrgs == List("org1"))

    var caught = false
    try {
      info("adding org1 again")
      db.impl.addOrg("org1")
    } catch {
      case _: CannotCreateOrganizationException => caught = true
    }

    if (!caught) {
      fail("org1 was added twice to DB")
    }
  }

  it should "not add the same org twice, when adding with users" in {
    info("adding user-name-1")
    impl.addAuth("user-name-1", "my-password")
    assert(impl.getAuth("user-name-1", "my-password"), "did not create user-name-1")

    info("adding user-name-2")
    impl.addAuth("user-name-2", "my-password-2")
    assert(impl.getAuth("user-name-2", "my-password-2"), "user-name-2 not created")

    info("adding org1")
    db.impl.addOrg("org1")
    info("verifying org1")
    assert(impl.listOrgs == List("org1"))

    var caught = false
    try {
      info("adding org1 again")
      db.impl.addOrg("org1", Seq("user-name-1", "user-name-2"))
    } catch {
      case _: CannotCreateOrganizationException => caught = true
    }

    if (!caught) {
      fail("org1 was added twice to DB")
    }
  }

  it should "not allow you to add invalid users" in {
    var caught = false
    try {
      info("adding org1 with non-existent users")
      db.impl.addOrg("org1", Seq("user-name-1"))
    } catch {
      case _: UserNotFoundException => {
        info("blocked non-existent user")
        caught = true
      }
    }
    if (!caught) {
      fail("allowed org creation with non-existent users")
    }

    assert(impl.listOrgs == List.empty)
  }

  "impl.addToOrg" should "add users to an org" in {
    info("adding org1")
    db.impl.addOrg("org1")
    info("verifying org1")
    assert(impl.listOrgs == List("org1"))

    info("adding user-name-1")
    impl.addAuth("user-name-1", "my-password")
    assert(impl.getAuth("user-name-1", "my-password"), "did not create user-name-1")

    info("adding user-name-2")
    impl.addAuth("user-name-2", "my-password-2")
    assert(impl.getAuth("user-name-2", "my-password-2"), "user-name-2 not created")

    info("adding user-name-3")
    impl.addAuth("user-name-3", "my-password-2")
    assert(impl.getAuth("user-name-3", "my-password-2"), "user-name-3 not created")

    info("verifying org1 is still empty")
    assert(impl.listOrgUsers("org1").toSet == Set.empty)

    info("adding users to org1")
    impl.addToOrg("org1", Seq("user-name-1", "user-name-2"))
    assert(impl.listOrgUsers("org1").toSet == Set("user-name-1", "user-name-2"))
  }

  it should "not add non-existent users to an org" in {
    info("adding org1")
    db.impl.addOrg("org1")
    info("verifying org1")
    assert(impl.listOrgs == List("org1"))

    info("adding users to org1")
    var caught = false
    try {
      impl.addToOrg("org1", Seq("user-name-1", "user-name-2"))
    } catch {
      case e: UserNotFoundException => {
        info("blocked adding non-existent users")
        caught = true
      }
    }

    if (!caught) {
      fail("did not block adding non-existent users")
    }
  }
}
