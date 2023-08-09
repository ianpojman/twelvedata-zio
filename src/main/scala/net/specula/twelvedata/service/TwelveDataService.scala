package net.specula.twelvedata.service

import net.specula.twelvedata.client.model.TwelveDataHistoricalDataRequest

import java.time.Instant

sealed trait Interval {
  def asString: String
}

abstract class AbstractInterval(val asString: String) extends Interval

case object OneMin extends AbstractInterval("1min")
case object FiveMin extends AbstractInterval("5min")
case object FifteenMin extends AbstractInterval("15min")
case object ThirtyMin extends AbstractInterval("30min")
case object FortyFiveMin extends AbstractInterval("45min")
case object OneHour extends AbstractInterval("1h")
case object TwoHours extends AbstractInterval("2h")
case object FourHours extends AbstractInterval("4h")
case object OneDay extends AbstractInterval("1day")
case object OneWeek extends AbstractInterval("1week")
case object OneMonth extends AbstractInterval("1month")

case class HistoricalDataQuery(symbols: List[Symbol],
                               interval: Interval,
                               outputSize: Int,
                               startTime: Option[Instant],
                               endTime: Option[Instant])

class TwelveDataService {
  def fetchHistoricalData(historicalDataQuery: HistoricalDataQuery) = {
    val historicalDataRequest: TwelveDataHistoricalDataRequest = fromQuery(historicalDataQuery)
    ???
  }

  def fromQuery(historicalDataQuery: HistoricalDataQuery): TwelveDataHistoricalDataRequest = ???
    //TwelveDataHistoricalDataRequest(historicalDataQuery.symbols.map(_.name),)
}
object TwelveDataService {
  //  lazy val live: ZLayer[CarRepository & DB, Nothing, TwelveDataService] =
  //    ZLayer.fromFunction(TwelveDataService(_, _))
}