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
    val tickers = "SPX,AAPL".split(",").map(Symbol(_)).toList
    for {
      tickers <- TwelveDataClient.fetchPrices.provide(testLayers ++ ZLayer.succeed(tickers)).retry(retryPolicy)
      _ <- zio.Console.printLine("quotes = " + tickers)
    } yield ()
  }

  private val fetchPricesInWindow = {
    val tickers = "SPX,AAPL".split(",").map(Symbol(_)).toList
    for {
      tickers <- TwelveDataClient.fetchHistoricalPriceChanges.provide(testLayers ++ ZLayer.succeed(tickers) ++ ZLayer.succeed(TimeSeriesInterval.OneHour))
      _ <- zio.Console.printLine("time series interval response = " + tickers)
    } yield ()
  }


  override val run = {
    fetchPrices *> fetchPricesInWindow
  }

}
