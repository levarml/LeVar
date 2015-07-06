/**
 * Power user tools to update the DB directly from the Scala console
 */
package object console {

  import db._

  /**
   * Set up the DB
   */
  def dbSetUp() { db.impl.setUp() }

  /**
   * Create a new user
   *
   * @param name the new user name
   * @param pass the new user password
   */
  def createUser(name: String, pass: String) { db.impl.addAuth(name, pass) }

  /**
   * Drop a user from the DB
   *
   * @param name name of the user to remove
   */
  def dropUser(name: String) { db.impl.delAuth(name) }

  /**
   * Create a new organization
   *
   * @param name the name of the new organization
   */
  def createOrg(name: String) { db.impl.addOrg(name) }

  /**
   * Create a new organization with users
   *
   * @param name the name of the new organiation
   */
  def createOrg(name: String, users: Seq[String]) { db.impl.addOrg(name, users) }

  /**
   * Drop an organization from the DB
   *
   * @param name the name of the organization to remove
   */
  def dropOrg(name: String) { db.impl.delOrg(name) }

  /**
   * Add members to an organization
   *
   * @param org the name of the organization
   * @param users the names of users to add to the organization
   */
  def addToOrg(org: String, users: Seq[String]) { db.impl.addToOrg(org, users) }

  /**
   * Add a user to an organization
   *
   * @param org the organization name
   * @param user the user name
   */
  def addToOrg(org: String, user: String) { addToOrg(org, Seq(user)) }

  /**
   * Print the organizations for a user
   *
   * @param user the user name
   */
  def listOrgs(user: String) { for (org <- db.impl.listUserOrgs(user)) println(s"- $org") }
}
