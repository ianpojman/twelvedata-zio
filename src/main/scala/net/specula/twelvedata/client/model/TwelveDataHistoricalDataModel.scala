package net.specula.twelvedata.client.model

import java.time.{Instant, ZonedDateTime}
import java.time.format.DateTimeFormatter

/*
The classes in this file represent a 1:1 mapping of the JSON response model from Twelvedata, including fields which match
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
 * @param end_date Similar as start_date
 * @param methods Accepts: "time_series", "quote", "price" or any of the technical indicators (e.g. "ema"). TODO: • Could be either a string with method name, then default parameters are taken. Alternatively, with object you might specify custom parameters.
  * @param outputsize Number of results to return. Default is 30
 * */
case class TwelveDataHistoricalDataRequest(
                                            symbols: List[String],
                                            intervals: List[String],
                                            methods: List[Map[String, Int]],
                                            start_date: Option[Instant],
                                            end_date: Option[Instant],
                                            outputsize: Int, // default on server side is 30
                                          )

// can be either a json object ("ema" -> 10), or a string like "time_series"
sealed trait ComplexDataRequestMethod {
  def apiName: String
  def asMap: Map[String, Int] = Map(apiName -> 0) // todo support objects such as ema->10
}
object ComplexDataRequestMethod {
  abstract class NamedComplexDataRequestMethod(override val apiName: String) extends ComplexDataRequestMethod
  case object TimeSeriesMethod extends NamedComplexDataRequestMethod("time_series")
  case object QuoteMethod extends NamedComplexDataRequestMethod("quote")
  case object PriceMethod extends NamedComplexDataRequestMethod("price")
  case class OtherMethod(otherName: String) extends NamedComplexDataRequestMethod(otherName) // for something not yet added above
}

object TwelveDataHistoricalDataRequest:
  /** More typeful constructor that generates a valid request object */
  def apply(symbols: List[String],
            intervals: List[TimeSeriesInterval],
            startTime: Instant,
            methods: List[ComplexDataRequestMethod], //List(Map("ema" ->  10)),
            endTime: Option[Instant] = None,
            outputSize: Int = 30,
           ): TwelveDataHistoricalDataRequest = {

    TwelveDataHistoricalDataRequest(
      symbols,
      intervals.map(_.apiName),
      methods.map(_.asMap),
      Some(startTime),
      endTime,
      outputSize
    )
  }
end TwelveDataHistoricalDataRequest

// response model

/** Indicator data which is optional and may be included as part of the complex_data endpoint response */
case class Indicator(name: String, series_type: String, time_period: Int)

/** Contains metadata about the query, including data about the exchange, the interval used, and any relevant indicator data */
case class ResponseMetadata(
                 symbol: String,
                 interval: String,
                 currency: String,
                 exchange_timezone: String,
                 exchange: String,
                 mic_code: String,
                 `type`: String,
                 indicator: Option[Indicator] // this is optional because not all elements have this field
               )

/** Represents a price bar or an ema value as returned in Twelvedata's JSON response model */
case class Value(
                  datetime: String,
                  open: Option[String], // these are optional because not all elements have these fields
                  high: Option[String],
                  low: Option[String],
                  close: Option[String],
                  volume: Option[String],
                  ema: Option[String]
                ) {
  def instant(timeZone: String): Instant = Value.dateTimeToInstant(this.datetime, timeZone)

}

object Value:
  def dateTimeToInstant(dateTime: String, timeZone: String): Instant = {
    val combinedString = s"$dateTime $timeZone"
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
    ZonedDateTime.parse(combinedString, dateTimeFormatter).toInstant
  }

end Value

/** A list of values which are either price bars (timestamped OHLC data) or ema data. */
case class DataElement(
                        meta: ResponseMetadata,
                        values: List[Value],
                        status: String
                      )

case class TwelveDataHistoricalDataResponse(
                               data: Option[List[DataElement]], // server will return null if no data matches, hence this is Option
                               status: String) {
  def dataList: List[DataElement] = data.getOrElse(Nil)
}