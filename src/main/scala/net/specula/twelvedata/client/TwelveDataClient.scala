package net.specula.twelvedata.client

import net.specula.twelvedata.client.TwelveDataUrls.cleanUrl
import net.specula.twelvedata.client.model.*
import net.specula.twelvedata.client.websocket.{TwelveDataWebsocketClient, WebsocketSession}
import zio.*
import zio.http.{Body, Client, Method, Response}
import zio.stream.{ZSink, ZStream}

/** Client for Twelvedata HTTP API */
object TwelveDataClient {

  import net.specula.twelvedata.client.model.json.JsonCodecs.*
  import zio.json.*

  /**
   * Fetch EOD data for a given symbol.
   */
  def fetchEOD(symbol: String): RIO[TwelveDataConfig & Client, ApiEOD] = {
    for {
      config <- ZIO.service[TwelveDataConfig]
      url = TwelveDataUrls.eodUrl(symbol, config)
      _ <- ZIO.logDebug(s"Fetching EOD data: ${cleanUrl(url)}")
      res <- Client.request(url)
      response <- res.body.asString
      eod <- ZIO.fromEither(response.fromJson[ApiEOD])
        .mapError(new RuntimeException(_))
    } yield eod
  }

  def newSession(symbols: List[String]): RIO[Scope & Client & TwelveDataConfig, WebsocketSession] = {
    for {
      q <- Queue.unbounded[Event]
      s = ZStream.fromQueue(q)
      sess <- TwelveDataWebsocketClient.streamPrices(symbols, q)
    } yield sess
  }


  def fetchQuote(symbol: String): RIO[TwelveDataConfig & Client, ApiQuote] =
    fetchQuotes(List(symbol)).map(_.values.head)

  /** Quote returns a bar (OHLC, volume) */
  def fetchQuotes(symbols: List[String], useExtendedPrices: Boolean = false): RIO[TwelveDataConfig & Client, TickerToApiQuoteMap] = {
    val quotesMap = for {
      config <- ZIO.service[TwelveDataConfig]
      url <- ZIO.attempt(TwelveDataUrls.quoteUrl(symbols, config))
      _ <- ZIO.logDebug(s"Fetching ${symbols.size} quote(s): ${cleanUrl(url)}")
      res <- Client.request(url)
      response <- res.body.asString

      quoteResponse <-
        (symbols match {
          case List(singleSymbol) =>
            ZIO.fromEither(response.fromJson[ApiQuote].map(apiQuote => Map(singleSymbol -> apiQuote)))
          case _ =>
            ZIO.fromEither(response.fromJson[TickerToApiQuoteMap])
        })
          .mapError(new RuntimeException(_))
          .tapError(_ => ZIO.logDebug(s"ERROR: Response JSON body was: ${response.replaceAll("\n", "")}"))

    } yield quoteResponse

    // apply extended prices if applicable if market is closed and extended price is present
    quotesMap.map { quoteMap =>
      quoteMap.map { case (symbol, quote) =>
        val adjustedQuote =
          if (!quote.isMarketOpen) {
            val extendedPrice = quote.extendedPrice.getOrElse(quote.close)
            quote.copy(close = extendedPrice) // overwrite close with extended price if market's closed
          } else {
            quote
          }
        symbol -> adjustedQuote
      }
    }
  }

