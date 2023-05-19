package net.specula.twelvedata.client

import net.specula.twelvedata.client.model.Event
import net.specula.twelvedata.client.util.NetworkConfigurationUtil
import zio.*
import zio.http.ChannelEvent.{ChannelRead, ChannelRegistered, ExceptionCaught, UserEvent, UserEventTriggered}
import zio.http.socket.{SocketApp, WebSocketChannelEvent, WebSocketFrame}
import zio.http.*
import zio.http.ChannelEvent.UserEvent.HandshakeComplete


object TwelveDataWebsocketClientExample extends ZIOAppDefault {
  NetworkConfigurationUtil.tryDisableIPv6()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    TwelveDataWebsocketClient.app
      .provide(Scope.default, Layers.defaultLayers)

}

object TwelveDataWebsocketClient {

  // A promise is used to be able to notify application about websocket errors
  def makeHttpSocket(p: Promise[Nothing, Throwable]): Http[Any, Throwable, WebSocketChannelEvent, Unit] =
  // Listen for all websocket channel events
    Http.collectZIO[WebSocketChannelEvent] {
      case ChannelEvent(ch, ChannelRead(WebSocketFrame.Text(t))) =>
        import zio.json._
        import net.specula.twelvedata.client.model.EventCodecs._
        val e = t.fromJson[Event]
        ZIO.logInfo(s"Received event: ${e}")

      case ChannelEvent(ch, UserEventTriggered(HandshakeComplete)) =>
        zio.Console.printLine("Subscribing...") *>
        ch.writeAndFlush(WebSocketFrame.text(
          s"""
             |{
             |  "action": "subscribe",
             |  "params": {
             |	"symbols": "AAPL,INFY,TRP,QQQ,IXIC,EUR/USD,USD/JPY,BTC/USD,ETH/BTC"
             |  }
             |}
             |
             |""".stripMargin))

      // Handle exception and convert it to failure to signal the shutdown of the socket connection via the promise
      case ChannelEvent(_, ExceptionCaught(t)) =>
        ZIO.fail(t)

      case other =>
        ZIO.logInfo(s"Received other: ${other}")
    }
      .tapErrorZIO { f =>
        // signal failure to application
        p.succeed(f)
      }


  val app: ZIO[ApiQueryRequirements with Client with Scope, Throwable, Unit] = {
    (for {
      p <- zio.Promise.make[Nothing, Throwable]
      config <- ZIO.service[TwelveDataConfig]
      url = s"wss://ws.twelvedata.com/v1/quotes/price?apikey=${config.apiKey}"

      _ <- makeHttpSocket(p).toSocketApp.connect(url).catchAll { t =>
        // convert a failed connection attempt to an error to trigger a reconnect
        p.succeed(t)
      }
      f <- p.await
      _ <- ZIO.logError(s"App failed: $f")
      _ <- ZIO.logError(s"Trying to reconnect...")
      _ <- ZIO.sleep(1.seconds)
    } yield {
      ()
    }) *> app
  }


}
