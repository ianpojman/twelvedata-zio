package net.specula.twelvedata.client

import net.specula.twelvedata.client.util.NetworkConfigurationUtil
import zio.http.Client
import zio.{Schedule, ZIO, ZIOAppDefault, ZLayer}

object TwelveDataClientExample extends ZIOAppDefault {

  NetworkConfigurationUtil.trDisableIPv6()

  import zio._

  private val testLayers =
    Client.default ++ ZLayer.fromZIO(TwelveDataConfig.loadConfig)

  val retryPolicy = (Schedule.exponential(10.milliseconds) >>> Schedule.elapsed).whileOutput(_ < 30.seconds)

  private val fetchPrices = {
    val tickers = "BTC/USD".split(",").map(Symbol(_)).toList
    for {
      tickers <- TwelveDataClient.fetchPrices.provide(testLayers ++ ZLayer.succeed(tickers)).retry(retryPolicy)
      _ <- zio.Console.printLine("quotes = " + tickers)
    } yield ()
  }

  private val fetchPricesInWindow = {
    val tickers = "AAPL,MSFT".split(",").map(Symbol(_)).toList
    val query = TimeSeriesIntervalQuery(tickers, TimeSeriesInterval.ThirtyMinutes)
    for {
      timeSeriesItems <- TwelveDataClient.fetchHistoricalPriceChanges
        .provide(testLayers ++  ZLayer.succeed(query))
      //_ <- zio.Console.printLine("time series interval response = " + tickers)
      _ <- timeSeriesItems match
        case Left(value) =>
          ZIO.fail(value)
        case Right(value) => ZIO.foreachDiscard(value) { case (k,v) =>
          //ZIO.succeed(println(s"$k -> $v"))
          val msg = v.values.map { x =>
            s"$k @ ${x.datetime} -> close price: ${x.close}"
          }.mkString("\n")
          Console.printLine(msg)
        }

    } yield timeSeriesItems
  }

  // TODO fetch options data
//  private val fetchPricesInWindow = {

  override val run = {
//    fetchPrices *> fetchPricesInWindow
    fetchPricesInWindow
  }

}
