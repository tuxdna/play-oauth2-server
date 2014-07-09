package oauth2

import scalaoauth2.provider.{ AuthInfo, DataHandler }
import play.api.db.slick.DB
import play.api.Play.current
import java.util.Date
import java.sql.Timestamp
import models.oauth2._
import models.Helpers

class OAuthDataHandler extends DataHandler[User] {

  val log = play.Logger.of("application")

  def validateClient(clientId: String, clientSecret: String, grantType: String): Boolean = {
    log.debug(s"validateClient: $clientId $clientSecret ")
    DB.withTransaction { implicit session =>
      Clients.validate(clientId, clientSecret, grantType)
    }
  }

  def findUser(username: String, password: String): Option[User] = {
    log.debug("findUser")
    DB.withSession { implicit session =>
      val encodedPass = Helpers.encodePassword(password)
      Users.findByUsernameAndPassword(username, encodedPass)
    }
  }

  def createAccessToken(authInfo: AuthInfo[User]): scalaoauth2.provider.AccessToken = {
    log.debug("createAccessToken")
    DB.withSession { implicit session =>
      val accessTokenExpiresIn = 60 * 60 // 1 hour
      val now = new Date()
      val createdAt = new Timestamp(now.getTime)
      val refreshToken = Crypto.generateToken()
      val accessToken = Crypto.generateToken()
      val scope = authInfo.scope
      val uId = authInfo.user.id.get
      val clientId = authInfo.clientId
      val client = Clients.findByClientId(clientId)
      client match {
        case None => throw new UnsupportedOperationException
        case Some(c) =>
          val tokenObject = AccessToken(None, accessToken,
            refreshToken,
            c.id,
            uId,
            scope match { case None => "" case Some(s) => s },
            accessTokenExpiresIn,
            createdAt)

          log.debug(tokenObject.toString())
          AccessTokens.updateByUserAndClient(tokenObject, authInfo.user.id.get, c.id)

          scalaoauth2.provider.AccessToken(accessToken, Some(refreshToken),
            authInfo.scope, Some(accessTokenExpiresIn.toLong), now)
      }

    }
  }

  def getStoredAccessToken(authInfo: AuthInfo[User]): Option[scalaoauth2.provider.AccessToken] = {
    log.debug("getStoredAccessToken")
    DB.withSession { implicit session =>
      val clientId = authInfo.clientId
      val client = Clients.findByClientId(clientId)
      client match {
        case None => throw new UnsupportedOperationException
        case Some(c) =>
          val uid = authInfo.user.id
          AccessTokens.findByUserAndClient(uid.get, c.id) map { a =>
            scalaoauth2.provider.AccessToken(a.accessToken,
              Some(a.refreshToken), Some(a.scope),
              Some(a.expiresIn.toLong), a.createdAt)
          }
      }
    }
  }

  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): scalaoauth2.provider.AccessToken = {
    log.debug("refreshAccessToken")
    createAccessToken(authInfo)
  }

  def findAuthInfoByCode(code: String): Option[AuthInfo[User]] = {
    log.debug("findAuthInfoByCode: " + code)
    DB.withSession { implicit session =>
      AuthCodes.find(code) map { a =>
        log.debug("found!")
        val user = Users.get(a.userId).get
        AuthInfo(user, a.clientId, a.scope, a.redirectUri)
      }
    }
  }

  def findAuthInfoByRefreshToken(refreshToken: String): Option[AuthInfo[User]] = {
    log.debug("findAuthInfoByRefreshToken")
    DB.withSession { implicit session =>
      AccessTokens.findByRefreshToken(refreshToken) map { a =>
        val user = Users.get(a.userId).get
        val client = Clients.get(a.clientId).get
        AuthInfo(user, client.id, Some(a.scope), Some(""))
      }
    }
  }

  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]): Option[User] = {
    log.debug("findClientUser")
    None // TODO: ?
  }

  def findAccessToken(token: String): Option[scalaoauth2.provider.AccessToken] = {
    log.debug("findAccessToken")
    DB.withSession { implicit session =>
      AccessTokens.find(token) map { a =>
        val user = Users.get(a.userId).get
        val client = Clients.get(a.clientId).get
        scalaoauth2.provider.AccessToken(a.accessToken, Some(a.refreshToken),
          Some(a.scope), Some(a.expiresIn.toLong), a.createdAt)
      }
    }
  }

  def findAuthInfoByAccessToken(accessToken: scalaoauth2.provider.AccessToken): Option[AuthInfo[User]] = {
    log.debug("findAuthInfoByAccessToken")
    DB.withSession { implicit session =>
      AccessTokens.find(accessToken.token) map { a =>
        val user = Users.get(a.userId).get
        val client = Clients.get(a.clientId).get
        AuthInfo(user, client.id, Some(a.scope), Some(""))
      }
    }
  }

}
