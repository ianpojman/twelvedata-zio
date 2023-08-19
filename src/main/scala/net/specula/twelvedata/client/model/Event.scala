package net.specula.twelvedata.client.model

import net.specula.twelvedata.client.model.PriceBarSeries
import zio.json.{DeriveJsonDecoder, JsonDecoder, SnakeCase, jsonMemberNames}

/**  event from websocket stream
 * example json:
 * {{{
 * {"event":"price","symbol":"AAPL","currency":"USD","exchange":"NASDAQ","mic_code":"XNGS","type":"Common Stock","timestamp":1684508044,"price":175.6997,"day_volume":15991537}
 * }}}
 * */
@jsonMemberNames(SnakeCase)
case class Event(event: String,
                 status: Option[String],
                 symbol: Option[String],
                 currency: Option[String],
                 exchange: Option[String],
                 micCode: Option[String],
                 `type`: Option[String],
                 timestamp: Option[Long],
                 price: Option[Double],
                 dayVolume: Option[Double])

object EventCodecs {
  implicit val eventDecoder: JsonDecoder[Event] = DeriveJsonDecoder.gen
}