  def fetchPrice(symbol: String): RIO[TwelveDataConfig & Client, ApiPrice] =
    fetchPrices(List(symbol)).map(_.values.head)


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
  def fetchPrices(symbols: Seq[String]): RIO[TwelveDataConfig & Client, TickerToApiPriceMap] = {
    import net.specula.twelvedata.client.model.ApiCodecs.*
    for {
      config <- ZIO.service[TwelveDataConfig]
      url = TwelveDataUrls.findPriceUrl(symbols, config)
      _ <- ZIO.logDebug(s"Fetching prices: ${cleanUrl(url)}")
      res <- Client.request(url)
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
        .tapError(_ => ZIO.logDebug(s"ERROR: Response JSON body was: ${response.replaceAll("\n", "")}"))

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
   **/
  def fetchTimeSeries(interval: TimeSeriesIntervalQuery): ZIO[TwelveDataConfig & Client, TwelveDataError, Map[String, PriceBarSeries]] =

    if (interval.outputCount > 5000) {
      ZIO.fail(TwelveDataError.InvalidQuery("Max output size must be <= 5000"))
    } else if (interval.startDate == interval.endDate) {
      ZIO.fail(TwelveDataError.InvalidQuery(s"Start date is the same as end date: ${interval.startDate}"))
    } else {
      //println("Fetching data: " + interval)
      val maxOutputSize = interval.outputCount

      fetchBatches(interval, maxOutputSize)
      //.provide(defaultClientLayer) // TODO externalize this to the caller

    }

  /** Subdivides the query into max batch sizes supported by Twelvedata. Fetches all the data from the requested time
   * frame and then combines the results. Currently its all loaded into memory, I should try to make it stream instead,
   * but mostly doing daily candles for now, so it doesn't matter yet */
  private def fetchBatches(interval: TimeSeriesIntervalQuery,
                           maxOutputSize: RuntimeFlags): ZIO[TwelveDataConfig & Client, TwelveDataError, Map[String, PriceBarSeries]] = {

    def fetchBatch(batchQuery: TimeSeriesIntervalQuery) = {
      //println("Fetching batch: " + batchQuery)
      val symbols = interval.symbols
      val baseUrl = TwelveDataUrls.baseUrl
      val apiName = interval.timeSeriesInterval.apiName
      val symbolsStr = symbols.mkString(",")

      val startDateParam = interval.startDate.map(date => s"&start_date=$date").getOrElse("")
      val endDateParam = interval.endDate.map(date => s"&end_date=$date").getOrElse("")

      for {
        config <- ZIO.service[TwelveDataConfig]
        apiKeyParam = s"&apikey=${config.apiKey}"
        url = s"$baseUrl/time_series?interval=$apiName&outputsize=${interval.outputCount}&symbol=$symbolsStr$startDateParam$endDateParam$apiKeyParam"
        _ <- zio.ZIO.logDebug(s"Fetching batch: URL: $url")
        res: Response <- Client.request(url)
          .mapError(e => TwelveDataError.RemoteException.ofMessage(e.toString))

        response <- res.body.asString.mapError(e => TwelveDataError.RemoteException.ofMessage(e.toString))
        //      _ <- ZIO.logDebug("RESPONSE BODY: \n"+response)
        res2 <- {
          if (!res.status.isSuccess || response.contains("""{"code":4""")) {
            if (response.contains("No data is available on the specified dates.")) {
              ZIO.logDebug(s"No data available for query: $batchQuery") *>
                ZIO.fail(TwelveDataError.NoDataForQuery(batchQuery))
            } else {
              ZIO.fail(TwelveDataError.RemoteException.ofMessage(s"Response: ${response.replaceAll("\n", "")}"))
            }
          } else {
            val remoteResponseParsed = parseResponse(response, symbols)
            ZIO.fromEither(remoteResponseParsed)
              .mapError(e => TwelveDataError.RemoteException.ofMessage(e))
              .map(_.map { case (k, v) => k -> v }.toMap)
              .tapError(_ => ZIO.logDebug(s"ERROR: Response JSON body was: ${response.replaceAll("\n", "")}"))
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

    ZIO.foreach(batchQueries)(fetchBatch).map { batchResults =>
      batchResults
        .flatten
        .groupBy(_._1)
        .view
        .mapValues(seriesList =>
          seriesList.flatMap(_._2.values).sortBy(_.datetime) // Directly sort the merged list of values
        )
        .toMap
        .map { case (symbol, sortedValues) => symbol -> PriceBarSeries(sortedValues) }
    }
  }

  def fetchHistoricalData(req: TwelveDataComplexDataRequest): RIO[TwelveDataConfig & Client, TwelveDataHistoricalDataBatchResponse] = {
    import net.specula.twelvedata.client.model.json.JsonCodecs.*
    import zio.json.*

    for {
      //      _ <- zio.ZIO.logDebug(s"fetching historical: $url")

      config <- ZIO.service[TwelveDataConfig]
      url = TwelveDataUrls.baseUrl + s"/complex_data?apikey=${config.apiKey}"

      res <- Client.request(url, method = Method.POST, content = Body.fromString(req.toJson))

      responseString <-
        if (res.status.isSuccess) {
          res.body.asString
        } else {
          ZIO.logDebug(s"ERROR IN RESPONSE") *>
            ZIO.logDebug(s"-----------------") *>
            ZIO.logDebug(s"ERROR: Response headers was: ${res.headers.mkString(",")}") *>
            ZIO.logDebug(s"ERROR: Response status was: ${res.status}") *>
            ZIO.logDebug(s"ERROR: Request JSON body was: \n${req.toJson}") *>
            ZIO.logDebug(s"ERROR: Response JSON body was: \n${res.body.asString}") *>
            ZIO.fail(new RuntimeException(s"Error fetching historical data: ${res.status} ${res.body.asString}"))
        }

      remoteResponseParsed <-
        ZIO.fromEither(responseString.fromJson[TwelveDataHistoricalDataBatchResponse])
          .mapError(new RuntimeException(_))
          .tapError(_ => ZIO.logDebug(s"ERROR: Request JSON body was: ${req.toJson}"))
          .tapError(_ => ZIO.logDebug(s"ERROR: Response JSON body was: ${responseString.replaceAll("\n", "")}"))
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


  def fetchOptionExpirations(symbol: String): RIO[Client & TwelveDataConfig, OptionExpirations] = {
    val baseUrl = TwelveDataUrls.baseUrl


    for {
      config <- ZIO.service[TwelveDataConfig]
      apiKeyParam = s"&apikey=${config.apiKey}"
      url = s"$baseUrl/options/expiration?symbol=$symbol$apiKeyParam"
      _ <- zio.ZIO.logDebug(s"Fetching option expirations: ${cleanUrl(url)}")
      res <- Client.request(url)
      response <- res.body.asString
      expirations <- ZIO.fromEither(response.fromJson[OptionExpirations])
        .mapError(new RuntimeException(_))
        .tapError(_ => ZIO.logDebug(s"ERROR: Response JSON body was: ${response.replaceAll("\n", "")}"))
    } yield expirations
  }

  def fetchOptionChain(symbol: String, expiration_date: String): RIO[TwelveDataConfig & Client, OptionData] = {
    val baseUrl = TwelveDataUrls.baseUrl
    val expirationParam = s"&expiration_date=$expiration_date"

    for {
      config <- ZIO.service[TwelveDataConfig]
      apiKeyParam = s"&apikey=${config.apiKey}"
      url = s"$baseUrl/options/chain?symbol=$symbol$expirationParam$apiKeyParam"
      _ <- ZIO.logDebug(s"Fetching option chain: ${cleanUrl(url)}")
      res <- Client.request(url)
      response <- res.body.asString
      optionsData <- ZIO.fromEither(response.fromJson[OptionData])
        .mapError(new RuntimeException(_))
        .tapError(_ => ZIO.logDebug(s"ERROR: Response JSON body was: ${response.replaceAll("\n", "")}"))
    } yield optionsData
  }
}