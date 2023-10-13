package net.specula.twelvedata.client

import zio.{Config, ConfigProvider, IO, ZLayer}

import java.io.File

/** Contains API key which is unique to each paying customer */
case class TwelveDataConfig(apiKey: String)

object TwelveDataConfig:

  import zio.config._
  import zio.config.typesafe._
  import zio.config.magnolia._

  private val myConfigAutomatic: Config[TwelveDataConfig] = deriveConfig[TwelveDataConfig]

  /** Default behavior is to look in ~/.twelvedata.conf, format of the file should look like this:
   * {{{
   *   apiKey: "XXX"
   * }}}
   * Replace XXX with a valid Twelvedata API key */
  val loadConfig: IO[Config.Error, TwelveDataConfig] = {
    val fin = new File(sys.props.getOrElse("user.home", "") + "/.twelvedata.conf")
    val p = ConfigProvider.fromHoconFile(fin)
    p.load(myConfigAutomatic)
  }

  val live: ZLayer[Any, Config.Error, TwelveDataConfig] =
    ZLayer.fromZIO(loadConfig)

end TwelveDataConfig
