package db

class CannotCreateOrganizationException(msg: String) extends Exception(msg)
class OrganizationNotFoundException(msg: String) extends Exception(msg)
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
   * Look up auth credentials by user name and password
   *
   * @param user the user name
   * @param pass the password
   * @return authentication identifier if one exists, None if not
   */
  def getAuth(user: String, pass: String): Boolean

  /**
   * Add new auth credentials
   *
   * @param user the user name
   * @param pass the password
   */
  def addAuth(user: String, pass: String)

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
   */
  def addOrg(name: String)

  /**
   * Add a new organizaion and add users
   *
   * @param name the organization name
   * @param users a list of user names to add to the organization
   */
  def addOrg(name: String, users: Seq[String])

  /**
   * Remove an organization
   *
   * @param name the name of the organization to remove
   */
  def delOrg(name: String)

  /**
   * Add a user to an organization
   *
   * @param org the organization name
   * @param user the user name of the user to add to the organization
   */
  def addToOrg(org: String, user: String) { addToOrg(org, Seq(user)) }

  /**
   * Add a set of users to an organization
   *
   * @param org the organization name
   * @param users a list of users to add to the organization
   */
  def addToOrg(org: String, users: Seq[String])

  /**
   * List the organizations a user belongs to
   *
   * @param user the user name
   * @return a list of organization names
   */
  def listUserOrgs(user: String): Seq[String]

  /**
   * @return all organization names
   */
  def listOrgs: Seq[String]

  /**
   * @return the users in an organization
   */
  def listOrgUsers(org: String): Seq[String]

  /**
   * Rename an organization
   *
   * @param current the current name of the organization
   * @param next the name to change to
   */
  def renameOrg(current: String, next: String)
}

/**
 * Default/production implementation of [[Database]]
 */
object impl extends Database {
  import com.typesafe.config.ConfigFactory
  import java.net.URI
  import scalikejdbc._
  import play.api.Logger
  import java.util.UUID
  import math.min
  import util.validateIdentifier
  import org.postgresql.util.PSQLException
  import org.postgresql.util.PSQLState._

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

  /**
   * Generate a set of UUIDs
   *
   * Used for batch inserts when we need to extract a portion of the UUID
   * for a secondary key
   *
   * @param num the number of UUIDs to generate
   * @return a set of `n` distinct UUIDs with string semi-unique identifiers
   */
  def genUuids(num: Int): Seq[(UUID, String)] = {
    var result = Seq.empty[(UUID, String)]
    DB.readOnly { implicit session =>
      do {
        val uuids =
          sql"select uuid_generate_v1mc()::text uuid from generate_series(1, ${min(10000, num)})"
            .map(_.string("uuid"))
            .list
            .apply()
        val uniqd = uuids.map { u => (UUID.fromString(u), u.take(8)) }.groupBy(_._2).map(_._2.head)
        result ++= uniqd
      } while (result.size < num)
    }

    result.take(num)
  }

  def getAuth(user: String, pass: String) = {
    DB.readOnly { implicit session =>
      sql"""select exists(select 1 from auth
            where username = $user and password = crypt($pass, password) limit 1)"""
        .map(_.boolean(1))
        .single
        .apply()
        .get
    }
  }

