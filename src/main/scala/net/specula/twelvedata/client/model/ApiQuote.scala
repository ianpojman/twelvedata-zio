package net.specula.twelvedata.client.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder, SnakeCase, jsonField, jsonMemberNames}

/**
 * Example JSON
 * {{{
 *
 *{
 *"symbol": "NDX",
 *"name": "NASDAQ 100",
 *"exchange": "NASDAQ",
 *"mic_code": "XNGS",
 *"currency": "USD",
 *"datetime": "2023-04-27",
 *"timestamp": 1682625597,
 *"open": "12963.20020",
 *"high": "13175.61816",
 *"low": "12938.49805",
 *"close": "13160.02637",
 *"volume": "1195050000",
 *"previous_close": "12806.48047",
 *"change": "353.54590",
 *"percent_change": "2.76068",
 *"average_volume": "4489910000",
 *"is_market_open": false,
 *"fifty_two_week": {
 *"low": "10440.63965",
 *"high": "13720.91016",
 *"low_change": "2719.38672",
 *"high_change": "-560.88379",
 *"low_change_percent": "26.04617",
 *"high_change_percent": "-4.08780",
 *"range": "10440.639648 - 13720.910156"
 *}
 *}
 * }}}
 */
@jsonMemberNames(SnakeCase)
case class ApiQuote private[client](symbol: String,
                                    name: String,
                                    exchange: String,
                                    datetime: String,
                                    timestamp: Long,
                                    open: Double,
                                    close: Double,
                                    low: Double,
                                    high: Double,
                                    volume: Option[Long], // volume should be there for most securities but not for things like FX pairs
                                    previousClose: String,
                                    change: Double,
                                    percentChange: Double,
                                    averageVolume: Double)

object ApiQuote:
  implicit val decoder: JsonDecoder[ApiQuote] = DeriveJsonDecoder.gen
end ApiQuote


