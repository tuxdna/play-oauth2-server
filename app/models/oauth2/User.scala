package models.oauth2;

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import java.util.Date
import java.sql.Timestamp
import oauth2.Crypto

case class User(id: Option[Int], username: String, email: String,
  password: String, role: String)

class Users(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
  def username = column[String]("username", O.NotNull)
  def email = column[String]("email", O.NotNull)
  def password = column[String]("password", O.NotNull)
  def role = column[String]("role", O.NotNull)
  def * = (id.?, username, email, password, role) <> (User.tupled, User.unapply _)
}

object Users {
  val users = TableQuery[Users]

  def get(id: Int)(implicit session: Session): Option[User] =
    users.where(_.id === id).firstOption

  def findByUsername(username: String)(implicit session: Session): Option[User] =
    users.where(_.username === username).firstOption

  /**
   * @param username Username to find
   * @param encryptedPassword Encrypted version of password
   * @param session Implicit database session
   * @return Option containing User.
   */
  def findByUsernameAndPassword(username: String, encryptedPassword: String)(implicit session: Session): Option[User] = {
    users.where(user =>
      user.username === username && user.password === encryptedPassword).firstOption
  }

  def autoInc = users returning users.map(_.id)

  /**
   * @param user User object with already encrypted password
   * @param session
   * @return
   */
  def insert(user: User)(implicit session: Session) = {
    val encUser = User(user.id, user.username, user.email, user.password, user.role)
    encUser.id match {
      case None => autoInc += encUser
      case Some(x) => users += encUser
    }
  }

  /**
   * @param id User id to be updated
   * @param user New User details
   * @param session Implicit database session
   * @return
   */
  def update(id: Int, user: User)(implicit session: Session) =
    users.where(_.id === user.id).update(user)

  /**
   * @param user User object to be deleted
   * @param session Implicit database session
   * @return
   */
  def delete(user: User)(implicit session: Session) =
    users.where(_.id === user.id).delete

  /**
   * Delete all the users. NOTE: Use with caution.
   * @param session Implicit database session
   * @return
   */
  def deleteAll()(implicit session: Session) = users.delete

}