  def addAuth(user: String, pass: String) {
    validateIdentifier(user)
    logger.info(s"adding user $user")
    val uuidOpt = try {
      DB.localTx { implicit session =>
        sql"""insert into auth (username, password) values ($user, crypt($pass, gen_salt('md5')))
              returning auth_id::text"""
          .map(_.string("auth_id"))
          .single
          .apply()
          .map(UUID.fromString)
      }
    } catch {
      case e: PSQLException if e.getMessage.contains("duplicate key value") =>
        throw new CannotCreateUserException(user)
    }
    uuidOpt match {
      case Some(uuid) => {
        logger.info(s"created new analysis $uuid")
      }
      case None => {
        throw new CannotCreateUserException(user)
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
      sql"""update auth
            set username = $next, updated_at = current_timestamp
            where username = $current returning username"""
        .map(_.string("username"))
        .list
        .apply()
    }
    if (updatedUsers.isEmpty) {
      throw new UserNotFoundException(current)
    } else if (updatedUsers != List(next)) {
      val msg = s"unexpected users updated matching $current: ${updatedUsers.mkString(", ")}"
      throw new UnexpectedResultException(msg)
    }
  }

  def resetPassword(user: String, currentPass: String, nextPass: String) {
    logger.info(s"resetting password for $user with credentials")
    val updatedUsers = DB.localTx { implicit setssion =>
      sql"""update auth
            set password = crypt($nextPass, gen_salt('md5')), updated_at = current_timestamp
            where username = $user and password = crypt($currentPass, password)
            returning username"""
        .map(_.string("username"))
        .list
        .apply()
    }
    if (updatedUsers.isEmpty) {
      throw new UnauthorizedException(user)
    } else if (updatedUsers != List(user)) {
      val msg = s"unexpected users updated matching $user: ${updatedUsers.mkString(", ")}"
      throw new UnexpectedResultException(msg)
    }
  }

  def addOrg(name: String) {
    validateIdentifier(name)
    logger.info(s"creating new organization $name")
    val newNames = try {
      DB.localTx { implicit session =>
        sql"insert into org (name) values ($name) returning name"
          .map(_.string("name"))
          .list
          .apply()
      }
    } catch {
      case e: PSQLException if e.getMessage.contains("duplicate key value") =>
        throw new CannotCreateOrganizationException(name)
    }
    if (newNames.isEmpty) {
      throw new CannotCreateOrganizationException(name)
    } else if (newNames != List(name)) {
      val msg = s"unexpected organizations created matching $name: ${newNames.mkString(", ")}"
      throw new UnexpectedResultException(msg)
    }
  }

  def delOrg(name: String) {
    val deletedOrgs = DB.localTx { implicit session =>
      sql"delete from org where name = $name returning name"
        .map(_.string("name"))
        .list
        .apply()
    }
    if (deletedOrgs.isEmpty) {
      throw new OrganizationNotFoundException(name)
    } else if (deletedOrgs != List(name)) {
      val msg = s"unexpected orgs deleted matching $name: ${deletedOrgs.mkString(", ")}"
      throw new UnexpectedResultException(msg)
    }
  }

  private def lookupUserIds(users: Seq[String]): Seq[String] = DB.readOnly { implicit session =>
    val usersUniq = users.distinct
    val userNameIds = sql"select username, auth_id::text from auth where username in ($usersUniq)"
      .map { r => (r.string("username"), r.string("auth_id")) }
      .list
      .apply()

    if (userNameIds.size != usersUniq.size) {
      val notFound = usersUniq.toSet - userNameIds.map(_._1).toSet
      throw new UserNotFoundException(notFound.mkString(", "))
    }

    userNameIds.map(_._2)
  }

  def addOrg(name: String, users: Seq[String]) {
    validateIdentifier(name)
    DB.localTx { implicit session =>
      val userIds = lookupUserIds(users)

      val newIdInsert = try {
        sql"insert into org (name) values ($name) returning org_id::text"
          .map(_.string("org_id"))
          .single
          .apply()
      } catch {
        case e: PSQLException if e.getMessage.contains("duplicate key value") =>
          throw new CannotCreateOrganizationException(name)
      }

      newIdInsert match {
        case Some(newId) => {
          val batchVals = userIds.map(Seq(newId, _))
          sql"insert into org_membership (org_id, auth_id) values (?::uuid, ?::uuid)"
            .batch(batchVals: _*)
            .apply()
        }
        case None =>
          throw new CannotCreateOrganizationException(name)
      }
    }
  }

  def addToOrg(name: String, users: Seq[String]) {
    validateIdentifier(name)
    DB.localTx { implicit session =>
      val userIds = lookupUserIds(users)

      val orgIdLookup = sql"select org_id::text from org where name = $name"
        .map(_.string("org_id"))
        .single
        .apply()

      orgIdLookup match {
        case Some(orgId) => {
          val batchVals = userIds.map(Seq(orgId, _))
          sql"insert into org_membership (org_id, auth_id) values (?::uuid, ?::uuid)"
            .batch(batchVals: _*)
            .apply()
        }
        case None =>
          throw new OrganizationNotFoundException(name)
      }
    }
  }

  def listUserOrgs(user: String): Seq[String] = DB.readOnly { implicit session =>
    sql"""select org.name org
            from org
            inner join org_membership on org.org_id = org_membership.org_id
            inner join auth on org_membership.auth_id = auth.auth_id
            where auth.username = $user"""
      .map(_.string("org"))
      .list
      .apply()
  }

  def listOrgs: Seq[String] = DB.readOnly { implicit session =>
    sql"select name from org"
      .map(_.string("name"))
      .list
      .apply()
  }

  def listOrgUsers(org: String): Seq[String] = DB.readOnly { implicit session =>
    sql"""select username
          from auth
          inner join org_membership on auth.auth_id = org_membership.auth_id
          inner join org on org_membership.org_id = org.org_id
          where org.name = $org"""
      .map(_.string("username"))
      .list
      .apply()
  }

  def renameOrg(current: String, next: String) {
    validateIdentifier(next)
    logger.info(s"renaming organization $current to $next")
    val updatedOrgs = DB.localTx { implicit session =>
      sql"""update org
            set name = $next, updated_at = current_timestamp
            where name = $current
            returning name"""
        .map(_.string("name"))
        .list
        .apply()
    }

    if (updatedOrgs.isEmpty) {
      throw new OrganizationNotFoundException(current)
    } else if (updatedOrgs != List(next)) {
      val msg = s"unexpected orgs updated matching $current: ${updatedOrgs.mkString(", ")}"
      throw new UnexpectedResultException(msg)
    } else {
      logger.info(s"renamed org $current to $next")
    }
  }
}
