package levar

package object util {

  import java.net.{URL, MalformedURLException}

  /** Regex match for valid organization names */
  def validOrgName(name: String): Boolean = name.matches("""[-\w\.]+""")

  /** Check for valid URL */
  def validURL(url: String): Boolean = {
    try {
      new URL(url)
      true
    } catch {
      case _: MalformedURLException => false
    }
  }

  def eitherAsAny(eith: Either[_, _]): Any = eith.fold(identity, identity)
}
