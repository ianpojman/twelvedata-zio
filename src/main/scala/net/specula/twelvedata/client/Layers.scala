package net.specula.twelvedata.client

import zio.ZLayer
import zio.http.Client

object Layers {
  val defaultLayers =
    Client.default ++ ZLayer.fromZIO(TwelveDataConfig.loadConfig) >>> TwelveDataClient.live
}
