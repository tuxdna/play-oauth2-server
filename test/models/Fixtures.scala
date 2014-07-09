package models

import play.api._
import java.util.Date
import java.sql.Timestamp
import play.api.Play.current
import play.api.db.slick.DB

object Fixtures {
  import play.api.Play.current
  import play.api.db.slick.DB

  def populate: Unit = {
    DB.withSession { implicit session =>
      import models.oauth2._

      val user1 = User(None, "user1", "user1@localhost", "pass1", "")

      // Skip the fixtures if user already exists
      if (Users.findByUsername(user1.username).isDefined) return ;

      // add a user
      val user1Id = Users.insert(user1)

      // add some clients
      val client1Callback = "https://client.com/oauth2callback"
      val client1 = Client("client1", user1.username, "secret1", "this is client1", client1Callback, "all")
      val client2 = Client("client2", user1.username, "secret2", "this is client2", "http://localhost/9001", "")
      val client3 = Client("client3", user1.username, "secret3", "this is client3", "http://localhost/9001", "")
      val client4 = Client("client4", user1.username, "secret4", "this is client4", "http://localhost/9001", "read,write,update")
      val clients = List(client1, client2, client3, client4)

      clients foreach (c => Clients.insert(c))

      // insert grant_types in grant_type configuration table
      val grantTypes = List(
        GrantType(Some(1), "authorization_code"),
        GrantType(Some(2), "client_credentials"),
        GrantType(Some(3), "password"),
        GrantType(Some(4), "refresh_token"))

      grantTypes.foreach(gt => GrantTypes.insert(gt))

      // associate some grant_types to client1

      val cgts = grantTypes.map(gt => ClientGrantType(client1.id, gt.id.get))
      cgts foreach { cgt => ClientGrantTypes.insert(cgt) }

      // generate a sample auth code for client1
      // ('authcode1', 1, 'http://localhost:9001/', 'all', 'client1', 3600);

      val now = new Date()
      val createdAt = new Timestamp(now.getTime)
      val ac = AuthCode("authcode1", user1Id,
        Some(client1Callback), createdAt, Some("all"), client1.id, 36000)
      AuthCodes.insert(ac)
    }
  }
}