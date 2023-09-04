package net.specula.twelvedata.client

import net.specula.twelvedata.client.TwelveDataUrls.cleanUrl
import net.specula.twelvedata.client.model.*
import net.specula.twelvedata.client.websocket.{TwelveDataWebsocketClient, WebsocketSession}
import zio.*
import zio.http.{Body, Client, Method}
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
  def fetchTimeSeries(interval: TimeSeriesIntervalQuery): Task[Map[String, PriceBarSeries]] =
    if (interval.outputCount > 5000) {
      ZIO.fail(new RuntimeException("Max output size must be <= 5000"))
    } else {
      println("Fetching data: " + interval)
      val maxOutputSize = interval.outputCount

      def fetchBatch(batchInterval: TimeSeriesIntervalQuery): Task[Map[String, PriceBarSeries]] = {
        println("Fetching batch: " + batchInterval)
        val symbols = interval.symbols
        val baseUrl = TwelveDataUrls.baseUrl
        val apiName = interval.timeSeriesInterval.apiName
        val symbolsStr = symbols.mkString(",")

        val startDateParam = interval.startDate.map(date => s"&start_date=$date").getOrElse("")
        val endDateParam = interval.endDate.map(date => s"&end_date=$date").getOrElse("")
        val apiKeyParam = s"&apikey=${config.apiKey}"

        val url = s"$baseUrl/time_series?interval=$apiName&outputsize=${interval.outputCount}&symbol=$symbolsStr$startDateParam$endDateParam$apiKeyParam"
        for {
          //      _ <- zio.Console.printLine(s"Time series request URL: $url")
          res <- Client.request(url).provide(defaultClientLayer)
          response <- res.body.asString
          //      _ <- Console.printLine("RESPONSE BODY: \n"+response)
          _ <- {
            if (!res.status.isSuccess || response.contains("""{"code":4""")) // it seems 200 status will be returned even if the request is invalid, so we check for this specific error code
              ZIO.fail(new RuntimeException(s"Response: ${response.replaceAll("\n", "")}"))
            else
              ZIO.unit
          }
          remoteResponseParsed = parseResponse(response, symbols)

          // convert the twelvedata api either to a ZIO function
          res2 <- ZIO.fromEither(remoteResponseParsed)
            .mapError(new RuntimeException(_))
            .map(_.map { case (k, v) => k -> v }.toMap)
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
      val fetchTasks: List[Task[Map[String, PriceBarSeries]]] = batchQueries.map(fetchBatch)

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


}

object TwelveDataClient:

  // ZIO Service pattern - templates for invoking methods of the TwelveDataClient instance from the ZIO environment - https://zio.dev/reference/service-pattern/

  def fetchQuote(symbols: List[String]): ZIO[TwelveDataClient, Throwable, ApiQuote] =
    ZIO.serviceWithZIO[TwelveDataClient](_.fetchQuote(symbols))

  /** Fetch historical data using the time_series endpoint */
  def fetchTimeSeries(intervalQuery: TimeSeriesIntervalQuery): ZIO[TwelveDataClient, Throwable, Map[String, PriceBarSeries]] =
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

end TwelveDataClient
