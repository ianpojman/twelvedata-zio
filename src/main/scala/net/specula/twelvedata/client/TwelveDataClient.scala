package net.specula.twelvedata.client

import net.specula.twelvedata.client.TwelveDataUrls.cleanUrl
import net.specula.twelvedata.client.model.*
import net.specula.twelvedata.client.websocket.{TwelveDataWebsocketClient, WebsocketSession}
import zio.*
import zio.http.{Body, Client, Method, Response}
import zio.stream.{ZSink, ZStream}

/** Client for Twelvedata HTTP API */
case class TwelveDataClient(client: Client, config: TwelveDataConfig) {

  import net.specula.twelvedata.client.model.json.JsonCodecs.*
  import zio.json.*

  private val defaultClientLayer: ULayer[Client] = ZLayer.succeed(client)

  /**
   * Fetch EOD data for a given symbol.
   */
  def fetchEOD(symbol: String): Task[ApiEOD] = {
    val url = TwelveDataUrls.eodUrl(symbol, config)
    for {
      _ <- zio.Console.printLine(s"Fetching EOD data: ${cleanUrl(url)}")
      res <- Client.request(url).provide(defaultClientLayer)
      response <- res.body.asString
      eod <- ZIO.fromEither(response.fromJson[ApiEOD])
        .mapError(new RuntimeException(_))
    } yield eod
  }

  /** Quote has OHLC, volume */
  def fetchQuote(symbols: List[String]): Task[ApiQuote] = for {
    url <- ZIO.attempt(TwelveDataUrls.quoteUrl(symbols, config))
    _ <- zio.Console.printLine(s"Fetching quotes: ${cleanUrl(url)}")
    res <- Client.request(url).provide(defaultClientLayer)
    response <- res.body.asString
    e <- ZIO.fromEither(response.fromJson[ApiQuote]).mapError(new RuntimeException(_))
  } yield e

  def newSession(symbols: List[String]): ZIO[Scope & TwelveDataClient, Throwable, WebsocketSession] = {
    for {
      q <- Queue.unbounded[Event]
      s = ZStream.fromQueue(q)
      sess <- TwelveDataWebsocketClient.streamPrices(symbols, q)
    } yield sess
  }

  /**
   * Fetch current market price for the given symbols.
   * In TwelveData, the price response model varies depending if there are multiple tickers:
   * {{{
   * ❯ curl 'https://api.twelvedata.com/price?apikey=...&symbol=AAPL,MSFT'
   * {"AAPL":{"price":"182.00000"},"MSFT":{"price":"327.79999"}}%
   * ❯ curl 'https://api.twelvedata.com/price?apikey=...&symbol=AAPL'
   * {"price":"182.00000"}%
   * }}}
   * We convert the single symbol response to a map with a single entry so that a single type is returned from this API regardless of whether its a single ticker query or a batch one.
   *
   * TODO: handle when some symbols arent found, e.g.:
   * {{{
   *   {"SPY":{"price":"433.53000"},"IWM":{"price":"177.77500"},
   *    "NIKK":{"code":400,"message":"**symbol** not found: NIKK. Please specify it correctly according to API Documentation.","status":"error","meta":{"symbol":"SPY,IWM,NIKK,AAPL,NVDA,GOOG,AMZN,BTC/USD,ETH/USD,TSLA,MSFT,AMZN,UGA,XOP,GOLD,SLV,XLB,XLE,XLF,XLI,XLK,XLP,XLU,XLV,XLY","interval":"","exchange":""}},
   *    "AAPL":{"price":"174.90650"},"NVDA":{"price":"413.52000"},"GOOG":{"price":"132.63000"},
   *    "AMZN":{"price":"130.64010"},"BTC/USD":{"price":"26575.60000"}
   * }}}
   */
  def fetchPrices(symbols: Seq[String]): ZIO[Any, Throwable, TickerToApiPriceMap] = {
    import net.specula.twelvedata.client.model.ApiCodecs.*
    val url = TwelveDataUrls.findPriceUrl(symbols, config)
    for {
      _ <- zio.Console.printLine(s"Fetching prices: ${cleanUrl(url)}")
      res <- Client.request(url).provide(defaultClientLayer)
      response <- res.body.asString

      // If multiple tickers are being queried, the JSON in the response body format will correspond to TickerToApiPriceMap.
      // In the second case it's a flatter model, just an ApiPrice. So for the second case we convert the ApiPrice to a TickerToApiPriceMap
      // to have a consistent return type for both cases.
      multiSymbolResponse =
        symbols.toList match
          case List(singleSymbol) =>
            response.fromJson[ApiPrice].map(apiPrice => Map(singleSymbol -> apiPrice))
          case _ =>
            response.fromJson[TickerToApiPriceMap]

      e <- ZIO.fromEither(multiSymbolResponse)
        .mapError(new RuntimeException(_))
    } yield e
  }


  /** Get historical prices starting at the most recent price,in the given time intervals. Ordered by descending time
   * {{{
   *  $ curl 'https://api.twelvedata.com/time_series?symbol=AAPL,MSFT&interval=1year&outputsize=90&apikey=XXXX'
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
  def fetchTimeSeries(interval: TimeSeriesIntervalQuery): ZIO[Any, TwelveDataError, Map[String, PriceBarSeries]] =
    if (interval.outputCount > 5000) {
      ZIO.fail(TwelveDataError.InvalidQuery("Max output size must be <= 5000"))
    }else if (interval.startDate==interval.endDate) {
      ZIO.fail(TwelveDataError.InvalidQuery(s"Start date is the same as end date: ${interval.startDate}"))

    } else {
      println("Fetching data: " + interval)
      val maxOutputSize = interval.outputCount

      def fetchBatch(batchQuery: TimeSeriesIntervalQuery): ZIO[Any, TwelveDataError, Map[String, PriceBarSeries]] = {
        println("Fetching batch: " + batchQuery)
        val symbols = interval.symbols
        val baseUrl = TwelveDataUrls.baseUrl
        val apiName = interval.timeSeriesInterval.apiName
        val symbolsStr = symbols.mkString(",")

        val startDateParam = interval.startDate.map(date => s"&start_date=$date").getOrElse("")
        val endDateParam = interval.endDate.map(date => s"&end_date=$date").getOrElse("")
        val apiKeyParam = s"&apikey=${config.apiKey}"

        val url = s"$baseUrl/time_series?interval=$apiName&outputsize=${interval.outputCount}&symbol=$symbolsStr$startDateParam$endDateParam$apiKeyParam"
        for {
          _ <- zio.Console.printLine(s"Fetching batch: URL: $url").orDie

          res: Response <- Client.request(url).provide(defaultClientLayer)
            .mapError(e => TwelveDataError.RemoteException.ofMessage(e.toString))

          response <- res.body.asString.mapError(e => TwelveDataError.RemoteException.ofMessage(e.toString))
          //      _ <- Console.printLine("RESPONSE BODY: \n"+response)
          res2 <- {
            if (!res.status.isSuccess || response.contains("""{"code":4""")) {
              if (response.contains("No data is available on the specified dates.")) {
                Console.printLine(s"No data available for query: $batchQuery").orDie *>
                  ZIO.fail(TwelveDataError.NoDataForQuery(batchQuery))
              } else {
                ZIO.fail(TwelveDataError.RemoteException.ofMessage(s"Response: ${response.replaceAll("\n", "")}"))
              }
            } else {
              val remoteResponseParsed = parseResponse(response, symbols)
              ZIO.fromEither(remoteResponseParsed)
                .mapError(e => TwelveDataError.RemoteException.ofMessage(e))
                .map(_.map { case (k, v) => k -> v }.toMap)
            }
          }
        } yield {
          res2
        }
      }

      // Calculate total expected bars and number of batches
      val totalExpectedBars = interval.outputCount
      val batches = (totalExpectedBars.toDouble / maxOutputSize).ceil.toInt

      // Generate list of batch queries
      val batchQueries = (1 to batches).toList.map { batchNum =>
        val batchSize = if (batchNum == batches) totalExpectedBars - (maxOutputSize * (batches - 1)) else maxOutputSize
        interval.copy(outputCount = batchSize) // Modify other parameters if needed for pagination, like startDate or endDate
      }

      // Fetch all batches and combine results
      val fetchTasks: List[ZIO[Any, TwelveDataError, Map[String, PriceBarSeries]]] = batchQueries.map(fetchBatch)

      ZIO.collectAll(fetchTasks).map { results =>
        results.flatten.toMap
      }
    }

  def fetchHistoricalData(req: TwelveDataComplexDataRequest): Task[TwelveDataHistoricalDataBatchResponse] = {
    import net.specula.twelvedata.client.model.json.JsonCodecs.*
    import zio.json.*

    val url = TwelveDataUrls.baseUrl + s"/complex_data?apikey=${config.apiKey}"

    for {
      //      _ <- zio.Console.printLine(s"fetching historical: $url")

      res <- Client.request(url, method = Method.POST, content = Body.fromString(req.toJson))
        .provide(defaultClientLayer)

      responseString <-
        if (res.status.isSuccess) {
          res.body.asString
        } else {
          Console.printLine(s"ERROR IN RESPONSE") *>
            Console.printLine(s"-----------------") *>
            Console.printLine(s"ERROR: Response headers was: ${res.headers.mkString(",")}") *>
            Console.printLine(s"ERROR: Response status was: ${res.status}") *>
            Console.printLine(s"ERROR: Request JSON body was: \n${req.toJson}") *>
            Console.printLine(s"ERROR: Response JSON body was: \n${res.body.asString}") *>
            ZIO.fail(new RuntimeException(s"Error fetching historical data: ${res.status} ${res.body.asString}"))
        }

      remoteResponseParsed <-
        ZIO.fromEither(responseString.fromJson[TwelveDataHistoricalDataBatchResponse])
          .mapError(new RuntimeException(_))
          .tapError(_ => Console.printLine(s"ERROR: Request JSON body was: ${req.toJson}"))
          .tapError(_ => Console.printLine(s"ERROR: Response JSON body was: ${responseString.replaceAll("\n", "")}"))
    } yield remoteResponseParsed
  }

  /**
   * as in other endpoints, the response body format varies depending on whether multiple tickers are being queried.
   * when multiple `Symbol`s are provided, the response body is a `Map` of `Symbol`s to `TimeSeriesItems`.
   * when a single `Symbol` is provided, the response body is just the single `TimeSeriesItems`.
   *
   * @param response Response body from the API
   * @param symbols  tickers we asked to look up in the request (assumes the response has the same tickers?)
   * @return
   */
  private def parseResponse(response: String, symbols: List[String]): Either[String, TickerToTimeSeriesItemMap] = {
    symbols match {
      case List(singleSymbol) =>
        response.fromJson[PriceBarSeries]
          .map(timeSeriesItem => Map(singleSymbol -> timeSeriesItem))
      case Nil =>
        sys.error("Symbols required")
      case _ =>
        response.fromJson[TickerToTimeSeriesItemMap]
    }
  }



    def fetchOptionExpirations(symbol: String): Task[OptionExpirations] = {
      val baseUrl = TwelveDataUrls.baseUrl
      val apiKeyParam = s"&apikey=${config.apiKey}"

      val url = s"$baseUrl/options/expiration?symbol=$symbol$apiKeyParam"

      for {
        _ <- zio.Console.printLine(s"Fetching option expirations: ${cleanUrl(url)}")
        res <- Client.request(url).provide(defaultClientLayer)
        response <- res.body.asString
        expirations <- ZIO.fromEither(response.fromJson[OptionExpirations])
          .mapError(new RuntimeException(_))
      } yield expirations
    }

    def fetchOptionChain(symbol: String, expiration_date: String): Task[OptionData] = {
      val baseUrl = TwelveDataUrls.baseUrl
      val apiKeyParam = s"&apikey=${config.apiKey}"
      val expirationParam = s"&expiration_date=$expiration_date"

      val url = s"$baseUrl/options/chain?symbol=$symbol$expirationParam$apiKeyParam"

      for {
        _ <- zio.Console.printLine(s"Fetching option chain: ${cleanUrl(url)}")
        res <- Client.request(url).provide(defaultClientLayer)
        response <- res.body.asString
        optionsData <- ZIO.fromEither(response.fromJson[OptionData])
          .mapError(new RuntimeException(_))
      } yield optionsData
    }

}

