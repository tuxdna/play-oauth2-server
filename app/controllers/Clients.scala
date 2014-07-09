package controllers

import play.api.mvc.{ Action, Controller }
import models.oauth2.Client
import play.api.db.slick.DBAction
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.db.slick.DB
import java.util.UUID
import oauth2.Crypto

case class ClientDetails(id: String, secret: String, description: String, redirectUri: String, scope: String)

object Clients extends Controller with Secured {
  val log = play.Logger.of("application")

  val clientForm = Form(mapping(
    "id" -> nonEmptyText,
    "secret" -> nonEmptyText,
    "description" -> nonEmptyText,
    "redirectUri" -> nonEmptyText,
    "scope" -> text)(ClientDetails.apply)(ClientDetails.unapply))

  def list = withUser { user =>
    implicit request =>
      log.debug("Clients.list " + request.method)

      val allClients = DB.withSession { implicit session =>
        val clientsFromDb = models.oauth2.Clients.findByUser(user.username)
        println(clientsFromDb)
        clientsFromDb
      }

      Ok(views.html.clients.list(allClients, user))
  }

  def create() = withUser { user =>
    implicit request =>
      log.debug("Clients.newClient " + request.method)

      // generate unique and random values for id and secret
      val clientId = Crypto.generateUUID
      val clientSecret = Crypto.generateUUID
      val boundForm = clientForm.bind(Map("id" -> clientId, "secret" -> clientSecret))

      Ok(views.html.clients.new_client(boundForm, user))
  }

  def edit(id: String) = withUser { user =>
    implicit request =>
      log.debug("Clients.newClient " + request.method)

      DB.withSession { implicit session =>
        val clientOpt = models.oauth2.Clients.get(id)
        clientOpt match {
          case None => NotFound
          case Some(client) =>
            val boundForm = clientForm.bind(Map("id" -> client.id, "secret" -> client.secret,
              "description" -> client.description, "redirectUri" -> client.redirectUri,
              "scope" -> client.scope))

            Ok(views.html.clients.edit_client(boundForm, user))
        }
      }
  }

  def get(id: String) = withUser { user =>
    implicit request =>
      log.debug("Clients.get " + request.method)
      DB.withSession { implicit session =>
        val clientOpt = models.oauth2.Clients.get(id)
        clientOpt match {
          case None => NotFound
          case Some(client) =>
            Ok(views.html.clients.show_client(client, user))
        }
      }
  }

  def delete(id: String) = withUser { user =>
    implicit request =>
      log.debug("Clients.delete " + request.method)
      NotImplemented
  }

  def add = withUser { user =>
    implicit request =>
      log.debug("Clients.add " + request.method)
      request.method match {
        case "POST" =>
          log.debug(request.body.toString)

          val boundForm = clientForm.bindFromRequest
          boundForm.fold(

            // validate rules
            // form has errors
            formWithErrors => {
              log.debug("Form has errors")
              log.debug(formWithErrors.errors.toString)
              Ok(views.html.clients.new_client(formWithErrors, user)).flashing(
                "error" -> "Form has errors. Please enter correct values.")
            },

            clientDetails => {
              // check for duplicate
              log.debug(clientDetails.toString)
              DB.withSession { implicit session =>
                models.oauth2.Clients.get(clientDetails.id) match {
                  case None =>
                    // save
                    log.debug("Saving new client")
                    val client = models.oauth2.Client(clientDetails.id, user.username, clientDetails.secret,
                      clientDetails.description, clientDetails.redirectUri, clientDetails.scope)
                    models.oauth2.Clients.insert(client)
                    // redirect to list
                    log.debug("redirecting to client list")
                    Redirect(routes.Clients.list)
                  case Some(c) => // duplicate
                    log.debug("Duplicate client entry")
                    val flash = play.api.mvc.Flash(Map("error" -> "Please select another id for this client"))
                    Ok(views.html.clients.new_client(boundForm, user)(flash))

                }
              }
            })

        case "PUT" =>
          NotImplemented
      }
  }

  def update = withUser { user =>
    implicit request =>

      log.debug("Clients.update()")
      val boundForm = clientForm.bindFromRequest
      boundForm.fold(

        formWithErrors => { // validate rules
          log.debug("Form has errors")
          log.debug(formWithErrors.errors.toString)
          Ok(views.html.clients.new_client(formWithErrors, user)).flashing(
            "error" -> "Form has errors. Please enter correct values.")
        },

        clientDetails => {
          val cd = clientDetails
          log.debug("Client details")
          // log.debug((cd.id, cd.secret, cd.redirectUi, cd.scope, cd.description).toString)
          DB.withSession { implicit session =>

            // log.debug("Searching for client: " + cd.id)
            models.oauth2.Clients.get(cd.id) match {
              case Some(c) => // existing client
                log.debug("Saving new client")

                val client = models.oauth2.Client(
                  cd.id, user.username, cd.secret,
                  cd.description, cd.redirectUri, cd.scope)

                models.oauth2.Clients.update(client) // save
                log.debug("redirecting to client list")
                Redirect(routes.Clients.list) // redirect to client listing
              case None => // does not exist
                log.debug("No such client")
                NotFound
            }
          }
        })
  }

}
