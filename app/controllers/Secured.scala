package controllers

import play.api.mvc.Security
import play.api.mvc.RequestHeader
import play.api.mvc.Results
import play.api.mvc.Result
import play.api.mvc.Request
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

/**
 * Provide security features
 */
trait Secured {

  /**
   * Retrieve the connected user.
   */
  private def username(request: RequestHeader) = request.session.get("username")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = {

    // capture the original URL we want to be redirected to later on successful login
    val args: Seq[(String, String)] = if (request.method.equals("GET")) Seq("redirect_url" -> request.uri) else Seq()

    Results.Redirect(routes.Auth.login).flashing(args: _*)
  }

  // --

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  /**
   * This method shows how you could wrap the withAuth method to also fetch your user
   * You will need to implement UserDAO.findOneByUsername
   */
  def withUser(f: models.oauth2.User => Request[AnyContent] => Result) = withAuth { username =>
    implicit request =>
      import play.api.Play.current
      import play.api.db.slick.DB
      DB.withSession { implicit session =>
        models.oauth2.Users.findByUsername(username).map { user =>
          f(user)(request)
        }.getOrElse(onUnauthorized(request))
      }
  }

  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }

  //  /**
  //   * Check if the connected user is a member of this project.
  //   */
  //  def IsMemberOf(project: Long)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
  //    if(Project.isMember(project, user)) {
  //      f(user)(request)
  //    } else {
  //      Results.Forbidden
  //    }
  //  }
  //
  //  /**
  //   * Check if the connected user is a owner of this task.
  //   */
  //  def IsOwnerOf(task: Long)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
  //    if(Task.isOwner(task, user)) {
  //      f(user)(request)
  //    } else {
  //      Results.Forbidden
  //    }
  //  }

}