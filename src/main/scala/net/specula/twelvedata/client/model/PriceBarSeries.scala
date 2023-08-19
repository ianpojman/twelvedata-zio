package net.specula.twelvedata.client.model

case class PriceBarSeries(values: List[PriceBar])

case class PriceBar(datetime: String,
                    open: Double,
                    high: Double,
                    low: Double,
                    close: Double,
                    volume: Option[Double])
