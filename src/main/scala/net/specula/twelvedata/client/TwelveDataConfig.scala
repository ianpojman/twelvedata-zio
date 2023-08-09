package net.specula.twelvedata.client

import zio.{Config, ConfigProvider, IO, ZLayer}

import java.io.File

/** Contains API key which is unique to each paying customer  */
case class TwelveDataConfig(apiKey: String)

object TwelveDataConfig:
  import zio.config._
  import zio.config.typesafe._
  import zio.config.magnolia._

  private val myConfigAutomatic = deriveConfig[TwelveDataConfig]

  val live = ZLayer.fromZIO(loadConfig)

  /** Default behavior is to look in ~/.twelvedata.conf, format of the file should look like this:
   * {{{
   *   apiKey: "XXX"
   * }}}
   * Of course, you will need to replace XXX with a valid Twelvedata API key */
  val loadConfig: IO[Config.Error, TwelveDataConfig] = {
    val p = ConfigProvider.fromHoconFile(new File(sys.props.getOrElse("user.home", "")+"/.twelvedata.conf"))
    p.load(myConfigAutomatic)
  }
end TwelveDataConfig
