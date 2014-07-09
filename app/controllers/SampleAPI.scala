package controllers

import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import scalaoauth2.provider.OAuth2Provider
import oauth2.OAuthDataHandler

object SampleAPI extends Controller with OAuth2Provider {

  def status(id: String) = Action { implicit request =>

    authorize(new OAuthDataHandler()) { authInfo =>

      val user = authInfo.user

      val sampleData = Set("123", "345", "567", "789", "890")

      if (sampleData.contains(id)) {
        val responseData = Map("id" -> id, "total" -> "100", "completed" -> "30")
        Ok(Json.toJson(responseData))
      } else {
        NotFound
      }
    }
  }

}
