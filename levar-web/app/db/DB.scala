package db

import levar._
import utils._

class CannotCreateOrganizationException(msg: String) extends Exception(msg)
class OrganizationNotFoundException(msg: String) extends Exception(msg)
class CannotCreateUserException(msg: String) extends Exception(msg)
class ForbiddenException(msg: String) extends Exception(msg)
class UnauthorizedException(msg: String) extends Exception(msg)
class UserNotFoundException(msg: String) extends Exception(msg)
class UnexpectedResultException(msg: String) extends Exception(msg)

class DatasetIdAlreadyExists(msg: String) extends Exception(msg)
class ExperimentIdAlreadyExists(msg: String) extends Exception(msg)

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
   */
  def createDataset(org: String, ds: Dataset)

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
   */
  def updateDataset(org: String, id: String, updates: Dataset.Update)

  /**
   * Delete a dataset from the DB
   *
   * @param org the org associated with the dataset
   * @param id the dataset ID
   */
  def deleteDataset(org: String, id: String)

  /**
   * Search dataset data
   *
   * @param org the org associated with the dataset
   * @param datasetId the dataset ID
   * @param after if applicable, data must follow this datum ID
   * @param max if applicatble, number of rows to return
   */
  def searchData(org: String, datasetId: String, after: Option[String] = None, max: Option[Int] = None, withValue: Boolean): ResultSet[Datum]

  def searchExperiments(org: String): ResultSet[Experiment]

  def searchExperiments(org: String, datasetId: String): ResultSet[Experiment]

  def createExperiment(org: String, datasetId: String, experiment: Experiment)

  def getExperiment(org: String, datasetId: String, experimentId: String): Experiment

  def updateExperiment(org: String, datasetId: String, experimentId: String, updates: Experiment.Update)
}

/**
 * Default/production implementation of [[Database]]
 */
object impl extends Database with JsonLogging {
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
  import play.api.libs.json.Json.toJson
  import org.joda.time.DateTime
  import org.joda.time.DateTimeZone.UTC
  import levar.json._
  import levar.Dataset._
  import collection.mutable.Buffer

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

  GlobalSettings.loggingSQLErrors = conf.getBoolean("db.log_errors")

  /** Set up the database */
  def setUp() {
    info("status" -> "starting", "action" -> "setup_db")
    DB.localTx { implicit session =>
      sqlFromClasspath("/setup.sql").execute.apply()
    }
    info("status" -> "done", "action" -> "setup_db")
  }

