package net.specula.twelvedata.client.model

import java.time.Instant


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
 * */
case class TwelveDataHistoricalDataRequest(
                                            symbols: List[String],
                                            intervals: List[String],
                                            outputsize: Int,
                                            methods: List[Map[String, Int]],
                                            start_date: Option[Instant],
                                            end_date: Option[Instant]
                                          )

// response model
case class Indicator(name: String, series_type: String, time_period: Int)

case class Meta(
                 symbol: String,
                 interval: String,
                 currency: String,
                 exchange_timezone: String,
                 exchange: String,
                 mic_code: String,
                 `type`: String,
                 indicator: Option[Indicator] // this is optional because not all elements have this field
               )

case class Value(
                  datetime: String,
                  open: Option[String], // these are optional because not all elements have these fields
                  high: Option[String],
                  low: Option[String],
                  close: Option[String],
                  volume: Option[String],
                  ema: Option[String]
                )

case class DataElement(
                        meta: Meta,
                        values: List[Value],
                        status: String
                      )

case class TwelveDataHistoricalDataResponse(
                               data: List[DataElement],
                               status: String
                             )