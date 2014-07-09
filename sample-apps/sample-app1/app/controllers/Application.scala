package controllers

import play.api._
import play.api.mvc._
import java.net.URLEncoder
import play.api.libs.ws.WS
import scala.concurrent.Future
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import play.api.libs.concurrent.Execution.Implicits._
import scala.collection.JavaConversions._
import play.api.cache.Cache
import play.api.Play.current

object Application extends Controller {
  val log = play.Logger.of("application")

  val errorCodes = Map(
    "access_denied" -> "Access was denied",
    "invalid_request" -> "Request made was not valid",
    "unauthorized_client" -> "Client is not authorized to perform this action",
    "unsupported_response_type" -> "Response type requested is not allowed",
    "invalid_scope" -> "Requested scope is not allowed",
    "server_error" -> "Server encountered an error",
    "temporarily_unavailable" -> "Service is temporary unavailable")

  /**
   * Generates a Base URL based on the the hostname in request
   * @param request
   * @return Base HTTPS URL String
   */
  def myBaseUrl(implicit request: play.api.mvc.Request[play.api.mvc.AnyContent]) = {
    s"https://${request.host}/"
  }

  def index = Action { implicit request =>
    log.debug("Normal flow")
    Ok(views.html.index("Sample Client App"))
  }

  def sampleAPIStatus(elemId: String) = Action.async { implicit request =>
    log.debug("Called sampleAPIStatus")

    val apiServer = Cache.getAs[String]("api_server").get
    val apiUrl = s"${apiServer}sampleapi/status/${elemId}"

    Cache.getAs[String]("access_token") match {
      case None => // generate a new access token
        log.debug("No access token was found in cache")
        Future(Redirect(routes.Application.redirect))
      case Some("") =>
        log.debug("Access token is empty")
        Future(Redirect(routes.Application.redirect))
      case Some(accessToken) =>

        log.debug(s"Using access token: $accessToken")
        val authHeader = ("Authorization", s"Bearer ${accessToken}")

        log.debug(s"Requesting ${apiUrl} using authHeader: ${authHeader}")
        val rs: Future[play.api.libs.ws.Response] =
          WS.url(apiUrl).withHeaders(authHeader).get()

        log.debug(apiUrl)
        log.debug(rs.toString())

        val result = rs.map { response =>
          val ahcr = response.getAHCResponse
          val statusCode = ahcr.getStatusCode()
          val statusText = ahcr.getStatusText()

          val res = statusCode match {
            case 200 => // 200 OK
              // we got the response back
              Ok(response.body.toString())
            case statusCode @ _ =>

              val wwwAuthHeaders = ahcr.getHeaders("WWW-Authenticate").toList
              val authHeader = wwwAuthHeaders.mkString

              val bearerError = """Bearer error="(.*)", error_description="(.*)"""".r
              log.debug(s"response: ${statusCode} - ${statusText}")

              statusCode match {
                // Unknown respose type
                case 400 => // 400 Bad Request
                  // we can't make such a request
                  log.debug("You made a bad request")
                  authHeader match {
                    case bearerError(e, desc) =>
                      log.debug("EEE: " + (e, desc).toString)
                      BadRequest(s"$e - $desc")
                    case _ => BadRequest(authHeader)
                  }

                case 401 => // 401 Unauthorized
                  // the access token is not valid
                  log.debug("You made an unauthorized request. Get an valid token.")
                  val errors = authHeader match {
                    case bearerError(e, desc) =>
                      log.debug("EEE: " + (e, desc).toString)
                      s"$e - $desc"
                    case _ => authHeader
                  }
                  log.debug("Redirecting for token request")
                  Redirect(routes.Application.redirect)
                case _ =>
                  val msg = s"Don't know what to do with this response: ${statusCode} - ${statusText}"
                  log.debug(msg)
                  BadRequest(msg)
              }

          }
          res
        }

        result
    }

  }

  def showUnauthorized = Action { implicit request =>
    Ok(views.html.index("Your new application is ready."))
  }

