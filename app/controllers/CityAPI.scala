package controllers

import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import scalaoauth2.provider.OAuth2Provider
import oauth2.OAuthDataHandler
import play.api.db.slick.DBAction
import play.api.mvc.Result
import play.api.mvc.SimpleResult
import play.api.db.slick.DB
import play.api.Play.current

object CityAPI extends Controller with SecuredOAuth {

  def findById(id: Long) = DBAction { implicit rs =>
    val result: Result = authorize(new OAuthDataHandler()) { authInfo =>
      Ok
    }
    // TODO: This is a dirty way. Fix the DBAction result.
    result.asInstanceOf[SimpleResult]
  }

  // Another alternative is to directly use slick DB.withSession
  def findById1(id: Long) = authorizedReadAction { implicit request =>
    authInfo =>
      DB.withSession { implicit session =>
        // perform some read from database
        Ok
      }
  }
}
