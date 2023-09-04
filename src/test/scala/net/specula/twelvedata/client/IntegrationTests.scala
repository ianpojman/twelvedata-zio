package net.specula.twelvedata.client

import net.specula.twelvedata.client.model.{TimeSeriesInterval, TimeSeriesIntervalQuery, TwelveDataComplexDataRequest}
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
          .provide(Layers.defaultLayers)
      } yield assertTrue(response.keySet == Set("AAPL","MSFT"))
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
          .provide(Layers.defaultLayers)
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
          .provide(Layers.defaultLayers)
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
        intervals = List(TimeSeriesInterval.OneMinute,TimeSeriesInterval.FiveMinutes),
        methods = ComplexMethodList.fromComplexMethods(ComplexMethod.timeseries()),
        start_date = Some(LocalDate.parse("2022-04-05")),
        timezone = Some("America/New_York"),
        outputsize = 50
      )

      for {
        response <- TwelveDataClient.fetchComplexData(request)
          .provide(Layers.defaultLayers)
//        _ <- zio.Console.printLine("RESPONSE = "+response)
      } yield {
        assertTrue(response.status == "ok") &&
          assertTrue(response.dataList.headOption.map(_.values.size).exists(_ > 1))
      }
    },

    test("stream realtime prices with websocket") {
      for {
        s <- TwelveDataClient.newSession(List("AAPL", "MSFT", "GOOGL", "BTC/USD"))
          .provideSome[Scope](Layers.defaultLayers)
        res <- s.stream().take(3).runCollect
      } yield {
        assertTrue(res.exists(_.price.isDefined))
      }
    },
  )
}
