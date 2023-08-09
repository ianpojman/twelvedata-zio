package net.specula.twelvedata.client

import net.specula.twelvedata.client.TwelveDataUrls.cleanUrl
import net.specula.twelvedata.client.model.{ApiPrice, ApiQuote, TimeSeriesIntervalQuery, TimeSeriesItems, TwelveDataHistoricalDataRequest, TwelveDataHistoricalDataResponse}
import zio.*
import zio.http.{Body, Client, Method}

object TwelveDataUrls {
  val baseUrl = "https://api.twelvedata.com"

  def cleanUrl(url: String) = url.replaceAll("\\?apikey.*", "")

  /**
   * Quote endpoint is an efficient method to retrieve the latest quote of the selected instrument.
   * [[https://twelvedata.com/docs#quote]]
   */
  def quoteUrl(symbols: List[model.Symbol], config: TwelveDataConfig) =
    baseUrl + s"/quote?symbol=${symbols.map(_.name).mkString(",")}&apikey=${config.apiKey}"

  def findPriceUrl(symbols: Seq[model.Symbol], config: TwelveDataConfig) =
     baseUrl + s"/price?symbol=${symbols.map(_.name).mkString(",")}&apikey=${config.apiKey}"
}

class TwelveDataClient(client: Client, config: TwelveDataConfig) {

  import net.specula.twelvedata.client.model.json.JsonCodecs.*
  import zio.json.*

  private val requiredClientLayer: ULayer[Client] = ZLayer.succeed(client)

  /** Quote has OHLC, volume */
  def fetchQuote(symbols: List[model.Symbol]): Task[ApiQuote] = for {
    url <- ZIO.attempt(TwelveDataUrls.quoteUrl(symbols, config))
    _ <- zio.Console.printLine(s"Fetching quotes: ${cleanUrl(url)}")
    res <- Client.request(url).provide(requiredClientLayer)
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
  def fetchPrices(symbols: Seq[model.Symbol]): ZIO[Any, Throwable, TickerToApiPriceMap] = {
    import net.specula.twelvedata.client.model.ApiCodecs.*
    val url = TwelveDataUrls.findPriceUrl(symbols, config)
    for {
      _ <- zio.Console.printLine(s"Fetching prices: ${cleanUrl(url)}")
      res <- Client.request(url).provide(requiredClientLayer)
      response <- res.body.asString

      // If multiple tickers are being queried, the JSON in the response body format will correspond to TickerToApiPriceMap.
      // In the second case it's a flatter model, just an ApiPrice. So for the second case we convert the ApiPrice to a TickerToApiPriceMap
      // to have a consistent return type for both cases.
      multiSymbolResponse =
        symbols.toList match
          case List(singleSymbol) =>
            response.fromJson[ApiPrice].map(apiPrice => Map(singleSymbol.name -> apiPrice))
          case _ =>
            response.fromJson[TickerToApiPriceMap]

      e <- ZIO.fromEither(multiSymbolResponse)
        .mapError(new RuntimeException(_))
    } yield e
  }


  /** Get historical prices starting at the most recent price,in the given time intervals. Ordered by descending time
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
  def fetchTimeSeries(interval: TimeSeriesIntervalQuery): Task[Map[model.Symbol, TimeSeriesItems]] = {

    val symbols = interval.symbols
    val url = TwelveDataUrls.baseUrl + s"/time_series?interval=${interval.timeSeriesInterval.apiName}" +
      s"&symbol=${symbols.map(_.name).mkString(",")}" +
      s"&end_date=2023-05-12" +
      s"&apikey=${config.apiKey}"

    for {
      _ <- zio.Console.printLine(s"URL: $url")
      res <- Client.request(url).provide(requiredClientLayer)
      response <- res.body.asString

      remoteResponseParsed: Either[String, TickerToTimeSeriesItemMap] =
        parseResponse(response, symbols)

      // convert the twelvedata api either to a ZIO function
      res2 <- ZIO.fromEither(remoteResponseParsed)
        .mapError(new RuntimeException(_))
        .map(_.map { case (k, v) => model.Symbol.fromString(k) -> v }.toMap)
    } yield {
      res2
    }

  }

  def fetchHistoricalData(req: TwelveDataHistoricalDataRequest): Task[TwelveDataHistoricalDataResponse] = {
    val url = TwelveDataUrls.baseUrl + s"/complex_data?apikey=${config.apiKey}"

    for {
//      _ <- zio.Console.printLine(s"URL: $url")
      res <- Client.request(url, method = Method.POST, content = Body.fromString(req.toJson))
        .provide(requiredClientLayer)
      responseString <-
        if (res.status.isSuccess)
          res.body.asString
        else
          ZIO.fail(new RuntimeException(s"Error fetching historical data: ${res.status} ${res.body.asString}"))

      remoteResponseParsed <-
        ZIO.fromEither(responseString.fromJson[TwelveDataHistoricalDataResponse])
        .mapError(new RuntimeException(_))
        .tapError(_ => Console.printLine(s"ERROR: Request JSON body was: ${req.toJson}"))
        .tapError(_ => Console.printLine(s"ERROR: Response JSON body was: ${responseString.replaceAll("\n", "")}"))
    } yield remoteResponseParsed
  }

  /**
   * as in other endpoints, the response body format varies depending on whether multiple tickers are being queried.
   * when multiple `Symbol`s are provided, the response body is a `Map` of `Symbol`s to `TimeSeriesItems`.
   * when a single `Symbol` is provided, the response body is just the single `TimeSeriesItems`.
   * @param response Response body from the API
   * @param symbols tickers we asked to look up in the request (assumes the response has the same tickers?)
   * @return
   */
  private def parseResponse(response: String, symbols: List[model.Symbol]): Either[String, TickerToTimeSeriesItemMap] = {
    symbols match {
      case List(singleSymbol) =>
        response.fromJson[TimeSeriesItems]
          .map(timeSeriesItem => Map(singleSymbol.name -> timeSeriesItem))
      case Nil =>
        sys.error("Symbols required")
      case _ =>
        response.fromJson[TickerToTimeSeriesItemMap]
    }
  }


}

object TwelveDataClient:

  lazy val live: ZLayer[Client & TwelveDataConfig, Nothing, TwelveDataClient] =
    ZLayer {
      for {
        metadataRepo <- ZIO.service[Client]
        blobStorage <- ZIO.service[TwelveDataConfig]
      } yield TwelveDataClient(metadataRepo, blobStorage)
    }

  def fetchHistoricalData(request: TwelveDataHistoricalDataRequest): ZIO[TwelveDataClient, Throwable, TwelveDataHistoricalDataResponse] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchHistoricalData(request))

  def fetchTimeSeries(intervalQuery: TimeSeriesIntervalQuery): ZIO[TwelveDataClient, Throwable, Map[model.Symbol, TimeSeriesItems]] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchTimeSeries(intervalQuery))

  def fetchPrices(tickers: String*): ZIO[TwelveDataClient, Throwable, TickerToApiPriceMap] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchPrices(tickers.map(model.Symbol.fromString)))

end TwelveDataClient
