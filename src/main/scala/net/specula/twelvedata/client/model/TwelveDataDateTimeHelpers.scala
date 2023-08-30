package net.specula.twelvedata.client.model

import java.time.format.DateTimeFormatter
import java.time.*

object TwelveDataDateTimeHelpers {
  // datetime format for datetime field of price bars 
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  def localDateToInstant(date: String, timeZone: String): Instant = {
    if (date.contains("T") || date.contains(" ")) {
      // Date + time format
      val parsedDateTime = LocalDateTime.parse(date.replace('T', ' '), formatter)
      parsedDateTime.atZone(ZoneId.of(timeZone)).toInstant
    } else {
      // Only date format
      val parsedLocalDate = LocalDate.parse(date)
      parsedLocalDate.atStartOfDay(ZoneId.of(timeZone)).toInstant
    }
  }

  def instantToLocaldateString(instant: Instant, timeZone: String): String = {
    val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of(timeZone))
    zonedDateTime.format(formatter)
  }
}
