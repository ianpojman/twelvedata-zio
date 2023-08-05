package net.specula.twelvedata.client

import zio.http.Client
import zio.test.*
import zio.{Schedule, ZIO, ZIOAppDefault, ZLayer, *}

/** These tests require Api key to be set up locally. They verify basic response parsing is working */
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
      } yield assertTrue(response.keySet == Set(Symbol.fromString("AAPL"), Symbol.fromString("MSFT")))
    },
  )

  val retryPolicy = (Schedule.exponential(10.milliseconds) >>> Schedule.elapsed).whileOutput(_ < 30.seconds)

  private def fetchPrices(symbols: String*): ZIO[Any, Throwable, TickerToApiPriceMap] = {
    val symbolList = symbols.map(Symbol.fromString(_)).toList
    for {
      tickers <- TwelveDataClient.fetchPrices
        .provide(Layers.defaultLayers ++ ZLayer.succeed(symbolList))
      _ <- zio.Console.printLine("Prices = " + tickers)
    } yield tickers
  }

  private def fetchTimeseries(timeSeriesInterval: TimeSeriesInterval, tickers: String*) = {
    val symbols = tickers.map(_.ensuring(!_.contains(","))).toList
    val query = TimeSeriesIntervalQuery(symbols.map(Symbol.fromString), timeSeriesInterval)
    for {
      timeSeriesItems <-
        TwelveDataClient.fetchHistoricalPriceChanges
          .provide(Layers.defaultLayers ++ ZLayer.succeed(query))
    } yield timeSeriesItems
  }

}
