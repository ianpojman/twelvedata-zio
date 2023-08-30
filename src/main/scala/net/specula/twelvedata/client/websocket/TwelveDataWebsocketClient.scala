package net.specula.twelvedata.client.websocket

import net.specula.twelvedata.client.{Layers, TwelveDataClient}
import net.specula.twelvedata.client.model.{Event, PriceResponse}
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.HandshakeComplete
import zio.http.html.q
import zio.stream.ZStream

import java.time.Instant
import scala.util.Right

trait PriceHandler {
  def accept(p: PriceResponse): Task[Unit]
}

case class WebsocketSession(url: String,
                            tickers: Set[String],
                            queue: Queue[Event]) {

  // val tickers = Set("AAPL", "MSFT", "GOOG", "AMZN", "FB", "TSLA", "NVDA", "PYPL", "ADBE", "NFLX", "INTC", "CMCSA", "PEP", "CSCO", "AVGO", "TMUS", "TXN", "QCOM", "AMGN", "CHTR", "SBUX", "GILD", "MDLZ", "FISV", "INTU", "BKNG", "ADP", "ISRG", "VRTX", "REGN", "AMD", "MU", "ATVI", "CSX", "ILMN", "ADI", "ADSK", "BIIB", "AMAT", "MELI", "LRCX", "WBA", "JD", "NXPI", "ADI", "ADSK", "BIIB", "AMAT", "MELI", "LRCX", "WBA", "JD", "NXPI", "KHC", "EXC", "EBAY", "ROST", "MAR", "WDC", "CTSH", "ORLY", "EA", "BIDU", "KLAC", "MNST", "WDAY", "NTES", "XEL", "SNPS", "CTAS", "VRSK", "PCAR", "XLNX", "PAYX", "DLTR", "ALGN", "ANSS", "SIRI", "CDNS", "FAST", "SWKS", "CPRT", "MXIM", "CERN", "CHKP", "INCY", "ULTA", "TTWO", "FOXA", "FOX", "IDXX", "MCHP", "NTAP", "LULU", "VRSN", "ASML", "TCOM", "CDW", "SGEN", "EXPE", "WLTW", "DOCU", "CTXS", "KLAC", "MNST", "WDAY", "NTES", "XEL", "SNPS", "CTAS", "VRSK", "PCAR", "XLNX", "PAYX", "DLTR", "ALGN", "ANSS", "SIRI", "CDNS", "FAST", "SWKS", "CPRT", "MX


//
//  def checkConnection(): ZIO[Scope, Nothing, Unit] =
//    ref.get.flatMap {
//      case Some(fiberId) =>
//        // nothing to do, we're already connected
//        ZIO.unit
//      case None =>
////        ZIO.scoped {
//          socketApp(tickers.toList)
//            .connect(url)
//            .provide(Client.default, Scope.default)
//            .forkDaemon
//            .tap(fiberId => ref.set(Some(fiberId.id)))
//            .unit
////        }
//    }

  /** start a new stream based on whatever the active websocket client has downloaded and enqueued */
  def stream(): ZStream[Any, Throwable, Event] = ZStream.fromQueue(queue)
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
          val maybeE = t.fromJson[Event]
          maybeE match {
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

  import net.specula.twelvedata.client.model.Event.*
  import zio.json.*

  /**
   * Opens a websocket with Twelvedata API and streams prices for the given tickers.
   */
  def streamPrices(tickers: Seq[String],
                   q: Queue[Event]): RIO[Scope & TwelveDataClient, WebsocketSession] = {
    for {
      client <- ZIO.service[TwelveDataClient]
      config = client.config
      ref <- Ref.make[Option[FiberId]](None)
      url = s"wss://ws.twelvedata.com/v1/quotes/price?apikey=${config.apiKey}"
      _ <- WebsocketSession.socketApp(tickers.toList, q)
        .connect(url)
        .provide(Client.default, Scope.default)
        .forkDaemon
//        .tap(fiberId => ref.set(Some(fiberId.id)))
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