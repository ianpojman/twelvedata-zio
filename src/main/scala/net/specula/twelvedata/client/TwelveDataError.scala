package net.specula.twelvedata.client

import net.specula.twelvedata.client.model.TimeSeriesIntervalQuery

sealed trait TwelveDataError {
  def toThrowable: Throwable = this match {
    case TwelveDataError.RemoteException(t) => t
    case TwelveDataError.InvalidQuery(message) => new RuntimeException(message)
    case TwelveDataError.NoDataForQuery(query) => new RuntimeException(s"No data for query: $query")
  }
}

object TwelveDataError {
  case class NoDataForQuery(timeSeriesIntervalQuery: TimeSeriesIntervalQuery) extends TwelveDataError
  case class RemoteException(t: Throwable) extends TwelveDataError
  case class InvalidQuery(message: String) extends TwelveDataError

  object RemoteException:
    def ofMessage(s: String): RemoteException = RemoteException(new RuntimeException(s))
  end RemoteException
}

