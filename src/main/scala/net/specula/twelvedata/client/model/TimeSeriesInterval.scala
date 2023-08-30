package net.specula.twelvedata.client.model

import net.specula.twelvedata.client.model.{ TimeSeriesInterval}
import zio.*

import java.time.{Instant, LocalDate, ZoneOffset}

/** Query for time series data, from the current time backwards, at the interval specified */
case class TimeSeriesIntervalQuery(symbols: List[String],
                                   timeSeriesInterval: TimeSeriesInterval,
                                   timezone: String,
                                   startDate: Option[LocalDate] = None,
                                   endDate:  Option[LocalDate] = None,
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

  def duration: Duration =
    this match
      case OneMinute => Duration.fromMillis(60 * 1000)
      case FiveMinutes => Duration.fromMillis(5 * 60 * 1000)
      case FifteenMinutes => Duration.fromMillis(15 * 60 * 1000)
      case ThirtyMinutes => Duration.fromMillis(30 * 60 * 1000)
      case FortyFiveMinutes => Duration.fromMillis(45 * 60 * 1000)
      case OneHour => Duration.fromMillis(60 * 60 * 1000)
      case TwoHours => Duration.fromMillis(2 * 60 * 60 * 1000)
      case FourHours => Duration.fromMillis(4 * 60 * 60 * 1000)
      case OneDay => Duration.fromMillis(24 * 60 * 60 * 1000)
      case OneWeek => Duration.fromMillis(7 * 24 * 60 * 60 * 1000)
      case OneMonth => Duration.fromMillis(30 * 24 * 60 * 60 * 1000)

object TimeSeriesInterval:
  def fromString(str: String): Option[TimeSeriesInterval] =
    TimeSeriesInterval.values.find(_.apiName == str)