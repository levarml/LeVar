package utils

package object auth {

  import play.api.mvc._
  import play.api.mvc.Results._
  import play.api.libs.iteratee.Done
  import org.apache.commons.codec.binary.{ Base64, StringUtils }

  private val BasicAuthStr = """^Basic ([A-Za-z0-9=]+)$""".r
  private val UsernamePasswdStr = """^([^:]+):(.*)$""".r

  private def getUser(request: RequestHeader): Option[String] = {
    request.headers.get("Authorization").flatMap { authorization =>
      authorization match {
        case BasicAuthStr(encodedAuthStr) => {
          StringUtils.newStringUtf8(Base64.decodeBase64(encodedAuthStr)) match {
            case UsernamePasswdStr(username, password) => {
              if (db.impl.getAuth(username, password))
                Some(username)
              else
                None
            }
          }
        }
        case _ => None
      }
    }
  }

  def Authenticated(action: String => EssentialAction): EssentialAction = {
    EssentialAction { request =>
      getUser(request).map(u => action(u)(request)).getOrElse {
        Done(Unauthorized.withHeaders("WWW-Authenticate" -> "Basic realm=\"Authorized\""))
      }
    }
  }

  def HasOrgAccess(user: String, org: String)(action: EssentialAction): EssentialAction = {
    if (db.impl.userHasOrgAccess(user, org))
      action
    else
      EssentialAction { _ => Done(NotFound) }
  }
}
