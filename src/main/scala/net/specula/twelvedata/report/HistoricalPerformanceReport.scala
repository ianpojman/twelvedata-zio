package net.specula.twelvedata.report

import net.specula.twelvedata.client.{Layers, TwelveDataClient}
import net.specula.twelvedata.client.model.{TimeSeriesInterval, TwelveDataComplexDataRequest}
import net.specula.twelvedata.client.rest.{ComplexMethod, ComplexMethodList}
import zio.*

import java.time.OffsetDateTime

case class ReportRequest(tickers: List[String], periods: List[Period])

case class Period(name: String, daysAgo: Int, interval: TimeSeriesInterval, bars: Int)

case class ReportEntry(ticker: String, changes: Map[String, Double])

case class Report(entries: List[ReportEntry])

object PerformanceReport {

  // Define a utility function to calculate percentage change
  def percentageChange(oldValue: Double, newValue: Double): Double = {
    if (oldValue == 0) 0 else (newValue - oldValue) / oldValue * 100
  }

  // Define a function to generate the report
  def generateReport(request: ReportRequest): ZIO[TwelveDataClient, Throwable, Report] = {
    for {
      currentDate <- zio.Clock.currentDateTime
      entries <- ZIO.foreach(request.tickers) { ticker =>
        for {
          changes <- ZIO.foreach(request.periods) { period =>
            fetchForPeriod(currentDate, ticker, period)
          }
        } yield ReportEntry(ticker, changes.toMap)

      }
    } yield Report(entries)
  }

  private def fetchCurrentPrice(ticker: String): ZIO[TwelveDataClient, Throwable, Double] = {
    for {
      response <- TwelveDataClient.fetchQuote(ticker)
        .mapError(_ => new RuntimeException(s"Unable to get current price for $ticker"))
    } yield response.close
  }

  private def fetchForPeriod(currentDate2: OffsetDateTime, ticker: String, period: Period) = {
    val currentDate = currentDate2.minusDays(1).toLocalDate
    println(s"Fetching data for Ticker: $ticker, Period: ${period.name}, Start Date: $currentDate") // Print the ticker, period, and start date


    val requestData = TwelveDataComplexDataRequest(
      symbols = List(ticker),
      intervals = List(period.interval), // Use the interval from the period
      methods = ComplexMethodList.fromComplexMethods(ComplexMethod.timeseries()),
      start_date = Some(currentDate),
      outputsize = period.bars, // Use the output size from the period
      timezone = Some("America/New_York")
    )
    for {
      response <- TwelveDataClient.fetchComplexData(requestData)

      values = response.valuesForSymbol(ticker)

      firstDataPoint = values.flatMap(_.headOption) // service returns prices in time desc order
      lastDataPoint = values.flatMap(_.lastOption)

      _ <- ZIO.attempt {


        // Printing out the details.
        firstDataPoint.foreach { data =>
          println(s"First Date: ${data.datetime}")
          println(s"First Close Value: ${data.closeValue.getOrElse("N/A")}")
        }

        lastDataPoint.foreach { data =>
          println(s"Last Date: ${data.datetime}")
          println(s"Last Close Value: ${data.closeValue.getOrElse("N/A")}")
        }
      }
      _ = println(s"Raw Data Response for ${period.name}: ${response.valuesForSymbol("AAPL")}")
      // Fixed section
      endDate <- ZIO.fromOption(lastDataPoint.map(_.datetime))
        .mapError(_ => new RuntimeException(s"Unable to get end date for $ticker"))

      startDateFromResponse <- ZIO.fromOption(firstDataPoint.map(_.datetime))
        .mapError(_ => new RuntimeException(s"Unable to get start date for $ticker"))

      startValue <- ZIO.fromOption(lastDataPoint.flatMap(_.closeValue))
        .mapError(_ => new RuntimeException(s"Unable to get end value for $ticker"))

      endValue <- fetchCurrentPrice(ticker)

      _ = println(s"Start Date from Response: $startDateFromResponse, End Date: $endDate, for ${period.name}")
      pc = percentageChange(startValue, endValue)
      _ = println(s"Start Value: $startValue,  End Value: $endValue for ${period.name} (percentage change: $pc)")

    } yield {
      period.name -> percentageChange(startValue, endValue)
    }

  }
}

object HistoricalPerformanceReportTest extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val request = ReportRequest(
      tickers = List("AAPL"), //, "GOOG"),
      periods = List(
        Period("1 day", 1, TimeSeriesInterval.OneDay, 1),
        Period("1 week", 5, TimeSeriesInterval.OneWeek, 1),
        Period("1 month", 30, TimeSeriesInterval.OneMonth, 1),
        Period("1 year", 365, TimeSeriesInterval.OneMonth, 12)
      )
    )

    for {
      r <- PerformanceReport.generateReport(request).provideLayer(Layers.defaultLayers)
      _ <- zio.Console.printLine("report: " + r.toString)
    } yield ()
  }
}
