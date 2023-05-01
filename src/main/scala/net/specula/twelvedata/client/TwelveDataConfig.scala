package net.specula.twelvedata.client

import zio.{Config, ConfigProvider, IO}

import java.io.File

case class TwelveDataConfig(apiKey: String)

object TwelveDataConfig:
  import zio.config._
  import zio.config.typesafe._
  import zio.config.magnolia._

  private val myConfigAutomatic = deriveConfig[TwelveDataConfig]

  val loadConfig: IO[Config.Error, TwelveDataConfig] = {
    val p = ConfigProvider.fromHoconFile(new File(sys.props.getOrElse("user.home", "")+"/.twelvedata.conf"))
    p.load(myConfigAutomatic)
  }
end TwelveDataConfig
