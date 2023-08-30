package net.specula.twelvedata.client.model

final case class ApiEOD(
                         symbol: String,
                         exchange: String,
                         mic_code: String,
                         currency: String,
                         datetime: String,
                         close: String
                       )
