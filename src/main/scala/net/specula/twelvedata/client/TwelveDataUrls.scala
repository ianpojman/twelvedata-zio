package net.specula.twelvedata.client

object TwelveDataUrls {
  val baseUrl = "https://api.twelvedata.com"

  def cleanUrl(url: String) = url.replaceAll("\\?apikey.*", "")

  /**
   * Quote endpoint is an efficient method to retrieve the latest quote of the selected instrument.
   * [[https://twelvedata.com/docs#quote]]
   */
  def quoteUrl(symbols: List[model.Symbol], config: TwelveDataConfig) =
    baseUrl + s"/quote?symbol=${symbols.map(_.name).mkString(",")}&apikey=${config.apiKey}"

  def findPriceUrl(symbols: Seq[model.Symbol], config: TwelveDataConfig) =
    baseUrl + s"/price?symbol=${symbols.map(_.name).mkString(",")}&apikey=${config.apiKey}"
}
