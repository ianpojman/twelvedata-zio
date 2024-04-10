package net.specula.twelvedata.client.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder, SnakeCase, jsonField, jsonMemberNames}

/**
 * Example JSON
 * {{{
 * {
 *   "symbol": "AAPL",
 *   "name": "Apple Inc",
 *   "exchange": "NASDAQ",
 *   "mic_code": "XNGS",
 *   "currency": "USD",
 *   "datetime": "2024-04-09",
 *   "timestamp": 1712748809,
 *   "open": "168.70000",
 *   "high": "170.08000",
 *   "low": "168.35001",
 *   "close": "169.67000",
 *   "volume": "42373800",
 *   "previous_close": "168.45000",
 *   "change": "1.22000",
 *   "percent_change": "0.72425",
 *   "average_volume": "50215500",
 *   "is_market_open": false,
 *   "fifty_two_week": {
 *     "low": "159.78000",
 *     "high": "199.62000",
 *     "low_change": "9.89000",
 *     "high_change": "-29.95000",
 *     "low_change_percent": "6.18976",
 *     "high_change_percent": "-15.00351",
 *     "range": "159.779999 - 199.619995"
 *   },
 *   "extended_change": "0.65000",
 *   "extended_percent_change": "0.38310",
 *   "extended_price": "170.32000",
 *   "extended_timestamp": 1712669400
 * }
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
                                    averageVolume: Double,
                                    isMarketOpen: Boolean,
                                    extendedPrice: Option[Double])

object ApiQuote:
  implicit val decoder: JsonDecoder[ApiQuote] = DeriveJsonDecoder.gen
end ApiQuote


