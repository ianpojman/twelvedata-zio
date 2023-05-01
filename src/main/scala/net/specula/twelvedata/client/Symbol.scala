package net.specula.twelvedata.client

import net.specula.twelvedata.client
import zio.ZLayer

case class Symbol(name: String)

object Symbol:
  /** Convenience constructor to create ZLayer for ZIO dependency injection */
  def asLayer(tickers: List[client.Symbol]) = ZLayer.succeed(tickers)
end Symbol
