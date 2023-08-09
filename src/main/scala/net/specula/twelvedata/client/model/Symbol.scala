package net.specula.twelvedata.client.model

import net.specula.twelvedata.client
import zio.ZLayer

case class Symbol private(name: String)

object Symbol:
  def fromString(s: String) =
    Symbol(
      s.ensuring(!_.contains(","), "Symbol cannot contain commas")
    )

  /** Convenience constructor to create ZLayer for ZIO dependency injection */
  def asLayer(tickers: List[Symbol]) = ZLayer.succeed(tickers)
end Symbol
