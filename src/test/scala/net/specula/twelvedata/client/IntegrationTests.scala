package net.specula.twelvedata.client

import net.specula.twelvedata.client.model.{TimeSeriesInterval, TimeSeriesIntervalQuery, TwelveDataHistoricalDataRequest}
import net.specula.twelvedata.client.rest.{ComplexMethod, ComplexMethodList}
import zio.*
import zio.http.Client
import zio.test.*

import java.time.{Instant, LocalDate}

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

        response <- TwelveDataClient.fetchTimeSeries(
            TimeSeriesIntervalQuery(
              symbols = List("AAPL", "MSFT"),
              timeSeriesInterval = TimeSeriesInterval.OneDay,
              startDate = Instant.parse("2022-04-05T17:16:00Z"),
              endDate = Instant.parse("2022-04-06T17:16:00Z"))
          )
          .provide(Layers.defaultLayers)
      } yield assertTrue(response.keySet == Set(model.Symbol.fromString("AAPL"), model.Symbol.fromString("MSFT")))
    },

    test("fetch historical data w/ alternate request constructor") {
      val request = TwelveDataHistoricalDataRequest(
        symbols = List("AAPL"),
        intervals = List(TimeSeriesInterval.FifteenMinutes, TimeSeriesInterval.OneDay).map(_.apiName),
        methods = ComplexMethodList.fromComplexMethods(ComplexMethod.timeseries(startDate = LocalDate.parse("2022-04-05"),endDate = LocalDate.parse("2022-04-06"))),
        outputsize = 15,
      )

      for {
        response <- TwelveDataClient.fetchHistoricalData(request) // Symbol.fromString("AAPL"))
          .provide(Layers.defaultLayers)
      } yield {
        assertTrue(response.status == "ok") &&
          assertTrue(response.dataList.headOption.map(_.values.size).exists(_ > 1))
      }
    },

  )

//  case class TwelveDataHistoricalDataRequest(
//                                              symbols: List[String],
//                                              intervals: List[TimeSeriesInterval],
//                                              methods: List[ComplexMethod],
//                                              outputsize: Int, // default on server side is 30
//                                            )

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
