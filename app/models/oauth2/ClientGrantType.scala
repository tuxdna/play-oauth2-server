package models.oauth2

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import java.util.Date
import java.sql.Timestamp
import oauth2.Crypto

case class ClientGrantType(clientId: String, grantTypeId: Int)

class ClientGrantTypes(tag: Tag) extends Table[ClientGrantType](tag, "client_grant_types") {
  def clientId = column[String]("client_id")
  def grantTypeId = column[Int]("grant_type_id")
  def * = (clientId, grantTypeId) <> (ClientGrantType.tupled, ClientGrantType.unapply _)
  val pk = primaryKey("pk_client_grant_type", (clientId, grantTypeId))
}

object ClientGrantTypes {
  val clientGrantTypes = TableQuery[ClientGrantTypes]

  def insert(cgt: ClientGrantType)(implicit session: Session) = {
    clientGrantTypes += cgt
  }
}

