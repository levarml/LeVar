/**
 * Power user tools to update the DB directly from the Scala console
 */
package object console {

  import db._

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
  def dropUser(name: String) { /* TODO */ }

  /**
   * Create a new organization
   *
   * @param name the name of the new organization
   */
  def createOrg(name: String) { /* TODO */ }

  /**
   * Drop an organization from the DB
   *
   * @param name the name of the organization to remove
   */
  def dropOrg(name: String) { /* TODO */ }

  /**
   * Add members to an organization
   *
   * @param org the name of the organization
   * @param users the names of users to add to the organization
   */
  def addToOrg(org: String, users: Seq[String]) { /* TODO */ }

  /**
   * Add a user to an organization
   *
   * @param org the organization name
   * @param usre the user name
   */
  def addToOrg(org: String, user: String) { addToOrg(org, Seq(user)) }
}
