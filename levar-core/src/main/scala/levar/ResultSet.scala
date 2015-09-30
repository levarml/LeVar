package levar

/**
 * Data type of query result sets for whatever
 *
 * @tparam A the type of object in the result set
 * @param size the size of the result set
 * @param path the URL path to view the result set directly
 * @param total the total results in the result set
 * @param nextPath a URL path to link to the next query results
 */
case class ResultSet[A](
    items: Seq[A],
    path: Option[String] = None,
    total: Option[Int] = None,
    nextPath: Option[String] = None) extends Traversable[A] {

  def foreach[U](f: A => U) { items.foreach(f) }

  override def lastOption: Option[A] = items.lastOption
}
