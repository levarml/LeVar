package db

import levar._

class CannotCreateOrganizationException(msg: String) extends Exception(msg)
class OrganizationNotFoundException(msg: String) extends Exception(msg)
class CannotCreateUserException(msg: String) extends Exception(msg)
class ForbiddenException(msg: String) extends Exception(msg)
class UnauthorizedException(msg: String) extends Exception(msg)
class UserNotFoundException(msg: String) extends Exception(msg)
class UnexpectedResultException(msg: String) extends Exception(msg)

class DatasetIdAlreadyExists(msg: String) extends Exception(msg)

class NotFoundInDb extends Exception

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
   * @param id the organization external id
   */
  def addOrg(id: String)

  /**
   * Add a new organizaion and add users
   *
   * @param id the organization external id
   * @param users a list of user names to add to the organization
   */
  def addOrg(id: String, users: Seq[String])

  /**
   * Remove an organization
   *
   * @param id the external id of the organization to remove
   */
  def delOrg(id: String)

  /**
   * Add a user to an organization
   *
   * @param org the organization external id
   * @param user the user name of the user to add to the organization
   */
  def addToOrg(org: String, user: String) { addToOrg(org, Seq(user)) }

  /**
   * Add a set of users to an organization
   *
   * @param org the organization external id
   * @param users a list of users to add to the organization
   */
  def addToOrg(org: String, users: Seq[String])

  /**
   * List the organizations a user belongs to
   *
   * @param user the user name
   * @return a list of organization external ids
   */
  def listUserOrgs(user: String): Seq[String]

  /**
   * @return all organization external ids
   */
  def listOrgs: Seq[String]

  /**
   * @return the users in an organization
   */
  def listOrgUsers(org: String): Seq[String]

  /**
   * Rename an organization
   *
   * @param current the current external id of the organization
   * @param next the id to change to
   */
  def renameOrg(current: String, next: String)

  /**
   * Return whether a user has access to an organization
   *
   * @param user the user name
   * @param org the organization external id
   * @return true if the user has access to the organization
   */
  def userHasOrgAccess(user: String, org: String): Boolean

  /**
   * Search for datasets
   *
   * @param org the organization owner of the datasets
   * @param path the path reference to include in the result set
   * @return a result set of datasets
   */
  def searchDatasets(org: String, afterDate: Long): ResultSet[Dataset]

  /**
   * Add a new dataset to the DB
   *
   * @param org the associated organization
   * @param ds the dataset to be added to the DB
   * @return the same dataset with optionally some more metadata
   */
  def createDataset(org: String, ds: Dataset): Dataset

  /**
   * Look up a dataset in the DB
   *
   * Throws a {{NotFoundInDb}} exception if the resource isn't found
   *
   * @param org the org associated with the dataset
   * @param id the dataset ID
   * @return the dataset in the org with the provided ID
   */
  def getDataset(org: String, id: String): Dataset

  /**
   * Update a dataset in the DB
   *
   * @param org the org associated with the dataset
   * @param id the dataset ID
   * @param updates the changes to make to the dataset
   * @return the upddated dataset
   */
  def updateDataset(org: String, id: String, updates: Dataset.Update): Dataset
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
  import utils.validateIdentifier
  import org.postgresql.util.PSQLException
  import org.postgresql.util.PSQLState._
  import play.api.libs.json._
  import org.joda.time.DateTime
  import org.joda.time.DateTimeZone.UTC

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
   * @return a set of n distinct UUIDs with string semi-unique identifiers
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
        result ++= uuids.map { u => (UUID.fromString(u), u.take(8)) }.groupBy(_._2).map(_._2.head)
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

  def addOrg(id: String) {
    validateIdentifier(id)
    logger.info(s"creating new organization $id")
    val newIds = try {
      DB.localTx { implicit session =>
        sql"insert into org (provided_id) values ($id) returning provided_id"
          .map(_.string("provided_id"))
          .list
          .apply()
      }
    } catch {
      case e: PSQLException if e.getMessage.contains("duplicate key value") =>
        throw new CannotCreateOrganizationException(id)
    }
    if (newIds.isEmpty) {
      throw new CannotCreateOrganizationException(id)
    } else if (newIds != List(id)) {
      val msg = s"unexpected organizations created matching $id: ${newIds.mkString(", ")}"
      throw new UnexpectedResultException(msg)
    }
  }

  def delOrg(id: String) {
    val deletedOrgs = DB.localTx { implicit session =>
      sql"delete from org where provided_id = $id returning provided_id"
        .map(_.string("provided_id"))
        .list
        .apply()
    }
    if (deletedOrgs.isEmpty) {
      throw new OrganizationNotFoundException(id)
    } else if (deletedOrgs != List(id)) {
      val msg = s"unexpected orgs deleted matching $id: ${deletedOrgs.mkString(", ")}"
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

  def addOrg(id: String, users: Seq[String]) {
    validateIdentifier(id)
    DB.localTx { implicit session =>
      val userIds = lookupUserIds(users)

      val newIdInsert = try {
        sql"insert into org (provided_id) values ($id) returning org_id::text"
          .map(_.string("org_id"))
          .single
          .apply()
      } catch {
        case e: PSQLException if e.getMessage.contains("duplicate key value") =>
          throw new CannotCreateOrganizationException(id)
      }

      newIdInsert match {
        case Some(newId) => {
          val batchVals = userIds.map(Seq(newId, _))
          sql"insert into org_membership (org_id, auth_id) values (?::uuid, ?::uuid)"
            .batch(batchVals: _*)
            .apply()
        }
        case None =>
          throw new CannotCreateOrganizationException(id)
      }
    }
  }

  def addToOrg(id: String, users: Seq[String]) {
    validateIdentifier(id)
    DB.localTx { implicit session =>
      val userIds = lookupUserIds(users)

      val orgIdLookup = sql"select org_id::text from org where provided_id = $id"
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
          throw new OrganizationNotFoundException(id)
      }
    }
  }

  def listUserOrgs(user: String): Seq[String] = DB.readOnly { implicit session =>
    sql"""select org.provided_id org
            from org
            inner join org_membership on org.org_id = org_membership.org_id
            inner join auth on org_membership.auth_id = auth.auth_id
            where auth.username = $user"""
      .map(_.string("org"))
      .list
      .apply()
  }

  def listOrgs: Seq[String] = DB.readOnly { implicit session =>
    sql"select provided_id from org"
      .map(_.string("provided_id"))
      .list
      .apply()
  }

  def listOrgUsers(org: String): Seq[String] = DB.readOnly { implicit session =>
    sql"""select username
          from auth
          inner join org_membership on auth.auth_id = org_membership.auth_id
          inner join org on org_membership.org_id = org.org_id
          where org.provided_id = $org"""
      .map(_.string("username"))
      .list
      .apply()
  }

  def renameOrg(current: String, next: String) {
    validateIdentifier(next)
    logger.info(s"renaming organization $current to $next")
    val updatedOrgs = DB.localTx { implicit session =>
      sql"""update org
            set provided_id = $next, updated_at = current_timestamp
            where provided_id = $current
            returning provided_id"""
        .map(_.string("provided_id"))
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

  def userHasOrgAccess(user: String, org: String) = DB.readOnly { implicit session =>
    sql"""select exists(
          select 1 from auth inner join org_membership on auth.auth_id = org_membership.auth_id
          inner join org on org_membership.org_id = org.org_id
          where username = $user and org.provided_id = $org
          limit 1)"""
      .map(_.boolean(1))
      .single
      .apply()
      .get
  }

  def searchDatasets(org: String, afterDate: Long = 32503680000L) = DB.readOnly { implicit session =>
    val datasets = sql"""
          select
          dataset.provided_id provided_id,
          dataset.dataset_type::text dataset_type,
          dataset.schema::text dschema,
          extract(epoch from dataset.created_at) created_at,
          extract(epoch from dataset.updated_at) updated_at
          from dataset inner join org on dataset.org_id = org.org_id
          where org.provided_id = ${org}
          and dataset.created_at < timestamp with time zone 'epoch' + ${afterDate} * interval '1 second'
          order by dataset.created_at
          limit 100"""
      .map { rs =>
        Dataset(
          rs.string("provided_id"),
          rs.string("dataset_type")(0),
          Json.toJson(rs.string("dschema")),
          createdAt = Some(jodaFromEpoch(rs.double("created_at"))),
          updatedAt = Some(jodaFromEpoch(rs.double("updated_at"))))
      }
      .list
      .apply()
    ResultSet(datasets)
  }

  private def jodaFromEpoch(epoch: Double) = new DateTime((1000 * epoch).toLong).withZone(UTC)

  def createDataset(org: String, ds: Dataset) = DB.localTx { implicit session =>
    val newId = sql"""
          insert into dataset (org_id, provided_id, dataset_type, schema)
          select org.org_id, ${ds.id}, ${ds.dtype}, ${ds.schema.toString}::json
          from org where org.provided_id = ${org}
          returning dataset_id::text"""
      .map(_.string(1))
      .single
      .apply()
    newId match {
      case Some(uuid) => {
        sql"""
          select
          dataset.provided_id,
          dataset.dataset_type::text,
          dataset.schema dschema,
          extract(epoch from created_at) created_at,
          extract(epoch from updated_at) updated_at
          from dataset
          where dataset_id = ${uuid}::uuid"""
          .map { rs =>
            Dataset(
              rs.string("provided_id"),
              rs.string("dataset_type")(0),
              Json.parse(rs.string("dschema")),
              createdAt = Some(jodaFromEpoch(rs.double("created_at"))),
              updatedAt = Some(jodaFromEpoch(rs.double("updated_at"))))
          }
          .single
          .apply()
          .get
      }

      case None => throw new OrganizationNotFoundException(org)

    }
  }

  def getDataset(org: String, id: String): Dataset = DB.readOnly { implicit session =>
    val dsOpt = sql"""
      select
      dataset.provided_id,
      dataset.dataset_type::text,
      dataset.schema dschema,
      extract(epoch from dataset.created_at) created_at,
      extract(epoch from dataset.updated_at) updated_at
      from dataset inner join org on dataset.org_id = org.org_id
      where dataset.provided_id = ${id}"""
      .map { rs =>
        Dataset(
          rs.string("provided_id"),
          rs.string("dataset_type")(0),
          Json.parse(rs.string("dschema")),
          createdAt = Some(jodaFromEpoch(rs.double("created_at"))),
          updatedAt = Some(jodaFromEpoch(rs.double("updated_at"))))
      }
      .single
      .apply()

    dsOpt match {
      case Some(ds) => ds
      case None => throw new NotFoundInDb()
    }
  }

  def updateDataset(org: String, id: String, updates: Dataset.Update) = DB.localTx { implicit session =>
    val idOpt = sql"""
      select dataset.dataset_id::text dataset_id
      from dataset inner join org on dataset.org_id = org.org_id
      where org.provided_id = ${org} and dataset.provided_id = ${id}
      """
      .map(_.string("dataset_id"))
      .single
      .apply()

    idOpt match {
      case Some(uuid) => {
        updates.id foreach { newProvidedId =>
          sql"""
          update dataset set provided_id = ${newProvidedId} where dataset_id = ${uuid}::uuid
          """
            .execute
            .apply()
        }
        sql"""
          select
          dataset.provided_id,
          dataset.dataset_type::text,
          dataset.schema dschema,
          extract(epoch from created_at) created_at,
          extract(epoch from updated_at) updated_at
          from dataset
          where dataset_id = ${uuid}::uuid"""
          .map { rs =>
            Dataset(
              rs.string("provided_id"),
              rs.string("dataset_type")(0),
              Json.parse(rs.string("dschema")),
              createdAt = Some(jodaFromEpoch(rs.double("created_at"))),
              updatedAt = Some(jodaFromEpoch(rs.double("updated_at"))))
          }
          .single
          .apply()
          .get

      }
      case None => throw new NotFoundInDb()
    }
  }
}
