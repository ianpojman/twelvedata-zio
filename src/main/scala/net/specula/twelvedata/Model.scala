package net.specula.twelvedata

import net.specula.twelvedata.client.model.Event

case class Price(symbol: String, price: Double, timestamp: Long, dayVolume: Option[Double])

object Price:
  def fromEvent(e: Event): Option[Price] = e match {
    case Event(_, _, Some(symbol), _, _, _, _, Some(timestamp), Some(price), dayVolume) =>
      Some(Price(symbol, price, timestamp, dayVolume))
    case other => 
      None
  }

end Price