object TwelveDataClient:

  // ZIO Service pattern - templates for invoking methods of the TwelveDataClient instance from the ZIO environment - https://zio.dev/reference/service-pattern/

  def fetchQuote(symbols: List[String]): ZIO[TwelveDataClient, Throwable, ApiQuote] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchQuote(symbols))

  /** Fetch historical data using the time_series endpoint */
  def fetchTimeSeries(intervalQuery: TimeSeriesIntervalQuery): ZIO[TwelveDataClient, TwelveDataError, Map[String, PriceBarSeries]] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchTimeSeries(intervalQuery))

  /** Fetch data using the complex_data endpoint */
  def fetchComplexData(request: TwelveDataComplexDataRequest): ZIO[TwelveDataClient, Throwable, TwelveDataHistoricalDataBatchResponse] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchHistoricalData(request))

  def fetchPrices(tickers: String*): ZIO[TwelveDataClient, Throwable, TickerToApiPriceMap] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchPrices(tickers))

  def newSession(tickers: List[String]): ZIO[Scope & TwelveDataClient & TwelveDataClient, Throwable, WebsocketSession] =
    ZIO.serviceWithZIO[TwelveDataClient](_.newSession(tickers))

  /** A ZLayer that creates a TwelveDataClient from its requirements in the Environment */
  lazy val live: ZLayer[Client & TwelveDataConfig, Nothing, TwelveDataClient] =
    ZLayer {
      for {
        requiredClient <- ZIO.service[Client]
        requiredConfig <- ZIO.service[TwelveDataConfig]
      } yield TwelveDataClient(requiredClient, requiredConfig)
    }


  def fetchEOD(symbol: String): ZIO[TwelveDataClient, Throwable, ApiEOD] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchEOD(symbol))


  def fetchOptionExpirations(symbol: String): ZIO[TwelveDataClient, Throwable, OptionExpirations] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchOptionExpirations(symbol))

  def fetchOptionChain(symbol: String, expiration_date: String): ZIO[TwelveDataClient, Throwable, OptionData] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchOptionChain(symbol, expiration_date))

end TwelveDataClient
