package net.specula.twelvedata.client


import zio.*

import java.time.{Instant, ZoneOffset}

/** Query for time series data, from the current time backwards, at the interval specified */
case class TimeSeriesIntervalQuery(symbols: List[Symbol],
                                   timeSeriesInterval: TimeSeriesInterval)


sealed abstract class TimeSeriesInterval(val apiName: String)
object TimeSeriesInterval {
  def fromStrings(s: String*): List[Symbol] = s.map(Symbol.fromString(_)).toList
  
  case object OneMinute extends TimeSeriesInterval("1min")
  case object FiveMinutes extends TimeSeriesInterval("5min")
  case object FifteenMinutes extends TimeSeriesInterval("15min")
  case object ThirtyMinutes extends TimeSeriesInterval("30min")
  case object FortyFiveMinutes extends TimeSeriesInterval("45min")
  case object OneHour extends TimeSeriesInterval("1h")
  case object TwoHours extends TimeSeriesInterval("2h")
  case object FourHours extends TimeSeriesInterval("4h")
  case object OneDay extends TimeSeriesInterval("1day")
  case object OneWeek extends TimeSeriesInterval("1week")
  case object OneMonth extends TimeSeriesInterval("1month")
}

class ConcreteTimeframe(name: String, duration: Duration)

//
//  def ytd: Duration = {
//    import java.time.LocalDate
//
//
//    val startOfYear = Instant.now().atZone(ZoneOffset.UTC).toLocalDate
//      .withDayOfYear(1).atStartOfDay()
//      .atZone(ZoneOffset.UTC).toInstant
//
//    Duration.fromInterval(startOfYear, Instant.now)
//  }
//}
