package net.specula.twelvedata.client

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object TwelveDataConfigDemo extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    for {
      c <- TwelveDataConfig.loadConfig
      _ <- zio.Console.printLine(c)
    } yield ()
}
