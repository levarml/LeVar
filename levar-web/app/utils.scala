package object utils {

  import play.api.mvc.Accepting

  /**
   * Checks whether a string identifier matches a simple URL friendly pattern
   */
  def validateIdentifier(s: String) {
    if (!s.matches("^[-\\w\\.]+$"))
      throw new InvalidIdentifierException(s)
  }

  val AcceptsText = Accepting("text/plain")
}
