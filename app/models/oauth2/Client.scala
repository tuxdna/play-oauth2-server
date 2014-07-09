package models.oauth2

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import java.util.Date
import java.sql.Timestamp
import oauth2.Crypto

case class Client(id: String, username: String, secret: String,
  description: String, redirectUri: String, scope: String)

class Clients(tag: Tag) extends Table[Client](tag, "clients") {
  def id = column[String]("client_id", O.PrimaryKey, O.NotNull)
  def username = column[String]("username", O.NotNull)
  def secret = column[String]("client_secret", O.NotNull)
  def description = column[String]("description", O.NotNull)
  def redirectUri = column[String]("redirect_uri", O.NotNull)
  def scope = column[String]("scope", O.NotNull)
  def * = (id, username, secret, description, redirectUri, scope) <>
    (Client.tupled, Client.unapply _)
}

object Clients {
  val grantTypes = TableQuery[GrantTypes]
  val clientGrantTypes = TableQuery[ClientGrantTypes]
  val clients = TableQuery[Clients]

  val grantTypeMapping = List(
    GrantType(Some(1), "authorization_code"),
    GrantType(Some(2), "client_credentials"),
    GrantType(Some(3), "password"),
    GrantType(Some(4), "refresh_token"))

  /**
   * Check if the given client and secret have a GrantType access configured in database.
   * @param clientId
   * @param clientSecret
   * @param grantType
   * @param session
   * @return
   */
  def validate(clientId: String, clientSecret: String, grantType: String)(implicit session: Session): Boolean = {
    val ccgt = clients innerJoin clientGrantTypes on ((c, cgt) => c.id === cgt.clientId)
    val ccgtgt = (ccgt) innerJoin grantTypes on (_._2.grantTypeId === _.id)
    val check = for {
      ((c, cgt), gt) <- ccgtgt
      if c.id === clientId && c.secret === clientSecret && gt.grantType === grantType
    } yield 0
    check.firstOption.isDefined
  }

  /**
   * Fetch all clients
   * @param session
   * @return
   */
  def list()(implicit session: Session) = { (for (c <- clients) yield c).list }

  /**
   * Fetch all Clients associated with the given username
   * @param username
   * @param session
   * @return
   */
  def findByUser(username: String)(implicit session: Session) = { (for (c <- clients.filter(x => x.username === username)) yield c).list }

  /**
   * Add a new Client to database.
   * @param client
   * @param session
   */
  def insert(client: Client)(implicit session: Session) = {
    clients += client
    updateGrantTypes(client)
  }

  /**
   * Update grant type for given client.
   * @param client
   * @param session
   */
  def updateGrantTypes(client: Client)(implicit session: Session) = {
    clientGrantTypes.where(_.clientId === client.id).delete
    val cgts = grantTypeMapping.map(gt => ClientGrantType(client.id, gt.id.get))
    cgts foreach { cgt => ClientGrantTypes.insert(cgt) }
  }

  /**
   * Delete the the given client from database.
   * @param client
   * @param session
   * @return
   */
  def delete(client: Client)(implicit session: Session) =
    clients.where(_.id === client.id).delete

  /**
   * Update the given client.
   * @param client
   * @param session
   */
  def update(client: Client)(implicit session: Session) = {
    clients.where(_.id === client.id).update(client)
    updateGrantTypes(client)
  }

  /**
   * Fetch a Client by ID
   * @param id
   * @param session
   * @return
   */
  def get(id: String)(implicit session: Session): Option[Client] =
    clients.where(_.id === id).firstOption

  /**
   * Fetch a Client by ID
   * @param clientId
   * @param session
   * @return
   */
  def findByClientId(clientId: String)(implicit session: Session): Option[Client] =
    clients.where(_.id === clientId).firstOption

  /**
   * Delete all clients from databse. NOTE: Use with caution.
   * @param session
   * @return
   */
  def deleteAll()(implicit session: Session) = clients.delete

}