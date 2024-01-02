package net.specula.twelvedata.client

import net.specula.twelvedata.client.model.{ApiQuote, TimeSeriesInterval, TimeSeriesIntervalQuery, TwelveDataComplexDataRequest}
import net.specula.twelvedata.client.rest.{ComplexMethod, ComplexMethodList}
import zio.*
import zio.test.*

import java.time.LocalDate

/** These tests require Api key to be set up locally. They verify basic response parsing is working */
object IntegrationTests extends ZIOSpecDefault {
  def spec = suite("api integration tests")(

    test("fetch single price") {
      for {
        response <- TwelveDataClient.fetchPrices("BTC/USD")
      } yield assertTrue(response.keySet.contains("BTC/USD"))
    },

    test("fetch multiple prices") {
      for {
        response <- TwelveDataClient.fetchPrices("BTC/USD", "AAPL")
      } yield
        assertTrue(response.keySet == Set("AAPL", "BTC/USD"))
    },

    test("fetch time series - multiple tickers") {
      for {
        response <- TwelveDataClient.fetchTimeSeries(
          TimeSeriesIntervalQuery(
            symbols = List("AAPL", "MSFT"),
            timeSeriesInterval = TimeSeriesInterval.OneDay,
            startDate = Some(LocalDate.parse("2022-04-05")),
            endDate = Some(LocalDate.parse("2022-04-09")),
            timezone = "America/New_York"
          )
        )
      } yield assertTrue(response.keySet == Set("AAPL", "MSFT"))
    },

    // note that if you want a price at open of a given date, you need to fetch the date after it. here, we want 2022-04-08 open, so we fetch 2022-04-09
    test("fetch time series - getting the price as of a specific date") {
      for {
        response <- TwelveDataClient.fetchTimeSeries(
          TimeSeriesIntervalQuery(
            symbols = List("AAPL"),
            timeSeriesInterval = TimeSeriesInterval.OneDay,
            endDate = Some(LocalDate.parse("2022-04-09")),
            timezone = "America/New_York",
            outputCount = 1
          )
        )
      } yield {
        val res = response.head._2.values.head
        assertTrue(res.datetime == "2022-04-08")
      }
    },

    // note that if you want a price at open of a given date, you need to fetch the date after it. here, we want 2022-04-08 open, so we fetch 2022-04-09
    test("fetch multiple time series - getting the price as of a specific date w/ different intervals") {
      val request = TwelveDataComplexDataRequest(
        symbols = List("AAPL"),
        intervals = List(TimeSeriesInterval.OneDay),
        methods = ComplexMethodList.fromComplexMethods(ComplexMethod.timeseries()),
        start_date = Some(LocalDate.parse("2022-04-05")),
        timezone = Some("America/New_York"),
        outputsize = 30
      )

      for {
        response <- TwelveDataClient.fetchComplexData(request)
        //        _ <- zio.Console.printLine("RESPONSE = " + response)
      } yield {
        val firstResult = response.dataList.headOption
        //        val responses = response.dataList.map(i => s"Response for symbol ${i.meta.symbol} at interval ${i.meta.interval}, dates = "+i.values.map(_.datetime).mkString(","))
        //        println("Got responses: \n"+responses.mkString("\n"))
        assertTrue(response.status == "ok") &&
          assertTrue(firstResult.map(_.values.size).exists(_ > 1))
      }
    },

    test("fetch historical data with multiple tickers and timeframes") {
      val request = TwelveDataComplexDataRequest(
        symbols = List("AAPL", "MSFT"),
        intervals = List(TimeSeriesInterval.OneMinute, TimeSeriesInterval.FiveMinutes),
        methods = ComplexMethodList.fromComplexMethods(ComplexMethod.timeseries()),
        start_date = Some(LocalDate.parse("2022-04-05")),
        timezone = Some("America/New_York"),
        outputsize = 50
      )

      for {
        response <- TwelveDataClient.fetchComplexData(request)
      } yield {
        assertTrue(response.status == "ok") &&
          assertTrue(response.dataList.headOption.map(_.values.size).exists(_ > 1))
      }
    },

    test("fetch historical data with multiple tickers and timeframes") {
      val request = TwelveDataComplexDataRequest(
        symbols = List("AAPL", "MSFT"),
        intervals = List(TimeSeriesInterval.OneMinute, TimeSeriesInterval.FiveMinutes),
        methods = ComplexMethodList.fromComplexMethods(ComplexMethod.timeseries()),
        start_date = Some(LocalDate.parse("2022-04-05")),
        timezone = Some("America/New_York"),
        outputsize = 50
      )

      for {
        response <- TwelveDataClient.fetchComplexData(request)
        //        _ <- zio.Console.printLine("RESPONSE = "+response)
      } yield {
        assertTrue(response.status == "ok") &&
          assertTrue(response.dataList.headOption.map(_.values.size).exists(_ > 1))
      }
    },

    test("fetch options data with multiple tickers and timeframes") {

      for {
        //OptionExpirations(Meta(AAPL,Apple Inc,USD,NASDAQ,XNGS,America/New_York),List(2023-10-06, 2023-10-13, 2023-10-20, 2023-10-27, 2023-11-03, 2023-11-10, 2023-11-17, 2023-12-15, 2024-01-19, 2024-02-16, 2024-03-15, 2024-04-19, 2024-06-21, 2024-09-20, 2024-12-20, 2025-01-17, 2025-06-20, 2025-12-19, 2026-01-16))
        response <- TwelveDataClient.fetchOptionExpirations("AAPL")
        sampleDate <- ZIO.fromOption(response.dates.headOption)

        chain <- TwelveDataClient.fetchOptionChain("AAPL", sampleDate)
        _ <- Console.printLine("Read options chain: " + chain)
      } yield {
        assertTrue(
          chain.calls
            .headOption
            .exists(_.last_price > 0))
      }
    },

    test("fetch quotes") {
      for {
        singleResponse <- TwelveDataClient.fetchQuote("AAPL")
        multipleResponse <- TwelveDataClient.fetchQuotes("AAPL", "MSFT")
      } yield
        assertTrue(singleResponse.symbol == "AAPL") &&
          assertTrue(multipleResponse.keySet == Set("AAPL", "MSFT")
      )
    },

    test("stream realtime prices with websocket") {
      for {
        s <- TwelveDataClient.newSession(List("AAPL", "MSFT", "GOOGL", "BTC/USD"))
          .provideSome[Scope](Layers.defaultLayers)
        res <- s.stream().take(3).runCollect
      } yield {
        assertTrue(res.exists(_.price.isDefined))
      }
    } @@ TestAspect.timeout(15.seconds),

  ).provide(Layers.defaultLayers, Scope.default)
}

