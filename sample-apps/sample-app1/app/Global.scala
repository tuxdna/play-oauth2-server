import java.io.File
import play.api._
import com.typesafe.config.ConfigFactory
import java.util.Date
import java.sql.Timestamp
import play.api.cache.Cache

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    super.onStart(app)
    implicit val currentApp = app

    // reset cache on startup
    val clientId = Play.current.configuration.getString("client_id").get
    val clientSecret = Play.current.configuration.getString("client_secret").get
    Cache.set("client_id", clientId)
    Cache.set("client_secret", clientSecret)
    Cache.set("auth_code", None)
    Cache.set("access_token", None)
    Cache.set("refresh_token", None)

    val apiServer = Play.current.configuration.getString("api_server").get
    Cache.set("api_server", apiServer)

    val oauth2Server = Play.current.configuration.getString("oauth2_server").get
    Cache.set("oauth2_server", oauth2Server)

    val oauth2TokenUrl = Play.current.configuration.getString("oauth2_token_url").get
    Cache.set("oauth2_token_url", oauth2TokenUrl)
    
  }
}
