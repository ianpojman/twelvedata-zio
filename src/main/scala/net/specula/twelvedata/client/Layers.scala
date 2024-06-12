package net.specula.twelvedata.client

import zio.ZLayer
import zio.http.Client

object Layers {
  /** default ZIO layers that point to live Twelvedata service, use along with TwelveDataClient. */ 
  val defaultLayers: ZLayer[Any, Throwable, Client & TwelveDataConfig] =
    Client.default ++ ZLayer.fromZIO(TwelveDataConfig.loadConfig)
}
