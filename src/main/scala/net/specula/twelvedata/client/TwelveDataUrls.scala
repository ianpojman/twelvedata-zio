package net.specula.twelvedata.client

object TwelveDataUrls {
  val baseUrl = "https://api.twelvedata.com"

  def cleanUrl(url: String): String =
    url.replaceAll("&apikey.*", "") // assumes we always use it as the last param

  /**
   * Quote endpoint is an efficient method to retrieve the latest quote of the selected instrument.
   * [[https://twelvedata.com/docs#quote]]
   */
  def quoteUrl(symbols: List[String], config: TwelveDataConfig): String =
    baseUrl + s"/quote?symbol=${symbols.mkString(",")}&prepost=true&apikey=${config.apiKey}"

  def findPriceUrl(symbols: Seq[String], config: TwelveDataConfig): String =
    baseUrl + s"/price?symbol=${symbols.mkString(",")}&apikey=${config.apiKey}"

  def eodUrl(symbol: String, config: TwelveDataConfig): String =
    s"${baseUrl}/eod?symbol=${symbol}&apikey=${config.apiKey}&start_date"

}
