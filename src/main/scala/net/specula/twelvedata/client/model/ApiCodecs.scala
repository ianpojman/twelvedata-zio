package net.specula.twelvedata.client.model

import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class ApiPrice private[client](price: Double)

//{"SPX":{"price":"4169.77000"},"AAPL":{"price":"169.67000"}}
object ApiCodecs:
  implicit val priceDecoder: JsonDecoder[ApiPrice] = DeriveJsonDecoder.gen
end ApiCodecs
