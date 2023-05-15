package net.specula.twelvedata.client

import net.specula.twelvedata.client.model.Price
import net.specula.twelvedata.client.model.twelvedata.*
import zio.*
import zio.http.Client

import java.time.{Instant, LocalDate, Period, ZoneOffset}


// TODO: spot silver/gold price: https://api.twelvedata.com/exchange_rate?symbol=XAG/USD&apikey=bf7d15e28ce44d62bf20f944901cc398
object TwelveDataClient {
  val baseUrl = "https://api.twelvedata.com" //quote?apikey=&symbol='

  import ApiQuote.*
  import zio.json.*

  type ApiQueryRequirements = Client & TwelveDataConfig

  val fetchQuote: ZIO[ApiQueryRequirements & List[Symbol], Throwable, Either[String, ApiQuote]] = for {
    symbols <- ZIO.service[List[Symbol]]
    config <- ZIO.service[TwelveDataConfig]
    url = TwelveDataClient.baseUrl + s"/quote?symbol=${symbols.map(_.name).mkString(",")}&apikey=${config.apiKey}"
    _ <- zio.Console.printLine(s"URL: $url")
    res <- Client.request(url)
    response <- res.body.asString
  } yield response.fromJson[ApiQuote]

  val fetchPrices: ZIO[ApiQueryRequirements & List[Symbol], Throwable, Either[String, PriceByTickerMap]] = {
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
  val fetchHistoricalPriceChanges: ZIO[ApiQueryRequirements & TimeSeriesIntervalQuery, Throwable, Either[String, MultiTickerTimeSeriesResponse]] = {
    import TimeSeriesCodecs.*

    for {
      interval <- ZIO.service[TimeSeriesIntervalQuery]
      symbols = interval.symbols
      config <- ZIO.service[TwelveDataConfig]
      url = TwelveDataClient.baseUrl + s"/time_series?interval=${interval.timeSeriesInterval.apiName}" +
        s"&symbol=${symbols.map(_.name).mkString(",")}" +
        s"&end_date=2023-05-12" +
        s"&apikey=${config.apiKey}"
      _ <- zio.Console.printLine(s"URL: $url")
      res <- Client.request(url)
      response <- res.body.asString
      _ <- Console.printLine("Got response: "+response)
      response2 <- ZIO.attempt {
        symbols.headOption match {
          case Some(Symbol(singleTicker)) if symbols.size == 1 => // single ticker case
            val res: Either[String, Map[String, TimeSeriesItems]] = response.fromJson[TimeSeriesItems]
              .map(items => Map(singleTicker -> items))
            res
          // we normalize the response to look like the same as the multi ticker response, so same return type can be used
          case other =>
            response.fromJson[MultiTickerTimeSeriesResponseJson]
        }
      }
    } yield {
      response2
    }
  }
  
  // TimeSeriesItem(2023-05-01 15:30:00
}





