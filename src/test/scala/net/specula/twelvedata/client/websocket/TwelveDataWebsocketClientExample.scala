package net.specula.twelvedata.client.websocket

import net.specula.twelvedata.client.model.PriceResponse
import net.specula.twelvedata.client.util.NetworkConfigurationUtil
import zio.{Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object TwelveDataWebsocketClientExample extends ZIOAppDefault {
  NetworkConfigurationUtil.tryDisableIPv6()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val pricehandler = ZLayer.succeed(new PriceHandler {
      override def accept(p: PriceResponse): Task[Unit] = zio.Console.printLine(s"[${p.symbol}] Received price ${p.price} ($p)")
    })
    val tickers = ZLayer.succeed(Tickers("AAPL,INFY,TRP,QQQ,IXIC,EUR/USD,USD/JPY,BTC/USD,ETH/BTC".split(",").toSet))
//    TwelveDataWebsocketClient.priceStreamingWebsocketApp
//      .provide(Scope.default, Layers.defaultLayers, pricehandler, tickers)
    ??? // TODO fixme
  }

}
