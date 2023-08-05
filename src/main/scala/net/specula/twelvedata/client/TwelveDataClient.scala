package net.specula.twelvedata.client

import net.specula.twelvedata.client.TwelveDataUrls.cleanUrl
import net.specula.twelvedata.client.model.{ApiPrice, ApiQuote}
import zio.*
import zio.http.Client

import java.time.{Instant, LocalDate, Period, ZoneOffset}

object TwelveDataUrls {
  def cleanUrl(url: String) = url.replaceAll("\\?apikey.*", "")

  /**
   * Quote endpoint is an efficient method to retrieve the latest quote of the selected instrument.
   * [[https://twelvedata.com/docs#quote]]
   */
  def quoteUrl(symbols: List[Symbol], config: TwelveDataConfig) =
    TwelveDataClient.baseUrl + s"/quote?symbol=${symbols.map(_.name).mkString(",")}&apikey=${config.apiKey}"

  def findPriceUrl(symbols: List[Symbol], config: TwelveDataConfig) =
    TwelveDataClient.baseUrl + s"/price?symbol=${symbols.map(_.name).mkString(",")}&apikey=${config.apiKey}"
}

// TODO: spot silver/gold price: https://api.twelvedata.com/exchange_rate?symbol=XAG/USD&apikey=bf7d15e28ce44d62bf20f944901cc398
object TwelveDataClient {
  val baseUrl = "https://api.twelvedata.com" //quote?apikey=&symbol='

  import zio.json.*

  /** Quote has OHLC, volume */
  val fetchQuote: ZIO[ApiQueryRequirements & List[Symbol], Throwable, ApiQuote] = for {
    symbols <- ZIO.service[List[Symbol]]
    config <- ZIO.service[TwelveDataConfig]
    url = TwelveDataUrls.quoteUrl(symbols, config)
    _ <- zio.Console.printLine(s"Fetching quotes: ${cleanUrl(url)}")
    res <- Client.request(url)
    response <- res.body.asString
    e <- ZIO.fromEither(response.fromJson[ApiQuote]).mapError(new RuntimeException(_))
  } yield e


  /** Simple current price only query.
   * Price model varies depending if there are multiple tickers:
   * {{{
   * ❯ curl 'https://api.twelvedata.com/price?apikey=...&symbol=AAPL,MSFT'
   * {"AAPL":{"price":"182.00000"},"MSFT":{"price":"327.79999"}}%
   * ❯ curl 'https://api.twelvedata.com/price?apikey=...&symbol=AAPL'
   * {"price":"182.00000"}%
   *
   * }}}
   */
  val fetchPrices: ZIO[ApiQueryRequirements & List[Symbol], Throwable, TickerToApiPriceMap] = {
    import net.specula.twelvedata.client.model.ApiCodecs.*
    for {
      symbols <- ZIO.service[List[Symbol]]
      config <- ZIO.service[TwelveDataConfig]
      url = TwelveDataUrls.findPriceUrl(symbols, config)

      _ <- zio.Console.printLine(s"Fetching prices: ${cleanUrl(url)}")
      res <- Client.request(url)
      response <- res.body.asString

      // If multiple tickers are being queried, the JSON in the response body format will correspond to TickerToApiPriceMap.
      // In the second case it's a flatter model, just an ApiPrice. So for the second case we convert the ApiPrice to a TickerToApiPriceMap
      // to have a consistent return type for both cases.
      multiSymbolResponse =
        symbols match
          case List(singleSymbol) =>
            response.fromJson[ApiPrice].map(apiPrice => Map(singleSymbol.name -> apiPrice))
          case _ =>
            response.fromJson[TickerToApiPriceMap]

      e <- ZIO.fromEither(multiSymbolResponse)
        .mapError(new RuntimeException(_))
    } yield e
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
  val fetchHistoricalPriceChanges: ZIO[ApiQueryRequirements & TimeSeriesIntervalQuery, Throwable, MultiTickerTimeSeriesResponse] = {
    import TimeSeriesCodecs.*

    val either: ZIO[Client with TwelveDataConfig with TimeSeriesIntervalQuery, Throwable, TickerToTimeSeriesItemMap] = for {
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
//      _ <- Console.printLine("Got response: " + response)
      tickerToTimeSeriesMap: TickerToTimeSeriesItemMap <- {
        val e: Either[String, TickerToTimeSeriesItemMap] = symbols.headOption match {
          case Some(Symbol(singleTicker)) if symbols.size == 1 => // single ticker case
            val res: Either[String, Map[String, TimeSeriesItems]] = response.fromJson[TimeSeriesItems]
              .map(items => Map(singleTicker -> items))
            res
          // we normalize the response to look like the same as the multi ticker response, so same return type can be used
          case _ =>
            response.fromJson[TickerToTimeSeriesItemMap]
        }
        ZIO.fromEither(e.left.map(new RuntimeException(_)))
      }
    } yield {
      tickerToTimeSeriesMap
    }
    either
  }

  def websocket = {

  }
  // TimeSeriesItem(2023-05-01 15:30:00
}





