package net.specula.twelvedata.client

import net.specula.twelvedata.Price
import net.specula.twelvedata.client.model.Event
import net.specula.twelvedata.client.util.NetworkConfigurationUtil
import zio.*
import zio.http.ChannelEvent.{ChannelRead, ChannelRegistered, ExceptionCaught, UserEvent, UserEventTriggered}
import zio.http.socket.{SocketApp, WebSocketChannelEvent, WebSocketFrame}
import zio.http.*
import zio.http.ChannelEvent.UserEvent.HandshakeComplete
import zio.stream.ZStream

import scala.util.Right


object TwelveDataWebsocketClientExample extends ZIOAppDefault {
  NetworkConfigurationUtil.tryDisableIPv6()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    TwelveDataWebsocketClient.app
      .provide(Scope.default, Layers.defaultLayers)

}

object TwelveDataWebsocketClient {

  import zio.json._
  import net.specula.twelvedata.client.model.EventCodecs._

  // A promise is used to be able to notify application about websocket errors
  def makeHttpSocket(p: Promise[Nothing, Throwable],
                     q: Queue[Event]): Http[Any, Throwable, WebSocketChannelEvent, AnyVal] =
    Http.collectZIO[WebSocketChannelEvent] {
        case ChannelEvent(ch, ChannelRead(WebSocketFrame.Text(t))) =>
          val maybeE = t.fromJson[Event]
          maybeE match {
            case Left(e) => ZIO.logWarning(s"Error receiving event: $e")
            case Right(value) => q.offer(value)
          }

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


  class PriceMonitor {
    def accept(e: Event): Task[Unit] = {
      Price.fromEvent(e) match
        case Some(value) =>
          ZIO.attempt(println(s"Received price $value"))
        case None =>
          ZIO.attempt(println(s"Received event $e"))
    }
  }
  object PriceMonitor {
    val create: Task[PriceMonitor] = ZIO.attempt(new PriceMonitor)
  }

  val app: ZIO[ApiQueryRequirements with Client with Scope, Throwable, Unit] = {
    (for {
      p <- zio.Promise.make[Nothing, Throwable]
      config <- ZIO.service[TwelveDataConfig]
      url = s"wss://ws.twelvedata.com/v1/quotes/price?apikey=${config.apiKey}"
      q <- Queue.unbounded[Event]
      s  <- PriceMonitor.create
      consumeQueue <- ZStream.fromQueue(q).foreach(e =>
        s.accept(e)
      ).fork

      r <- makeHttpSocket(p, q).toSocketApp.connect(url)
        .catchAll { t =>
          p.succeed(t) // convert a failed connection attempt to an error to trigger a reconnect
        }
      f <- p.await
      _ <- consumeQueue.interrupt
      _ <- ZIO.logError(s"App failed: $f")
      _ <- ZIO.logError(s"Trying to reconnect...")
      _ <- ZIO.sleep(1.seconds)

    } yield {
      ()
    }) *> app
  }


}

/*
//
// @author LazyBear
// List of all my indicators: https://www.tradingview.com/v/4IneGo8h/
//
study("Z distance from VWAP [LazyBear]", shorttitle="ZVWAP_LB")
length=input(20)

calc_zvwap(pds) =>
  mean = sum(volume*close,pds)/sum(volume,pds)
  vwapsd = sqrt(sma(pow(close-mean, 2), pds) )
  (close-mean)/vwapsd

plot(0)
upperTop=input(2.5)
upperBottom=input(2.0)
lowerTop=input(-2.5)
lowerBottom=input(-2.0)

plot(1, style=3, color=gray), plot(-1, style=3, color=gray)
ul1=plot(upperTop, "OB High")
ul2=plot(upperBottom, "OB Low")
fill(ul1,ul2, color=red)
ll1=plot(lowerTop, "OS High")
ll2=plot(lowerBottom, "OS Low")
fill(ll1,ll2, color=green)
plot(calc_zvwap(length),title="ZVWAP",color=maroon, linewidth=2)

*/