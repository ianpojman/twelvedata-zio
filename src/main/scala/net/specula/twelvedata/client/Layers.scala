package net.specula.twelvedata.client

import zio.ZLayer
import zio.http.Client

object Layers {
  /** default ZIO layers that point to live Twelvedata service, use along with TwelveDataClient. */ 
  val defaultLayers =
    Client.default ++ ZLayer.fromZIO(TwelveDataConfig.loadConfig) >>> TwelveDataClient.live // >>> will provide the preceding layers to the target layer, then generate a layer all 3 of them in the output
}