  def redirect = Action { implicit request =>

    val srvUrl = Cache.getAs[String]("oauth2_server")
    srvUrl match {
      case None => Ok("Error with configuration")
      case Some(resourceServerUri) => {
        // val myUri = "https://cc.hcpci.com/"
        val redirectUri = myBaseUrl + "oauth2callback"

        val clientId = Cache.getAs[String]("client_id").getOrElse("")

        // which user?
        val params = Map(
          "client_id" -> clientId,
          "redirect_uri" -> redirectUri,
          "scope" -> "all",
          "response_type" -> "code")

        val s = params.map { x => s"${x._1}=${URLEncoder.encode(x._2, "UTF-8")}" }.mkString("&")
        val baseUrl = s"${resourceServerUri}apps/authorize/"
        val url = List(baseUrl, s).mkString("?")
        Redirect(url)
      }
    }

  }

  def oauth2callback = Action.async { implicit request =>
    val mp = request.queryString.map { case (k, v) => k -> v.mkString }
    log.debug("oauth2callback")

    mp.get("error") match {
      case Some(error) =>
        Future(BadRequest(s"Error: ${error} -- ${errorCodes(error)}"))
      case None => // No problem found

        mp.get("code") match {
          case None =>
            Future(Ok("""
            Access code wasn't received. 
            Some problem with the workflow? 
            Perhaps error codes need to be processed"""))

          case Some(authCode) =>

            // here we store the authcode
            Cache.set("auth_code", authCode)
            log.debug("Received Auth Code")

            // Now exchange the auth_code and other credentials for access token
            log.debug("Exchange credentials and authcode for token")
            /*
        // wget -d -q -O -  
        //   --post-data "grant_type=authorization_code&
        //                client_id=client1&
        //                client_secret=secret1&
        //                code=authcode1&
        //                redirect_uri=http://localhost:9001/"
        //   http://localhost:9002/oauth2/access_token
        */
            val oauth2TokenUrl = Cache.getAs[String]("oauth2_token_url").get
            val clientId = Cache.getAs[String]("client_id").getOrElse("")
            val clientSecret = Cache.getAs[String]("client_secret").getOrElse("")
            val x = routes.Application.oauth2callback.toString
            log.debug(x)
            val redirectUri = myBaseUrl + "oauth2callback";
            val params = Map(
              "grant_type" -> "authorization_code",
              "client_id" -> clientId,
              "client_secret" -> clientSecret,
              "code" -> authCode,
              "redirect_uri" -> redirectUri)

            val postData = params map (x => (x._1 -> Seq(x._2)))
            log.debug(postData.toString)

            val authTokenResponse =
              WS.url(oauth2TokenUrl).post((postData))

            val reply = authTokenResponse.map { response =>
              val ahcr = response.getAHCResponse

              val sampleResponse = """
              {
                  "access_token": "ZjlmZTE5OGEtNDM5Yi00ODczLWIxYzEtOTk5M2RhNmU5MTIy",
                  "expires_in": 3600,
                  "refresh_token": "OTY0MzU5NmMtNTlkOC00ZmVhLTg4OTctZjYyYzk0MDU2ZGMz",
                  "scope": "",
                  "token_type": "Bearer"
              }
          """

              ahcr.getStatusCode() match {
                case 200 =>
                  // we are good to go
                  log.debug("200 response")

                  val json = response.json
                  val authToken = (json \ "access_token").as[String]
                  val refreshToken = (json \ "refresh_token").as[String]
                  val expiresIn = (json \ "expires_in").as[Int]
                  val scope = (json \ "scope").as[String]
                  val tokenType = (json \ "token_type").as[String]

                  log.debug((authToken, refreshToken, expiresIn, scope, tokenType).toString)
                  val cachekeys = List(
                    "access_token",
                    "refresh_token",
                    "expires_in",
                    "scope",
                    "token_type")

                  // cachekeys foreach (k => Cache.set(k, json \ k))
                  Cache.set("access_token", authToken)
                  Cache.set("refresh_token", refreshToken)
                  Cache.set("expires_in", expiresIn)
                  Cache.set("scope", scope)
                  Cache.set("token_type", tokenType)

                  cachekeys map (k => k -> Cache.get(k)) foreach println
                  Ok(response.json)
                case k @ _ =>
                  log.debug(s"$k response ${ahcr.getStatusText()}")

                  // we screwed up with either auth token or some other issue
                  BadRequest("")
              }
            }

            // begin token exchange
            // redirect back to the original URL

            reply
        }
    }

  }

}

