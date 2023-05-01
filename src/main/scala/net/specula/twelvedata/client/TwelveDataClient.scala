package net.specula.twelvedata.client

import net.specula.twelvedata.client.model.Price
import net.specula.twelvedata.client.model.twelvedata.*
import zio.*
import zio.http.Client

import java.time.{Instant, LocalDate, Period, ZoneOffset}



object TwelveDataClient {
  val baseUrl = "https://api.twelvedata.com" //quote?apikey=&symbol='

  import ApiQuote.*
  import zio.json.*

  type ApiQueryRequirements = Client & TwelveDataConfig & List[Symbol]

  val fetchQuote: ZIO[ApiQueryRequirements, Throwable, Either[String, ApiQuote]] = for {
    symbols <- ZIO.service[List[Symbol]]
    config <- ZIO.service[TwelveDataConfig]
    url = TwelveDataClient.baseUrl + s"/quote?symbol=${symbols.map(_.name).mkString(",")}&apikey=${config.apiKey}"
    _ <- zio.Console.printLine(s"URL: $url")
    res <- Client.request(url)
    response <- res.body.asString
  } yield response.fromJson[ApiQuote]

  val fetchPrices: ZIO[ApiQueryRequirements, Throwable, Either[String, PriceByTickerMap]] = {
    import ApiCodecs.*
    for {
      symbols <- ZIO.service[List[Symbol]]
      config <- ZIO.service[TwelveDataConfig]
      url = TwelveDataClient.baseUrl + s"/price?symbol=${symbols.map(_.name).mkString(",")}&apikey=${config.apiKey}"
      _ <- zio.Console.printLine(s"URL: $url")
      res <- Client.request(url)
      response <- res.body.asString
    } yield response.fromJson[PriceByTickerMap]
  }


  /** Generate historical changes in price range at various time intervals, e.g. % change in last 1M, 5M and 1Y.
   * {{{
   *  $ curl 'https://api.twelvedata.com/time_series?symbol=AAPL,MSFT&interval=1year&outputsize=90&apikey=bf7d15e28ce44d62bf20f944901cc398'
   * }}}
   * 
   * response: 
   * {{{
   *   {
   *    "AAPL": {
   *      "meta": {
   *        "symbol": "AAPL",
   *        "interval": "1min",
   *        "currency": "USD",
   *        "exchange_timezone": "America/New_York",
   *        "exchange": "NASDAQ",
   *        "mic_code": "XNGS",
   *        "type": "Common Stock"
   *      },
   *      "values": [
   *      {
   *        "datetime": "2023-05-01 15:59:00",
   *        "open": "169.89500",
   *        "high": "169.89999",
   *        "low": "169.55000",
   *        "close": "169.56000",
   *        "volume": "867211"
   *      },
   *    ....
   *    }, ...
   * }}}
   * */
  val fetchHistoricalPriceChanges: ZIO[ApiQueryRequirements & TimeSeriesInterval, Throwable, Either[String, TimeSeriesResponse]] = {
    import TimeSeriesCodecs.*

    for {
      symbols <- ZIO.service[List[Symbol]]
      interval <- ZIO.service[TimeSeriesInterval]
      config <- ZIO.service[TwelveDataConfig]
      url = TwelveDataClient.baseUrl + s"/time_series?interval=${interval.apiName}&symbol=${symbols.map(_.name).mkString(",")}&apikey=${config.apiKey}"
      _ <- zio.Console.printLine(s"URL: $url")
      res <- Client.request(url)
      response <- res.body.asString
      _ <- Console.printLine("Got response: "+response)
    } yield response.fromJson[Map[String, TimeSeriesItems]]
  }
}





