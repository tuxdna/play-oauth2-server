package models.oauth2

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import java.util.Date
import java.sql.Timestamp
import oauth2.Crypto

case class AccessToken(id: Option[Int], accessToken: String,
  refreshToken: String, clientId: String, userId: Int, scope: String,
  expiresIn: Long, createdAt: java.sql.Timestamp)

class AccessTokens(tag: Tag) extends Table[AccessToken](tag, "access_tokens") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
  def accessToken = column[String]("token", O.NotNull)
  def refreshToken = column[String]("refresh_token", O.NotNull)
  def clientId = column[String]("client_id", O.NotNull)
  def userId = column[Int]("user_id")
  def scope = column[String]("scope", O.NotNull)
  def expiresIn = column[Long]("expires_in", O.NotNull)
  def createdAt = column[java.sql.Timestamp]("created_at", O.NotNull)
  def * = (id.?, accessToken, refreshToken,
    clientId, userId, scope, expiresIn, createdAt) <>
    (AccessToken.tupled, AccessToken.unapply _)
}

object AccessTokens {
  val accessTokens = TableQuery[AccessTokens]

  /**
   * Fetch AccessToken by its ID.
   * @param id
   * @param session
   * @return
   */
  def get(id: Int)(implicit session: Session): Option[AccessToken] =
    accessTokens.where(_.id === id).firstOption

  /**
   * Find AccessToken by token value
   * @param accessToken
   * @param session
   * @return
   */
  def find(accessToken: String)(implicit session: Session): Option[AccessToken] =
    accessTokens.where(_.accessToken === accessToken).firstOption

  /**
   * Find AccessToken by User and Client
   * @param userId
   * @param clientId
   * @param session
   * @return
   */
  def findByUserAndClient(userId: Int, clientId: String)(implicit session: Session): Option[AccessToken] =
    accessTokens.where(a => a.userId === userId && a.clientId === clientId).firstOption

  /**
   * Find Refresh Token by its value
   * @param refreshToken
   * @param session
   * @return
   */
  def findByRefreshToken(refreshToken: String)(implicit session: Session): Option[AccessToken] =
    accessTokens.where(_.refreshToken === refreshToken).firstOption

  /**
   * Add a new AccessToken
   * @param token
   * @param session
   * @return
   */
  def insert(token: AccessToken)(implicit session: Session) = {
    token.id match {
      case None => (accessTokens returning accessTokens.map(_.id)) += token
      case Some(x) => accessTokens += token
    }
  }

  /**
   * Update existing AccessToken associated with a user and a client.
   * @param accessToken
   * @param userId
   * @param clientId
   * @param session
   * @return
   */
  def updateByUserAndClient(accessToken: AccessToken, userId: Int, clientId: String)(implicit session: Session) = {
    session.withTransaction {
      accessTokens.where(a => a.clientId === clientId && a.userId === userId).delete
      accessTokens.insert(accessToken)
    }
  }

  /**
   * Update AccessToken object based for the ID in accessToken object
   * @param accessToken
   * @param session
   * @return
   */
  def update(accessToken: AccessToken)(implicit session: Session) = {
    accessTokens.where(_.id === accessToken.id).update(accessToken)
  }

}

