package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.Helpers

object Auth extends Controller {
  val log = play.Logger.of("application")

  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text,
      "redirect_url" -> text) verifying ("Invalid email or password", result => result match {
        case (username, password, _) => check(username, password)
      }))

  def check(username: String, password: String) = {
    import play.api.Play.current
    import play.api.db.slick.DB

    DB.withSession { implicit session =>
      val encodedPass = Helpers.encodePassword(password)
      val u = models.oauth2.Users.findByUsernameAndPassword(username, encodedPass)
      u match {
        case None => false
        case Some(user) => true
      }
    }
  }

  def login = Action { implicit request =>
    val boundForm = loginForm.bind(Map("redirect_url" -> request.flash.get("redirect_url").getOrElse("/")))
    Ok(views.html.login(boundForm))
  }

  def authenticate = Action { implicit request =>
    val boundForm = loginForm.bindFromRequest

    boundForm.fold(
      formWithErrors => {
        log.debug("Form has errors: Invalid username or password")
        BadRequest(views.html.login(formWithErrors))
      },

      user => {
        val (username, password, redirectUrl) = user
        log.debug("Logged in !")

        (redirectUrl match {
          case "/" =>
            Redirect(routes.Application.index)
          case _ =>
            Redirect(redirectUrl)
        }).withSession(Security.username -> username)

      })
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You are now logged out.")
  }
}

