package net.specula.twelvedata.client.websocket

import net.specula.twelvedata.client.TwelveDataClient
import net.specula.twelvedata.client.model.{Event, PriceResponse}
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.HandshakeComplete
import zio.stream.ZStream

import scala.util.Right

trait PriceHandler {
  def accept(p: PriceResponse): Task[Unit]
}

case class WebsocketSession(url: String,
                            tickers: Set[String],
                            queue: Queue[Event]) {

  /** stream raw events from Twelvedata */
  def stream(): ZStream[Any, Throwable, Event] = ZStream.fromQueue(queue)

  /** stream only the price data */
  def streamPrices(): ZStream[Any, Throwable, PriceResponse] =
    stream().map(x => PriceResponse.fromEvent(x)).collect { case Some(p) => p }
}

object WebsocketSession:

  import net.specula.twelvedata.client.model.ApiCodecs.*
  import zio.json.*

  def socketApp(tickers: List[String],
                queue: Queue[Event]): Handler[Any, Throwable, WebSocketChannel, Nothing] =
    
    Handler.webSocket { channel =>
      channel.receiveAll {
        case UserEventTriggered(UserEvent.HandshakeComplete) =>
//          zio.Console.printLine(s"Subscribing to tickers ${tickers.mkString(",")}...") *>
            channel.send(ChannelEvent.Read(WebSocketFrame.text(
              s"""
                 |{
                 |  "action": "subscribe",
                 |  "params": {
                 |	"symbols": "${tickers.mkString(",")}"
                 |  }
                 |}
                 |""".stripMargin)))

        case Read(WebSocketFrame.Close(status, reason)) =>
          Console.printLine("Closing channel with status: " + status + " and reason: " + reason)

        case Read(WebSocketFrame.Text(t)) =>
          //println(s"[${Instant.now}] Got event over websocket: " + t)
          t.fromJson[Event] match {
            case Left(e) => ZIO.logWarning(s"Error receiving event: $e")
            case Right(value) => queue.offer(value)
          }

        case ExceptionCaught(t) =>
          ZIO.logError(s"Exception caught: ${t.getMessage}") *> ZIO.fail(t)

        case other =>
          ZIO.logInfo(s"Received other: ${other}")

      }
    }
end WebsocketSession

object TwelveDataWebsocketClient {

  /**
   * Opens a websocket with Twelvedata API and streams prices for the given tickers.
   */
  def streamPrices(tickers: Seq[String],
                   q: Queue[Event]): RIO[Scope & TwelveDataClient, WebsocketSession] = {
    for {
      client <- ZIO.service[TwelveDataClient]
      config = client.config
      url = s"wss://ws.twelvedata.com/v1/quotes/price?apikey=${config.apiKey}"
      _ <- WebsocketSession.socketApp(tickers.toList, q)
        .connect(url)
        .provide(Client.default, Scope.default)
        .forkDaemon
    } yield WebsocketSession(url, tickers.toSet, q)
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