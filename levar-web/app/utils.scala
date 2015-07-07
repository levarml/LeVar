package object utils {

  import play.api.mvc.Accepting

  /**
   * True if the identifier matches a simple URL friendly pattern
   */
  def validIdentifier(s: String) = s.matches("^[-\\w\\.]+$")

  /**
   * Checks whether a string identifier matches a simple URL friendly pattern
   */
  def validateIdentifier(s: String) {
    if (!validIdentifier(s))
      throw new InvalidIdentifierException(s)
  }

  val AcceptsText = Accepting("text/plain")
}
