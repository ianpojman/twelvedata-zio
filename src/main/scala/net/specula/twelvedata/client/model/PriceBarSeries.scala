package net.specula.twelvedata.client.model

import java.time.Instant

case class PriceBarSeries(values: List[PriceBar])

case class PriceBar(datetime: String,
                    open: Double,
                    high: Double,
                    low: Double,
                    close: Double,
                    volume: Option[Double]) {

  def instant(timeZone: String): Instant =
    TwelveDataDateTimeHelpers.localDateToInstant(this.datetime, timeZone)

}
