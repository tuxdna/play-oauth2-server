package models.oauth2

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import java.util.Date
import java.sql.Timestamp
import oauth2.Crypto

case class GrantType(id: Option[Int], grantType: String)

class GrantTypes(tag: Tag) extends Table[GrantType](tag, "grant_types") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
  def grantType = column[String]("grant_type")
  def * = (id.?, grantType) <> (GrantType.tupled, GrantType.unapply _)
}

object GrantTypes {
  val grantTypes = TableQuery[GrantTypes]

  def autoInc = grantTypes returning grantTypes.map(_.id)

  /**
   * Insert Grant Type in databse table.
   * @param grantType
   * @param session
   * @return
   */
  def insert(grantType: GrantType)(implicit session: Session) = {
    grantType.id match {
      case Some(id) =>
        grantTypes += grantType
      case None =>
        autoInc += grantType
    }
  }

  /**
   * Update GrantType
   * @param id
   * @param grantType
   * @param session
   * @return
   */
  def update(id: Int, grantType: GrantType)(implicit session: Session) =
    grantTypes.where(_.id === grantType.id).update(grantType)
}
