package net.specula.twelvedata.client.model

case class TimeSeriesItems(values: List[TimeSeriesItem])

case class TimeSeriesItem(datetime: String,
                          open: Double,
                          high: Double,
                          low: Double,
                          close: Double,
                          volume: Option[Double])
