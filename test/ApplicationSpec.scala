import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models.oauth2.User
import models.oauth2.Users

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the login page" in new WithApplication {
      val home = route(FakeRequest(GET, "/")).get
      // must redirect to login page
      status(home) must equalTo(SEE_OTHER)
      redirectLocation(home).map { location =>
        location must equalTo("/login")
        val loginpage = route(FakeRequest(GET, "/login")) map { result =>
          status(result) must equalTo(OK)
          contentType(result) must beSome.which(_ == "text/html")
          contentAsString(result) must contain("Apps: Login")
        }
      }
    }

    "render the homepage after login" in new WithApplication {
      val loginpage = route(FakeRequest(GET, "/login")) map { result =>
        status(result) must equalTo(OK)
        contentType(result) must beSome.which(_ == "text/html")
        contentAsString(result) must contain("Apps: Login")

        // ensure that we have a user configured

        import play.api.db.slick.DB
        DB.withSession { implicit session =>
          val plainPass = "pass14"
          val encodePass = models.Helpers.encodePassword(plainPass)
          val user = User(None, "user14", "user1@localhost", encodePass, "all")
          Users.insert(user)
          val u = models.oauth2.Users.findByUsernameAndPassword("user14", encodePass)
          u must beSome.which(x => x.username == "user14")
          u must beSome.which(x => x.password == encodePass)
        }

        val resOpt = route(FakeRequest(POST, "/authenticate").withFormUrlEncodedBody(
          ("username", "user14"), ("password", "pass14"),
          ("redirect_url", "/")))
        resOpt match {
          case None => throw new Exception("Login failed")
          case Some(res) =>
            status(res) must equalTo(SEE_OTHER)
            val cookiz = cookies(res)
            redirectLocation(res).map { location =>
              location must equalTo("/")

              val hpOpt = route(FakeRequest(GET, "/").withCookies(cookiz.toList: _*))
              val homepage = hpOpt map { result =>
                status(result) must equalTo(OK)
                contentType(result) must beSome.which(_ == "text/html")
                contentAsString(result) must contain("Welcome to Apps")
              }
            }
        }
      }
    }
  }
}
