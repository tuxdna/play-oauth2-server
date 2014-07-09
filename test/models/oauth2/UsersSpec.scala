package models.oauth2

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models.Helpers

@RunWith(classOf[JUnitRunner])
class UsersSpec extends Specification {

  import play.api.db.slick.DB

  "User" should {
    "be creatible, updatible and deletible" in new WithApplication {
      DB.withSession { implicit session =>
        Users.deleteAll()
        
        val pass1 = Helpers.encodePassword("password1")
        val user1 = User(None, "user1", "user1@localhost", pass1, "all")

        val pass2 = Helpers.encodePassword("password2")
        val user1pass2 = User(None, "user1", "user1@localhost", pass2, "all")
        val user1Id = Users.insert(user1)
        val rv2 = Users.update(user1Id, user1pass2)
        val rv3 = Users.delete(user1)
      }
    }

    "be searchable" in new WithApplication {
      DB.withSession { implicit session =>
        Users.deleteAll()
        val pass = Helpers.encodePassword("pass13")
        val user = User(None, "user13","user1@localhost", pass, "all")
        Users.insert(user)
        val u = Users.findByUsername(user.username)
        u.get.username must equalTo(user.username)
      }
    }

    "be validated" in new WithApplication {
      DB.withSession { implicit session =>
        Users.deleteAll()
        val plainPass = "pass14"
        val encodePass = Helpers.encodePassword(plainPass)
        val user = User(None, "user14","user1@localhost", encodePass, "all")
        Users.insert(user)
        val u = Users.findByUsernameAndPassword(user.username, encodePass)
        u.get.username must equalTo(user.username)
        u.get.password must equalTo(user.password)
        Users.deleteAll()
      }
    }
  }

}
