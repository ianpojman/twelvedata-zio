package net.specula.twelvedata

import net.specula.twelvedata.client.model.Event

case class Price(symbol: String, price: Double, timestamp: Long, volume: Double)

object Price:
  def fromEvent(e: Event): Option[Price] = e match {
    case Event(_, _, Some(symbol), _, _, _, _, Some(timestamp), Some(price), Some(dayVolume)) =>
      Some(Price(symbol, price, timestamp, dayVolume))
    case other => None
  }

end Price
