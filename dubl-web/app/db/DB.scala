package db

import java.util.UUID

/**
 * Database access layer
 */
trait Dao {

  /**
   * Generate a set of UUIDs
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
   * Grant access for an authenticated user to another object
   *
   * @param auth the authenticated user ID
   * @param object the other object ID
   * @param priv the privilege being granted: 'r' = read, 'w' = write, 'a' = all
   */
  def grant(auth: UUID, obj: UUID, priv: Char)
}

/**
 * Default/production implementation of [[Dao]]
 */
object impl extends Dao {
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
      s"jdbc:postgresql://${dbUri.getHost}:${dbUri.getPort}${dbUri.getPath}"
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

  def getAuth(user: String, pass: String) = None

  def genUuids(n: Int) = (for (_ <- 1 to n) yield UUID.randomUUID).toSeq

  def addAuth(user: String, pass: String) = UUID.randomUUID

  def grant(auth: UUID, obj: UUID, priv: Char) {}
}
