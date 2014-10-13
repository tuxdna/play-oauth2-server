package controllers

import scalaoauth2.provider.OAuth2Provider
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.RequestHeader
import play.api.mvc.Results
import play.api.mvc.Result
import play.api.mvc.Action
import play.api.mvc.Security
import play.api.mvc.SimpleResult
import scalaoauth2.provider.AuthInfo
import oauth2.OAuthDataHandler
import models.oauth2.User

trait SecuredOAuth extends Secured with OAuth2Provider {

  /**
   * Create an action which only succeeds if authorized according to OAuth2.0
   *
   * @param f Function which will be called on this action.
   * @return Action
   */
  def authorizedAction(f: (Request[AnyContent] => (AuthInfo[User] => Result))) = Action {
    implicit request =>
      authorize(new OAuthDataHandler()) { authInfo =>
        f(request)(authInfo)
      }
  }

  /**
   * Create an action that is only allowed for the scopes in allowedScopes set
   *
   * @param allowedScopes A Set of scope strings
   * @param f Function which will be called on this action.
   * @return Action
   */
  def authorizedScopedAction(allowedScopes: Set[String])(f: (Request[AnyContent] => (AuthInfo[User] => Result))) = Action {
    implicit request =>
      authorize(new OAuthDataHandler()) { authInfo =>
        authInfo.scope.filter(x => allowedScopes.contains(x)).map { sc =>
          f(request)(authInfo)
        }.getOrElse(Unauthorized)
      }
  }

  /**
   * Create an action that is only allowed for read scope
   *
   * @param f Function which will be called on this action.
   * @return Action
   */
  def authorizedReadAction(f: (Request[AnyContent] => (AuthInfo[User] => Result))) = authorizedScopedAction(Set("read", "readwrite"))(f)

  /**
   * Create an action that is only allowed for write scope
   *
   * @param f Function which will be called on this action.
   * @return Action
   */
  def authorizedWriteAction(f: (Request[AnyContent] => (AuthInfo[User] => Result))) = authorizedScopedAction(Set("readwrite"))(f)

  /**
   * Create an action that is only allowed for the given scope string
   *
   * @param f Function which will be called on this action.
   * @return Action
   */
  def authorizedAction(scope: String)(f: (Request[AnyContent] => (AuthInfo[User] => Result))) = authorizedScopedAction(Set(scope))(f)

}