package net.specula.twelvedata.client.model


case class PriceResponse(symbol: String, price: Double, timestamp: Long, dayVolume: Option[Double])

object PriceResponse:
  def fromEvent(e: Event): Option[PriceResponse] = e match {
    case Event(_, _, Some(symbol), _, _, _, _, Some(timestamp), Some(price), dayVolume) =>
      Some(PriceResponse(symbol, price, timestamp, dayVolume))
    case other =>
      None
  }

end PriceResponse

