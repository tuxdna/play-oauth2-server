package controllers
import scalaoauth2.provider._
import play.api.mvc.{ Action, Controller }
import oauth2.OAuthDataHandler

object OAuth2Controller extends Controller with OAuth2Provider {
  def accessToken = Action { implicit request =>

    val log = play.Logger.of("application")

    log.debug("Invoked OAuth2Controller request")

    issueAccessToken(new OAuthDataHandler())
  }
}