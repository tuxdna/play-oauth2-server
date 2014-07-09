import java.io.File
import play.api._
import com.typesafe.config.ConfigFactory
import java.util.Date
import java.sql.Timestamp

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    super.onStart(app)
    val isLoadFixtures = app.configuration.getBoolean("fixtures.populateOnStartUp").getOrElse(false)
    println("fixtures enabled: " + isLoadFixtures)
    if (isLoadFixtures) {
      /// load sample data here
    }
  }

  override def onLoadConfig(config: Configuration,
    path: File, classloader: ClassLoader,
    mode: Mode.Mode): Configuration = {

    val configfile = s"application.${mode.toString.toLowerCase}.conf"
    println(s"Mode is ${mode.toString}, Loading: ${configfile}")
    val modeSpecificConfig = config ++ Configuration(
      ConfigFactory.load(configfile))
    super.onLoadConfig(modeSpecificConfig, path, classloader, mode)
  }

}
