package net.specula.twelvedata.service

import net.specula.twelvedata.client.TimeSeriesInterval
import net.specula.twelvedata.client.model.TwelveDataHistoricalDataRequest

import java.time.Instant

case class HistoricalDataQuery(symbols: List[Symbol],
                               interval: TimeSeriesInterval,
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