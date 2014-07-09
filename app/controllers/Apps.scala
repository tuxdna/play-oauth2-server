package controllers

import play.api.mvc.{ Action, Controller }
import models.oauth2.Client
import play.api.db.slick.DBAction
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.db.slick.DB
import oauth2.OAuthDataHandler

case class AppAuthInfo(clientId: String, redirectUri: String,
  scope: String, state: String, accepted: String)

object Apps extends Controller with Secured {
  val log = play.Logger.of("application")

  val errorCodes = Map(
    "access_denied" -> "Access was denied",
    "invalid_request" -> "Request made was not valid",
    "unauthorized_client" -> "Client is not authorized to perform this action",
    "unsupported_response_type" -> "Response type requested is not allowed",
    "invalid_scope" -> "Requested scope is not allowed",
    "server_error" -> "Server encountered an error",
    "temporarily_unavailable" -> "Service is temporary unavailable")

  val AppAuthInfoForm = Form(mapping(
    "client_id" -> nonEmptyText,
    "redirect_uri" -> nonEmptyText,
    "scope" -> text,
    "state" -> text,
    "accepted" -> text)(AppAuthInfo.apply)(AppAuthInfo.unapply))

  def authorize = withUser { user =>
    implicit request =>
      // read URL parameters
      val params = List("client_id", "redirect_uri", "state", "scope")
      val data = params.map(k =>
        (k -> request.queryString.get(k).getOrElse(Seq("")).head)).toMap

      // check if such a client exists
      DB.withSession { implicit session =>
        val clientId = data("client_id")
        models.oauth2.Clients.findByClientId(clientId) match {
          case None => // doesn't exist
            BadRequest("No such client exists.")
          case Some(client) =>
            val aaInfoForm = AppAuthInfoForm.bind(data)

            log.debug(aaInfoForm.data.toString)
            data.keys.foreach { k =>
              log.debug(k)
              log.debug(aaInfoForm(k).value.toString)
            }
            Ok(views.html.apps.authorize(user, aaInfoForm))
        }
      }
  }

  def send_auth = withUser { user =>
    implicit request =>
      val boundForm = AppAuthInfoForm.bindFromRequest
      boundForm.fold(
        formWithErrors => {
          log.debug(formWithErrors.toString)
          Ok(views.html.apps.authorize(user, formWithErrors))
        },
        aaInfo => {
          aaInfo.accepted match {
            case "Y" =>
              val expiresIn = Int.MaxValue
              val acOpt =
                DB.withSession { implicit session =>
                  models.oauth2.AuthCodes.generateAuthCodeForClient(
                    aaInfo.clientId, aaInfo.redirectUri, aaInfo.scope,
                    user.id.get, expiresIn)
                }
              acOpt match {
                case Some(ac) =>
                  val authCode = ac.authorizationCode
                  val state = aaInfo.state
                  Redirect(s"${aaInfo.redirectUri}?code=${authCode}&state=${state}")
                case None =>
                  val errorCode = "server_error"
                  Redirect(s"${aaInfo.redirectUri}?error=${errorCode}")
              }

            case "N" =>
              val errorCode = "access_denied"
              Redirect(s"${aaInfo.redirectUri}?error=${errorCode}")
            case _ =>
              val errorCode = "invalid_request"
              Redirect(s"${aaInfo.redirectUri}?error=${errorCode}")
          }
        })
  }
}