  /** Tear down the database */
  def tearDown() {
    info("status" -> "starting", "action" -> "tear_down_db")
    DB.localTx { implicit session =>
      sqlFromClasspath("/teardown.sql").execute.apply()
    }
    info("status" -> "done", "action" -> "tear_down_db")
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
    info("status" -> "starting", "action" -> "add_auth", "user" -> user)
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
        info("status" -> "success", "action" -> "add_auth", "user" -> user)
      }
      case None => {
        throw new CannotCreateUserException(user)
      }
    }
  }

  def delAuth(user: String) {
    info("status" -> "starting", "action" -> "del_auth", "user" -> user)
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
    } else {
      info("status" -> "success", "action" -> "del_auth", "user" -> user)
    }
  }

  def renameAuth(current: String, next: String) {
    info("status" -> "starting", "action" -> "rename_auth", "user" -> current, "update" -> next)
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
    } else {
      info("status" -> "success", "action" -> "rename_auth", "user" -> current, "update" -> next)
    }
  }

  def resetPassword(user: String, currentPass: String, nextPass: String) {
    info("status" -> "starting", "action" -> "reset_password", "user" -> user)
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
    } else {
      info("status" -> "success", "action" -> "reset_password", "user" -> user)
    }
  }

  def addOrg(id: String) {
    validateIdentifier(id)
    info("status" -> "starting", "action" -> "add_org", "org" -> id)
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
    } else {
      info("status" -> "success", "action" -> "add_org", "org" -> id)
    }
  }

  def delOrg(id: String) {
    info("status" -> "starting", "action" -> "del_org", "org" -> id)
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
    } else {
      info("status" -> "success", "action" -> "del_org", "org" -> id)
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
    info("status" -> "starting", "action" -> "rename_org", "org" -> current, "update" -> next)
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
      info("status" -> "success", "action" -> "rename_org", "org" -> current, "update" -> next)
    }
  }

  def userHasOrgAccess(user: String, org: String) = DB.readOnly { implicit session =>
    info("status" -> "check", "action" -> "user_has_org_access", "user" -> user, "org" -> org)
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

  def datasetType(c: String) =
    if (c == ClassificationType.code.toString)
      ClassificationType
    else if (c == RegressionType.code.toString)
      RegressionType
    else
      throw new IllegalArgumentException(s"invalid dataset type: $c")

  def datasetValidator(c: String) = {
    Json.parse(c).asOpt[DataValidator] match {
      case Some(v) => v
      case None => throw new IllegalArgumentException(s"invalid format for dataset validator: ${c}")
    }
  }

  def searchDatasets(org: String, afterDate: Long = 32503680000L) = DB.readOnly { implicit session =>
    val datasets = sql"""
          select
          dataset.provided_id dataset_ident,
          dataset.dataset_type::text dataset_type,
          dataset.schema::text dschema,
          extract(epoch from dataset.created_at) d_created_at,
          extract(epoch from dataset.updated_at) d_updated_at,
          count(datum.datum_id) size
          from dataset inner join org on dataset.org_id = org.org_id
                       left join datum on dataset.dataset_id = datum.dataset_id
          where org.provided_id = ${org}
          and dataset.created_at < timestamp with time zone 'epoch' + ${afterDate} * interval '1 second'
          group by dataset_ident, dataset_type, dschema, d_created_at, d_updated_at
          order by d_created_at
          limit 100"""
      .map { rs =>
        Dataset(
          rs.string("dataset_ident"),
          datasetType(rs.string("dataset_type")),
          datasetValidator(rs.string("dschema")),
          createdAt = Some(jodaFromEpoch(rs.double("d_created_at"))),
          updatedAt = Some(jodaFromEpoch(rs.double("d_updated_at"))),
          size = Some(rs.int("size")))
      }
      .list
      .apply()
    ResultSet(datasets)
  }

  private def jodaFromEpoch(epoch: Double) = new DateTime((1000 * epoch).toLong).withZone(UTC)

  private val datasetDupeKeyMsgSnippet =
    """duplicate key value violates unique constraint "dataset_org_id_provided_id_key""""

  def createDataset(org: String, ds: Dataset) = DB.localTx { implicit session =>
    try {
      sql"""
        insert into dataset (org_id, provided_id, dataset_type, schema)
        select org.org_id, ${ds.id}, ${ds.dtype.code}, ${toJson(ds.schema).toString}::json
        from org where org.provided_id = ${org}"""
        .execute
        .apply()
    } catch {
      case e: PSQLException if e.getMessage.contains(datasetDupeKeyMsgSnippet) =>
        throw new DatasetIdAlreadyExists(s"$org/${ds.id}")
    }
  }

  def getDataset(org: String, id: String): Dataset = DB.readOnly { implicit session =>
    info("status" -> "starting", "action" -> "get_dataset", "org" -> org, "id" -> id)
    val dsOpt = sql"""
      select
      dataset.dataset_id::text dsid,
      dataset.provided_id ds_ident,
      dataset.dataset_type::text dataset_type,
      dataset.schema::text dschema,
      extract(epoch from dataset.created_at) d_created_at,
      extract(epoch from dataset.updated_at) d_updated_at,
      count(datum.datum_id) as size
      from dataset inner join org on dataset.org_id = org.org_id
                   left join datum on dataset.dataset_id = datum.dataset_id
      where dataset.provided_id = ${id} and org.provided_id = ${org}
      group by dsid, ds_ident, dataset_type, dschema, d_created_at, d_updated_at"""
      .map { rs =>
        val ds = Dataset(
          rs.string("ds_ident"),
          datasetType(rs.string("dataset_type")),
          datasetValidator(rs.string("dschema")),
          createdAt = Some(jodaFromEpoch(rs.double("d_created_at"))),
          updatedAt = Some(jodaFromEpoch(rs.double("d_updated_at"))),
          size = Some(rs.int("size")))
        (rs.string("dsid"), ds)
      }
      .single
      .apply()

    dsOpt match {
      case Some((dsid, ds)) => {
        info("status" -> "get_dataset_stats", "action" -> "get_dataset", "org" -> org, "id" -> id)
        val classCountsList = sql"""
          select datum.cvalue cls, count(1) count from datum
          where datum.dataset_id = ${dsid}::uuid and datum.cvalue is not null
          group by cls"""
          .map { rs => (rs.string("cls"), rs.int("count")) }
          .list
          .apply()

        val summaryStats = sql"""
          select min, max, mean, stddev, p[1] median, p[2] p10, p[3] p90 from (
            select
              min(rvalue),
              max(rvalue),
              avg(rvalue) mean,
              stddev(rvalue),
              (percentile_disc(array[.5, .1, .9]) within group (order by rvalue)) p
            from datum
            where dataset_id = ${dsid}::uuid and rvalue is not null) q1"""
          .map { rs =>
            (rs.doubleOpt("min"),
              rs.doubleOpt("max"),
              rs.doubleOpt("mean"),
              rs.doubleOpt("stddev"),
              rs.doubleOpt("median"),
              rs.doubleOpt("p10"),
              rs.doubleOpt("p90"))
          }
          .single
          .apply()

        info("status" -> "success", "action" -> "get_dataset", "org" -> org, "id" -> id)

        if (classCountsList.nonEmpty) {
          ds.copy(classCounts = Some(classCountsList.toMap))
        } else if (summaryStats.nonEmpty) {
          summaryStats match {
            case Some((Some(mn), Some(mx), Some(mean), Some(std), Some(med), Some(p10), Some(p90))) => {
              ds.copy(summaryStats = Some(Dataset.RegressionSummaryStats(mn, mx, mean, std, med, p10, p90)))
            }
            case _ => ds
          }
        } else {
          ds
        }
      }
      case None => throw new NotFoundInDb()
    }
  }

  private def hex2int(hex: String): Int = {
    java.lang.Long.parseLong(hex, 16).toInt
  }

  def updateDataset(org: String, id: String, updates: Dataset.Update) = DB.localTx { implicit session =>
    try {
      info("status" -> "starging", "action" -> "update_dataset", "org" -> org, "id" -> id)
      val idOpt = sql"""
        select dataset.dataset_id::text dataset_id, dataset.dataset_type::text dataset_type
        from dataset inner join org on dataset.org_id = org.org_id
        where org.provided_id = ${org} and dataset.provided_id = ${id}
        """
        .map(row => (row.string("dataset_id"), row.string("dataset_type")))
        .single
        .apply()

      idOpt match {
        case Some((uuid, dtypeStr)) => {
          val dtype = datasetType(dtypeStr)
          updates.id foreach { newProvidedId =>
            try {
              sql"""
                update dataset set
                  provided_id = ${newProvidedId},
                  updated_at = current_timestamp
                where dataset_id = ${uuid}::uuid
                """
                .execute
                .apply()
            } catch {
              case e: PSQLException if e.getMessage.contains("duplicate key value violates unique constraint") => {
                throw new DatasetIdAlreadyExists(newProvidedId)
              }
            }
          }
          updates.data foreach { dataBlock =>
            val newData: Seq[(String, Any)] = for {
              datum <- dataBlock
              value <- datum.valueAsAny
            } yield (datum.data.toString, value)

            val newIds: Seq[(UUID, String)] = genUuids(newData.size)
            val batchInserts: Seq[Seq[Any]] = for {
              ((datumId, datumIdShort), (data, value)) <- (newIds zip newData)
            } yield {
              Seq(datumId.toString, hex2int(datumIdShort), data, value, uuid)
            }
            dtype match {
              case Dataset.ClassificationType => {
                sql"""
                  insert into datum (datum_id, ident, data, cvalue, dataset_id)
                  values (?::uuid, ?, ?::json, ?, ?::uuid)
                  """
                  .batch(batchInserts: _*)
                  .apply()
              }
              case Dataset.RegressionType => {
                sql"""
                  insert into datum (datum_id, ident, data, rvalue, dataset_id)
                  values (?::uuid, ?, ?::json, ?, ?::uuid)
                  """
                  .batch(batchInserts: _*)
                  .apply()
              }
            }
          }
        }
        case None => throw new NotFoundInDb()
      }
      info("status" -> "success", "action" -> "update_dataset", "org" -> org, "id" -> id)
    } catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }
  }

  def deleteDataset(org: String, id: String) = {
    info("status" -> "starting", "action" -> "delete_dataset", "org" -> org, "id" -> id)
    val delIds = DB.localTx { implicit session =>
      sql"""
          delete from dataset where dataset_id in (
            select dataset.dataset_id
            from dataset inner join org on dataset.org_id = org.org_id
            where org.provided_id = ${org} and dataset.provided_id = ${id})
          returning provided_id
          """
        .map(_.string("provided_id"))
        .list
        .apply()
    }
    if (delIds.isEmpty) {
      throw new NotFoundInDb()
    } else if (delIds != List(id)) {
      val msg = s"unexpected datasets deleted matching $org/$id: ${delIds.mkString(", ")}"
      throw new UnexpectedResultException(msg)
    } else {
      info("status" -> "success", "action" -> "delete_dataset", "org" -> org, "id" -> id)
    }
  }

  def searchData(org: String, datasetId: String, after: Option[String] = None, max: Option[Int] = None, withVal: Boolean = false) = DB.readOnly { implicit session =>
    info("status" -> "starting", "action" -> "search_data", "org" -> org, "dataset_id" -> datasetId)
    val datasetDetails = sql"""
        select dataset.dataset_id d_id, count(datum.datum_id) total_count
        from dataset inner join org on dataset.org_id = org.org_id left join datum on datum.dataset_id = dataset.dataset_id
        where dataset.provided_id = ${datasetId} and org.provided_id = ${org}
        group by d_id"""
      .map { rs => (rs.string("d_id"), rs.int("total_count")) }
      .single
      .apply()
    val (datasetUuid, totalCount) = datasetDetails match {
      case Some((uuid, c)) => (uuid, c)
      case None => throw new NotFoundInDb
    }
    val afterIdent = after.map(hex2int).getOrElse(Int.MinValue)
    val maxRows = max.getOrElse(100)
    val datumQ: Seq[Datum] = sql"""
        select to_hex(ident) ident_hex, data, rvalue, cvalue
        from datum
        where dataset_id = ${datasetUuid}::uuid
        and ident > $afterIdent
        order by ident
        limit $maxRows"""
      .map { rs =>
        val ident = rs.string("ident_hex")
        val datastr = rs.string("data")
        val datajs = Json.parse(datastr)
        val rvalue = rs.doubleOpt("rvalue")
        val cvalue = rs.stringOpt("cvalue")
        val value: Either[Double, String] = (rvalue, cvalue) match {
          case (Some(num), None) => Left(num)
          case (None, Some(cls)) => Right(cls)
          case _ => throw new UnexpectedResultException(s"Unprocessed value for datum ${org}/${datasetId}/${ident}: ${(cvalue, rvalue)}")
        }
        Datum(datajs, if (withVal) Some(value) else None, Some(ident))
      }
      .list
      .apply()

    ResultSet(datumQ, total = Some(totalCount))
  }

  def searchExperiments(org: String): ResultSet[Experiment] = DB.readOnly { implicit session =>
    info("status" -> "starting", "action" -> "search_data", "org" -> org)
    val experiments = sql"""
        select distinct experiment.provided_id exp_id, dataset.provided_id dataset_id
        from experiment
          inner join dataset on dataset.dataset_id = experiment.dataset_id
          inner join org on dataset.org_id = org.org_id
        where org.provided_id = ${org}"""
      .map { rs => Experiment(rs.string("exp_id"), Some(rs.string("dataset_id"))) }
      .list
      .apply()
    ResultSet(experiments)
  }

  def searchExperiments(org: String, datasetId: String): ResultSet[Experiment] = DB.readOnly { implicit session =>
    info("status" -> "starting", "action" -> "search_data", "org" -> org)
    val experiments = sql"""
        select distinct experiment.provided_id exp_id, dataset.provided_id dataset_id
        from experiment
          inner join dataset on dataset.dataset_id = experiment.dataset_id
          inner join org on dataset.org_id = org.org_id
        where dataset.provided_id = ${datasetId}
          and org.provided_id = ${org}
        """
      .map { rs => Experiment(rs.string("exp_id"), Some(rs.string("dataset_id"))) }
      .list
      .apply()
    ResultSet(experiments)
  }

  private val experimentDupeKeyMsgSnippet =
    """duplicate key value violates unique constraint "experiment_provided_id_dataset_id_key""""

  def createExperiment(org: String, datasetId: String, experiment: Experiment) = DB.localTx { implicit session =>
    info("status" -> "starting", "action" -> "create_experiment", "org" -> org, "dataset" -> datasetId, "experiment" -> experiment.id)
    val datasetUuidOpt = sql"""
        select dataset.dataset_id::text did
        from dataset
          inner join org on dataset.org_id = org.org_id
        where dataset.provided_id = ${datasetId}
          and org.provided_id = ${org}"""
      .map(_.string("did"))
      .single
      .apply()

    datasetUuidOpt match {
      case Some(datasetUuid) => {
        try {
          sql"""
              insert into experiment (provided_id, dataset_id)
              values (${experiment.id}, ${datasetUuid}::uuid)"""
            .update
            .apply()
          info("status" -> "success", "action" -> "create_experiment", "org" -> org, "dataset" -> datasetId, "experiment" -> experiment.id)
        } catch {
          case e: PSQLException if e.getMessage.contains(experimentDupeKeyMsgSnippet) =>
            throw new ExperimentIdAlreadyExists(s"$org/$datasetId/${experiment.id}")
        }
      }
      case None => {
        throw new NotFoundInDb
      }
    }
  }

  def getExperiment(org: String, datasetId: String, experimentId: String) = DB.readOnly { implicit session =>
    info("status" -> "starting", "action" -> "get_experiment", "org" -> org, "dataset" -> datasetId, "experiment" -> experimentId)
    val experimentOpt = sql"""
        select
          experiment.provided_id exp_id,
          dataset.provided_id data_id,
          extract(epoch from experiment.created_at) e_created_at,
          extract(epoch from experiment.updated_at) e_updated_at,
          count (prediction.prediction_id) num_predictions
        from experiment
          inner join dataset on experiment.dataset_id = dataset.dataset_id
          inner join org on dataset.org_id = org.org_id
          left join prediction on prediction.experiment_id = experiment.experiment_id
        where experiment.provided_id = ${experimentId}
          and org.provided_id = ${org}
          and dataset.provided_id = ${datasetId}
        group by exp_id, data_id, e_created_at, e_updated_at"""
      .map { rs =>
        Experiment(
          rs.string("exp_id"),
          datasetId = Some(rs.string("data_id")),
          createdAt = Some(jodaFromEpoch(rs.double("e_created_at"))),
          updatedAt = Some(jodaFromEpoch(rs.double("e_updated_at"))))
      }
      .single
      .apply()

    experimentOpt match {
      case Some(experiment) => {
        info("status" -> "success", "action" -> "get_experiment", "org" -> org, "dataset" -> datasetId, "experiment" -> experimentId)
        experiment
      }
      case None => {
        throw new NotFoundInDb
      }
    }
  }

  def updateExperiment(org: String, datasetId: String, experimentId: String, updates: Experiment.Update) = DB.localTx { implicit session =>
    try {
      info("status" -> "starging", "action" -> "update_experiment", "org" -> org, "dataset" -> datasetId, "experiment" -> experimentId)
      val idOpt = sql"""
        select
          experiment.experiment_id::text exp_id, dataset.dataset_id::text data_id, dataset.dataset_type::text dataset_type
        from experiment
          inner join dataset on experiment.dataset_id = dataset.dataset_id
          inner join org on dataset.org_id = org.org_id
        where org.provided_id = ${org}
          and dataset.provided_id = ${datasetId}
          and experiment.provided_id = ${experimentId}
        """
        .map(row => (row.string("exp_id"), row.string("data_id"), row.string("dataset_type")))
        .single
        .apply()

      idOpt match {
        case Some((experimentUuid, datasetUuid, dtypeStr)) => {
          val dtype = datasetType(dtypeStr)
          updates.id foreach { newProvidedId =>
            try {
              sql"""
                update experiment set
                  provided_id = ${newProvidedId},
                  updated_at = current_timestamp
                where dataset_id = ${experimentUuid}::uuid
                """
                .execute
                .apply()
            } catch {
              case e: PSQLException if e.getMessage.contains("duplicate key value violates unique constraint") => {
                throw new ExperimentIdAlreadyExists(newProvidedId)
              }
            }
          }
          updates.data foreach { dataBlock =>

            val datumProvidedIds = for {
              prediction <- dataBlock
              datum <- prediction.datum
              datumId <- datum.id
            } yield hex2int(datumId)
            val datumProvidedIdsBuf = List(datumProvidedIds: _*)

            val datumUuids = {
              val datumIdents = Buffer(dataBlock.map(_.datumId).map(hex2int): _*)
              sql"""
                  select to_hex(ident) datum_ident, datum_id::text datum_uuid
                  from datum
                  where dataset_id = ${datasetUuid}::uuid and ident in (${datumIdents})"""
                .map { rs => rs.string("datum_ident") -> rs.string("datum_uuid") }
                .list
                .apply()
            }

            val datumUuidMap = datumUuids.toMap

            val newIds: Seq[(UUID, String)] = genUuids(datumUuidMap.size)

            val batchInserts: Seq[Seq[Any]] = for {
              ((newUuid, newIdent), prediction) <- (newIds zip dataBlock)
              datumUuid <- datumUuidMap.get(prediction.datumId)
            } yield {
              Seq(newUuid.toString, hex2int(newIdent), datumUuid.toString, experimentUuid.toString, prediction.valueAsAny)
            }

            dtype match {
              case ClassificationType => {
                sql"""
                    insert into prediction (prediction_id, ident, datum_id, experiment_id, cvalue)
                    values (?::uuid, ?, ?::uuid, ?::uuid, ?)"""
                  .batch(batchInserts: _*)
                  .apply()
              }
              case RegressionType => {
                sql"""
                    insert into prediction (prediction_id, ident, datum_id, experiment_id, rvalue)
                    values (?::uuid, ?, ?::uuid, ?::uuid, ?)"""
                  .batch(batchInserts: _*)
                  .apply()
              }
            }
          }
        }
        case None => throw new NotFoundInDb()
      }
      info("status" -> "success", "action" -> "update_experiment", "org" -> org, "dataset" -> datasetId, "experiment" -> experimentId)
    } catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }
  }
}
