package db

import java.util.UUID

class CannotCreateOrganizationException(msg: String) extends Exception(msg)
class InvalidUserNameException(msg: String) extends Exception(msg)
class CannotCreateUserException(msg: String) extends Exception(msg)
class ForbiddenException(msg: String) extends Exception(msg)
class UnauthorizedException(msg: String) extends Exception(msg)
class UserNotFoundException(msg: String) extends Exception(msg)
class UnexpectedResultException(msg: String) extends Exception(msg)

/**
 * Database access layer
 */
trait Database {

  /**
   * Generate a set of UUIDs
   *
   * Used for batch inserts when we need to extract a portion of the UUID
   * for a secondary key
   *
   * @param num the number of UUIDs to generate
   * @return a set of `n` distinct UUIDs
   */
  def genUuids(num: Int): Seq[UUID]

  /**
   * Generate a new UUID
   *
   * @return a new UUID
   */
  def genUuid: UUID = genUuids(1).head

  /**
   * Look up auth credentials by user name and password
   *
   * @param user the user name
   * @param pass the password
   * @return authentication identifier if one exists, None if not
   */
  def getAuth(user: String, pass: String): Option[UUID]

  /**
   * Add new auth credentials
   *
   * @param user the user name
   * @param pass the password
   * @return the new authentication identifier
   */
  def addAuth(user: String, pass: String): UUID

  /**
   * Delete auth credentials
   *
   * @param user the user name
   */
  def delAuth(user: String)

  /**
   * Rename a user without credentials
   *
   * @param current the current user name
   * @param next the user name to change
   */
  def renameAuth(current: String, next: String)

  /**
   * Reset user password with credentials
   *
   * @param user the user name
   * @param currentPass the current password
   * @param nextPass the password to change to
   */
  def resetPassword(user: String, currentPass: String, nextPass: String)

  /**
   * Add a new organization
   *
   * @param name the organization name
   * @return the new organization UUID
   */
  def addOrg(name: String): UUID
}

/**
 * Default/production implementation of [[Database]]
 */
object impl extends Database {
  import com.typesafe.config.ConfigFactory
  import java.net.URI
  import scalikejdbc._
  import play.api.Logger

  val logger: Logger = Logger("dao.impl")

  private def sqlFromClasspath(path: String) = {
    SQL(io.Source.fromInputStream(getClass.getResourceAsStream(path)).mkString)
  }

  lazy val conf = ConfigFactory.load()

  {
    val dbUri = new URI(conf.getString("db.url"))
    val Array(username, password) = try {
      dbUri.getUserInfo.split(":")
    } catch {
      case _: NullPointerException => Array[String](null, null)
      case e: Throwable => throw e
    }
    val dbUrl = if (conf.getBoolean("db.remote")) {
      s"jdbc:postgresql://${dbUri.getHost}:${dbUri.getPort}${dbUri.getPath}?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
    } else {
      s"jdbc:postgresql://${dbUri.getHost}:${dbUri.getPort}${dbUri.getPath}"
    }
    ConnectionPool.singleton(dbUrl, username, password)
  }

  /** Set up the database */
  def setUp() {
    logger.info("setting up DB")
    DB.localTx { implicit session =>
      sqlFromClasspath("/setup.sql").execute.apply()
    }
    logger.info("setting up DB done")
  }

  /** Tear down the database */
  def tearDown() {
    logger.info("tearing down DB")
    DB.localTx { implicit session =>
      sqlFromClasspath("/teardown.sql").execute.apply()
    }
    logger.info("tearing down DB done")
  }

  def getAuth(user: String, pass: String): Option[UUID] = {
    val uuidOpt = DB.readOnly { implicit session =>
      sql"""select auth_id::text from auth
            where username = $user and password = crypt($pass, password)"""
        .map(_.string("auth_id"))
        .single
        .apply()
        .map(UUID.fromString)
    }
    uuidOpt match {
      case Some(uuid) => logger.debug(s"found $user")
      case None => logger.info(s"failed auth request for $user")
    }
    uuidOpt
  }

  def genUuids(n: Int) = (for (_ <- 1 to n) yield UUID.randomUUID).toSeq

  def addAuth(user: String, pass: String) = {
    logger.info(s"adding user $user")
    DB.localTx { implicit session =>
      val uuidOpt = DB.localTx { implicit session =>
        sql"""insert into auth (username, password) values ($user, crypt($pass, gen_salt('md5')))
              returning auth_id::text"""
          .map(_.string("auth_id"))
          .single
          .apply()
          .map(UUID.fromString)
      }
      uuidOpt match {
        case Some(uuid) => {
          logger.info(s"created new analysis $uuid")
          uuid
        }
        case None => {
          throw new CannotCreateUserException(user)
        }
      }
    }
  }

  def delAuth(user: String) {
    logger.info(s"deleting user $user")
    val deletedUsers = DB.localTx { implicit session =>
      sql"delete from auth where username = $user returning username"
        .map(_.string("username"))
        .list
        .apply()
    }
    if (deletedUsers.isEmpty) {
      throw new UserNotFoundException(s"$user not found")
    } else if (deletedUsers != List(user)) {
      throw new UnexpectedResultException(s"unexpected users deleted matching $user: ${deletedUsers.mkString(", ")}")
    }
  }

  def renameAuth(current: String, next: String) {
    logger.info(s"renaming user $current to $next")
    val updatedUsers = DB.localTx { implicit session =>
      sql"update auth set username = $next where username = $current returning username"
        .map(_.string("username"))
        .list
        .apply()
    }
    if (updatedUsers.isEmpty) {
      throw new UserNotFoundException(current)
    } else if (updatedUsers != List(next)) {
      throw new UnexpectedResultException(s"unexpected users updated matching $current: ${updatedUsers.mkString(", ")}")
    }
  }

  def resetPassword(user: String, currentPass: String, nextPass: String) {
    logger.info(s"resetting password for $user with credentials")
    val updatedUsers = DB.localTx { implicit setssion =>
      sql"""update auth set password = crypt($nextPass, gen_salt('md5'))
            where username = $user and password = crypt($currentPass, password)
            returning username"""
        .map(_.string("username"))
        .list
        .apply()
    }
    if (updatedUsers.isEmpty) {
      throw new UnauthorizedException(user)
    } else if (updatedUsers != List(user)) {
      throw new UnexpectedResultException(s"unexpected users updated matching $user: ${updatedUsers.mkString(", ")}")
    }
  }

  def addOrg(name: String) = UUID.randomUUID
}
