package net.specula.twelvedata.client

import net.specula.twelvedata.client.model.TwelveDataHistoricalDataRequest
import zio.*
import zio.http.Client
import zio.test.*

import java.time.Instant

/** These tests require Api key to be set up locally. They verify basic response parsing is working */
object IntegrationTests extends ZIOSpecDefault {
  def spec = suite("api integration tests")(
    test("fetch single price") {
      for {
        response <- TwelveDataClient.fetchPrices("BTC/USD")
          .provide(Layers.defaultLayers)
      } yield assertTrue(response.keySet.contains("BTC/USD"))
    },
    test("fetch multiple prices") {
      for {
        response <- TwelveDataClient.fetchPrices("BTC/USD", "AAPL")
          .provide(Layers.defaultLayers)
      } yield
        assertTrue(response.keySet == Set("AAPL", "BTC/USD"))
    },
    test("fetch time series") {
      for {
        response <- TwelveDataClient.fetchTimeSeries(TimeSeriesIntervalQuery( List("AAPL", "MSFT").map(Symbol.fromString), TimeSeriesInterval.OneMinute))
          .provide(Layers.defaultLayers)
      } yield assertTrue(response.keySet == Set(Symbol.fromString("AAPL"), Symbol.fromString("MSFT")))
    },
    test("fetch historical data") {
      val request = TwelveDataHistoricalDataRequest(symbols = List("AAPL"),
        intervals = List("1min"),
        outputsize = 25,
        methods = List(Map("ema" ->  10)),
        start_date = Some(Instant.parse ( "2010-04-05T17:16:00Z" )), end_date = None)

      for {
        response <- TwelveDataClient.fetchHistoricalData(request) // Symbol.fromString("AAPL"))
          .provide(Layers.defaultLayers)
        //fetchTimeseries(TimeSeriesInterval.OneMinute, "AAPL", "MSFT")
      } yield assertTrue(response.status == "ok")
    },
  )

  val retryPolicy = (Schedule.exponential(10.milliseconds) >>> Schedule.elapsed).whileOutput(_ < 30.seconds)

//  private def fetchPrices(symbols: String*): ZIO[Any, Throwable, TickerToApiPriceMap] = {
//    val symbolList = symbols.map(Symbol.fromString(_)).toList
//    for {
//      tickers <- TwelveDataClient.fetchPrices
//        .provide(Layers.defaultLayers ++ ZLayer.succeed(symbolList))
//      _ <- zio.Console.printLine("Prices = " + tickers)
//    } yield tickers
//  }



}
