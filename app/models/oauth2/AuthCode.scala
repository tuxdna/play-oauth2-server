package models.oauth2

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import java.util.Date
import java.sql.Timestamp
import oauth2.Crypto

case class AuthCode(authorizationCode: String, userId: Int,
  redirectUri: Option[String], createdAt: java.sql.Timestamp,
  scope: Option[String], clientId: String, expiresIn: Int)

class AuthCodes(tag: Tag) extends Table[AuthCode](tag, "auth_codes") {
  def authorizationCode = column[String]("authorization_code", O.PrimaryKey)
  def userId = column[Int]("user_id")
  def redirectUri = column[Option[String]]("redirect_uri")
  def createdAt = column[java.sql.Timestamp]("created_at")
  def scope = column[Option[String]]("scope")
  def clientId = column[String]("client_id")
  def expiresIn = column[Int]("expires_in")
  def * = (authorizationCode, userId, redirectUri, createdAt, scope,
    clientId, expiresIn) <>
    (AuthCode.tupled, AuthCode.unapply _)
}

object AuthCodes {
  val authCodes = TableQuery[AuthCodes]
  val log = play.Logger.of("application")

  /**
   * Add AuthCode object to database.
   * @param ac
   * @param session
   * @return
   */
  def insert(ac: AuthCode)(implicit session: Session) = {
    authCodes += ac
  }

  /**
   * Delete AuthCode object from database.
   * @param ac
   * @param session
   * @return
   */
  def delete(ac: AuthCode)(implicit session: Session) =
    authCodes.where(_.clientId === ac.clientId).delete

  /**
   * Find AuthCode object by its value.
   * @param authCode
   * @param session
   * @return
   */
  def find(authCode: String)(implicit session: Session) = {

    val code = authCodes.where(_.authorizationCode === authCode).firstOption

    log.debug(code.toString())
    // filtering out expired authorization codes
    code.filter { p =>
      val codeTime = p.createdAt.getTime + (p.expiresIn)
      val currentTime = new Date().getTime
      log.debug(s"codeTime: $codeTime, currentTime: $currentTime")
      codeTime > currentTime
    }
  }

  /**
   * Generate a new AuthCode for given client and other details.
   * @param clientId
   * @param redirectUri
   * @param scope
   * @param userId
   * @param expiresIn
   * @param session
   * @return
   */
  def generateAuthCodeForClient(clientId: String, redirectUri: String,
    scope: String, userId: Int, expiresIn: Int)(
      implicit session: Session): Option[AuthCode] = {

    Clients.findByClientId(clientId).map {
      client =>
        {
          val authCode = Crypto.generateAuthCode()
          val createdAt = new Timestamp(new Date().getTime)
          val redirectUriOpt = Some(redirectUri)
          val scopeOpt = Some(scope)
          val ac = AuthCode(authCode, userId, redirectUriOpt,
            createdAt, scopeOpt, clientId, expiresIn)

          // replace with new auth code
          delete(ac)
          insert(ac)
          ac
        }
    }
  }
}