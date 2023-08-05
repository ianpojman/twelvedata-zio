package net.specula.twelvedata.client

import net.specula.twelvedata.Price
import net.specula.twelvedata.client.model.Event
import net.specula.twelvedata.client.util.NetworkConfigurationUtil
import zio.{Queue, *}
import zio.http.ChannelEvent.{ChannelRead, ChannelRegistered, ExceptionCaught, UserEvent, UserEventTriggered, exceptionCaught}
import zio.http.socket.{SocketApp, WebSocketChannelEvent, WebSocketFrame}
import zio.http.*
import zio.http.ChannelEvent.UserEvent.HandshakeComplete
import zio.stream.ZStream

import scala.util.Right

trait PriceHandler {
  def accept(p: Price): Task[Unit]
}

case class Tickers(tickers: Set[String])

object TwelveDataWebsocketClient {

  import zio.json._
  import net.specula.twelvedata.client.model.EventCodecs._

  /** Opens a websocket with Twelvedata API and streams the configured prices */
  val priceStreamingWebsocketApp: ZIO[Tickers with PriceHandler with ApiQueryRequirements with Client with Scope, Throwable, Unit] = {
    (for {
      p <- zio.Promise.make[Nothing, Throwable]
      config <- ZIO.service[TwelveDataConfig]
      ph <- ZIO.service[PriceHandler]
      tickers <- ZIO.service[Tickers]
      url = s"wss://ws.twelvedata.com/v1/quotes/price?apikey=${config.apiKey}"
      q <- Queue.unbounded[Event]
      _ <- ZStream.fromQueue(q).foreach(e =>
        Price.fromEvent(e) match
          case Some(p) =>
            ZIO.attempt(println(s"Received event $e"))
            ph.accept(p)
          case None =>
            ZIO.attempt(println(s"Received event $e"))
      ).fork

      _ <- createStreamTickersWebsocket(p, q, tickers).toSocketApp.connect(url)
        .catchAll { t =>
          p.succeed(t) // convert a failed connection attempt to an error to trigger a reconnect
        }
      f <- p.await
//      _ <- consumeQueue.interrupt
      _ <- ZIO.logError(s"Connection failed: $f")
      _ <- ZIO.logError(s"Trying to reconnect...")
      _ <- ZIO.sleep(1.seconds)

    } yield {}) *> priceStreamingWebsocketApp
  }


  /**
   * @param p A promise is used to be able to notify application about websocket errors
   * @param q Queue to store messages on as we receive them
   * */
  def createStreamTickersWebsocket(p: Promise[Nothing, Throwable],
                                   q: Queue[Event],
                                   tickers: Tickers): Http[Any, Throwable, WebSocketChannelEvent, AnyVal] =
    Http.collectZIO[WebSocketChannelEvent] {
        case ChannelEvent(ch, ChannelRead(WebSocketFrame.Text(t))) =>
//          println("RAW EVENT: "+ t)

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
                 |	"symbols": "${tickers.tickers.mkString(",")}"
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