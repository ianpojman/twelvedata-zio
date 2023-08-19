package net.specula.twelvedata.client.model

import net.specula.twelvedata.client.model.{Symbol, TimeSeriesInterval}
import zio.*

import java.time.{Instant, ZoneOffset}

/** Query for time series data, from the current time backwards, at the interval specified */
case class TimeSeriesIntervalQuery(symbols: List[String],
                                   timeSeriesInterval: TimeSeriesInterval,
                                   startDate: Instant,
                                   endDate: Instant,
                                   outputCount: Int=30)


enum TimeSeriesInterval(val apiName: String):
  case OneMinute extends TimeSeriesInterval("1min")
  case FiveMinutes extends TimeSeriesInterval("5min")
  case FifteenMinutes extends TimeSeriesInterval("15min")
  case ThirtyMinutes extends TimeSeriesInterval("30min")
  case FortyFiveMinutes extends TimeSeriesInterval("45min")
  case OneHour extends TimeSeriesInterval("1h")
  case TwoHours extends TimeSeriesInterval("2h")
  case FourHours extends TimeSeriesInterval("4h")
  case OneDay extends TimeSeriesInterval("1day")
  case OneWeek extends TimeSeriesInterval("1week")
  case OneMonth extends TimeSeriesInterval("1month")

object TimeSeriesInterval:
  def fromStrings(s: String*): List[Symbol] = s.map(Symbol.fromString).toList

  def fromString(str: String): Option[TimeSeriesInterval] =
    TimeSeriesInterval.values.find(_.apiName == str)