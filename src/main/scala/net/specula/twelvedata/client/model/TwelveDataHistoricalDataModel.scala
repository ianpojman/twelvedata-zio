package net.specula.twelvedata.client.model

import net.specula.twelvedata.client.rest.{ComplexMethod, ComplexMethodList}
import zio.http.html.meta

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

/*
WARNING: The classes in this file represent a 1:1 mapping of the JSON response model from Twelvedata, including fields which match
 the field names of the JSON. JSON codecs are generated from these classes, so the structure and names cannot be changed,
  or the response won't be parsed correctly.
 */

/** Fetch historical time series data from the given start/end points
 * using the complex_data endpoint.
 * {{{
 *  $ https://api.twelvedata.com/complex_data?apikey=your_api_key
 *    --header "Content-Type: application/json" \
 *    --request POST \
 *    --data '{
 *    "symbols": ["AAPL", "MSFT", "GOOG"],
 *    "intervals": ["5min", "1day"],
 *    "outputsize": 25,
 *    "methods": [
 *    "time_series",
 *    {
 *    "name": "ema",
 *    "time_period": 12
 *    }]
 *    }'
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
 *
 * @param start_date Format 2006-01-02 or 2006-01-02 15:04:05 ... If timezone is given then, start_date will be used in the specified location
 * @param end_date   Similar as start_date
 * @param methods    Accepts: "time_series", "quote", "price" or any of the technical indicators (e.g. "ema"). TODO: • Could be either a string with method name, then default parameters are taken. Alternatively, with object you might specify custom parameters.
 * @param outputsize Number of results to return. Default is 30
 * */
case class TwelveDataComplexDataRequest(symbols: List[String],
                                        intervals: List[TimeSeriesInterval],
                                        methods: ComplexMethodList = ComplexMethodList(Nil),
                                        outputsize: Int = 30, // default on server side is 30
                                        start_date: Option[LocalDate] = None,
                                        end_date: Option[LocalDate] = None,
                                        timezone: Option[String] = None, // e.g. "America/New_York"
                                          )

object TwelveDataComplexDataRequest:
end TwelveDataComplexDataRequest

/** Indicator data which is optional and may be included as part of the complex_data endpoint response */
case class Indicator(name: String, series_type: String, time_period: Int)

/** Contains metadata about the query, including data about the exchange, the interval used, and any relevant indicator data */
case class ResponseElementMetadata(symbol: String,
                                   interval: String,
                                   currency: String,
                                   exchange_timezone: String,
                                   exchange: String,
                                   mic_code: String,
                                   `type`: String,
                                   indicator: Option[Indicator] // this is optional because not all elements have this field
                                  ) {
  def timeSeriesInterval: TimeSeriesInterval = TimeSeriesInterval.fromString(this.interval)
    .getOrElse(sys.error(s"Unknown interval: $interval"))
}

/** Represents a price bar or an ema value as returned in Twelvedata's JSON response model */
case class ResponseElementValues(datetime: String,
                                 open: Option[String], // these are optional because not all elements have these fields
                                 high: Option[String],
                                 low: Option[String],
                                 close: Option[String],
                                 volume: Option[String],
                                 ema: Option[String]
                                ) {
  def closeValue: Option[Double] = close.map(_.toDouble)
  def openValue: Option[Double] = open.map(_.toDouble)


  def instant(timeZone: String): Instant =
    TwelveDataDateTimeHelpers.localDateToInstant(this.datetime, timeZone)

  def toPriceBar(timezone: String): Option[PriceBar] = for {
    o <- open
    h <- high
    l <- low
    c <- close
    v <- volume
  } yield PriceBar(datetime, o.toDouble, h.toDouble, l.toDouble, c.toDouble, Some(v.toDouble))
}


/** A list of values which are either price bars (timestamped OHLC data) or ema data.
 * NOTE: A [[zio.json.JsonDecoder]] is generated based on this class, so the structure and names cannot be changed, or the response won't be parsed correctly.
 */
case class TwelveDataHistoricalDataResponse(meta: ResponseElementMetadata,
                                            values: List[ResponseElementValues],
                                            status: String) {
  def firstValue: Option[ResponseElementValues] = values.headOption
  def lastValue: Option[ResponseElementValues] = values.lastOption

}

/** In the Twelvedata API, each "method" generates a different response, which we call are calling [[TwelveDataHistoricalDataResponse]] here.
 * NOTE: A [[zio.json.JsonDecoder]] is generated based on this class, so the structure and names cannot be changed, or the response won't be parsed correctly.
 *
 * @param data Responses by symbol(/method?) */
case class TwelveDataHistoricalDataBatchResponse(data: Option[List[TwelveDataHistoricalDataResponse]],
                                                 status: String) {


  def valuesForSymbol(symbol: String): Option[List[ResponseElementValues]] =
    responseBySymbol.get(symbol).map(_.values)

  def responseBySymbol: Map[String, TwelveDataHistoricalDataResponse] =
    data.getOrElse(Nil).map { e =>
      e.meta.symbol -> e
    }.toMap

  def dataList: List[TwelveDataHistoricalDataResponse] = data.getOrElse(Nil)
}