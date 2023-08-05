package net.specula.twelvedata.client

import net.specula.twelvedata.client.util.NetworkConfigurationUtil
import zio.http.Client
import zio.test.*
import zio.{Schedule, ZIO, ZIOAppDefault, ZLayer, *}

/** These tests require Api key to bet set up locally */
object IntegrationTests extends ZIOSpecDefault {
  def spec = suite("api integration tests")(
    test("fetch single price") {
      for {
        response <- fetchPrices("BTC/USD")
      } yield assertTrue(response.keySet.contains("BTC/USD"))
    },
    test("fetch multiple prices") {
      for {
        response <- fetchPrices("BTC/USD", "AAPL")
      } yield
        assertTrue(response.keySet == Set("AAPL", "BTC/USD"))
    },
    test("fetch time series") {
      for {
        response <- fetchTimeseries(TimeSeriesInterval.OneMinute, "AAPL", "MSFT")
      } yield assertTrue(response.keySet == Set("AAPL", "MSFT"))
    },
  )

  val retryPolicy = (Schedule.exponential(10.milliseconds) >>> Schedule.elapsed).whileOutput(_ < 30.seconds)

  private def fetchPrices(symbols: String*): ZIO[Any, Throwable, TickerToApiPriceMap] = {
    val symbolList = symbols.map(Symbol(_)).toList
    for {
      tickers <- TwelveDataClient.fetchPrices.provide(Layers.defaultLayers ++ ZLayer.succeed(symbolList))
        .tapError(e => Console.printLine("error = " + e))
        .retry(retryPolicy)
      _ <- zio.Console.printLine("Prices = " + tickers)
    } yield tickers
  }

  private def fetchTimeseries(timeSeriesInterval: TimeSeriesInterval, tickers: String*) = {
    val symbols = tickers.map(_.ensuring(!_.contains(","))).map(Symbol(_)).toList
    val query = TimeSeriesIntervalQuery(symbols, timeSeriesInterval)
    for {
      timeSeriesItems: MultiTickerTimeSeriesResponse <-
        TwelveDataClient.fetchHistoricalPriceChanges
          .provide(Layers.defaultLayers ++ ZLayer.succeed(query))

      //_ <- zio.Console.printLine("time series interval response = " + tickers)
      //      _ <- ZIO.foreachDiscard(timeSeriesItems) { case (k,v) =>
      //          //ZIO.succeed(println(s"$k -> $v"))
      //          val msg = v.values.take(5).map { x =>
      //            s"$k @ ${x.datetime} -> close price: ${x.close}"
      //          }.mkString("\n")
      //          Console.printLine(msg)
      //        }

    } yield timeSeriesItems
  }

}
