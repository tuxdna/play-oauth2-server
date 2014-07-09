package controllers

import play.api._
import play.api.mvc._

object Application extends Controller with Secured {

  def index = withUser { user =>
    implicit request =>
      Ok(views.html.index("Welcome to OAuth2 Server running on Play! 2.0 Framework.", user))
  }

  def apis = withUser { user =>
    implicit request =>
      Ok(views.html.apis("API Listing", user))
  }

  def docs = withUser { user =>
    implicit request =>
      Ok(views.html.docs("API documentation", user))
  }

}

