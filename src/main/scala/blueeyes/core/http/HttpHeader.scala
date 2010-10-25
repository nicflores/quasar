package blueeyes.core.http

import blueeyes.util.ProductPrefixUnmangler


sealed trait HttpHeader extends Product2[String, String] with ProductPrefixUnmangler { self =>
  def _1 = productPrefix
  def _2 = value
  
  def name: String = _1
  
  def value: String
  
  def header = _1 + ": " + _2
  
  def canEqual(any: Any) = any match {
    case header: HttpHeader => true
    case _ => false
  }
  
  override def productPrefix = self.getClass.getSimpleName
  
  override def toString = header
  
  override def equals(any: Any) = any match {
    case that: HttpHeader => (self._1.toLowerCase == that._1.toLowerCase) && 
                             (self._2.toLowerCase == that._2.toLowerCase)
    case _ => false
  }
}

object HttpHeaders {

  /************ Requests ************/

  class Accept(val mimeTypes: MimeType*) extends HttpHeader {
    def value = mimeTypes.map(_.value).mkString(", ")
  }
  object Accept {
    def apply(mimeTypes: MimeType*): Accept = new Accept(mimeTypes: _*)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "accept") Some(MimeTypes.parseMimeTypes(keyValue._2)) else None
  }
  
  class `Accept-Charset`(val charSets: CharSet*) extends HttpHeader {
    def value = charSets.map(_.value).mkString(";")
  }
  object `Accept-Charset` {
    def apply(charSets: CharSet*): `Accept-Charset` = new `Accept-Charset`(charSets: _*)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "accept-charset") Some(CharSets.parseCharSets(keyValue._2)) else None
  }

  class `Accept-Encoding`(val encodings: Encoding*) extends HttpHeader  {
    def value = encodings.map(_.value).mkString(", ")
  }
  object `Accept-Encoding` {
    def apply(encodings: Encoding*) = new `Accept-Encoding`(encodings: _*)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "accept-encoding") Some(Encodings.parseEncodings(keyValue._2)) else None
  }

  class `Accept-Language`(val languageRanges: LanguageRange*) extends HttpHeader {
    def value = languageRanges.map(_.value).mkString(", ");
  }
  object `Accept-Language` {
    def apply(languageRanges: LanguageRange*) = new `Accept-Language`(languageRanges: _*)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "accept-language") Some(LanguageRanges.parseLanguageRanges(keyValue._2)) else None
  }

  class `Accept-Ranges`(val rangeUnit: RangeUnit) extends HttpHeader {
    def value = rangeUnit.toString
  }
  object `Accept-Ranges` {
    def apply(rangeUnit: RangeUnit) = new `Accept-Ranges`(rangeUnit);
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "accept-ranges") 
      Some(RangeUnits.parseRangeUnits(keyValue._2)) else None
  }

  class Authorization(val value: String) extends HttpHeader 
  object Authorization {
    def apply(credentials: String) = new Authorization(credentials)  // can we do better here?
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "authorization") Some(keyValue._2) else None
  }

  class Connection(val connectionToken: ConnectionToken) extends HttpHeader {
    def value = connectionToken.toString 
  }
  object Connection {
    def apply(connectionToken: ConnectionToken) = new Connection(connectionToken)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "connection")
      Some(ConnectionTokens.parseConnectionTokens(keyValue._2)) else None
  }

  class Cookie(val cookie: HttpCookie) extends HttpHeader {
    val value = cookie.toString
  }
  object Cookie {
    def apply(cookie: HttpCookie) = new Cookie(cookie)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "cookie")
      Some(HttpCookies.parseHttpCookies(keyValue._2)) else None
  }

  class `Content-Length`(val length: Long) extends HttpHeader {
    def value = length.toString
  }
  object `Content-Length` {
    def apply(length: Long): `Content-Length` = new `Content-Length`(length)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "content-length") Some(keyValue._2.toLong) else None
  }

  class `Content-Type`(val mimeTypes: MimeType*) extends HttpHeader {
    def value = mimeTypes.map(_.value).mkString(";")
  }
  object `Content-Type` {
    def apply(mimeTypes: MimeType*) = new `Content-Type`(mimeTypes :_*)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "content-type") Some(MimeTypes.parseMimeTypes(keyValue._2)) else None
  }

  class Date(val httpDate: HttpDateTime) extends HttpHeader {
    def value = httpDate.toString
  }
  object Date {
    def apply(httpDate: HttpDateTime) = new Date(httpDate)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "date") Some(HttpDateTimes.parseHttpDateTimes(keyValue._2)) else None
  }

  class Expect(val expectation: Expectation) extends HttpHeader {
    def value = expectation.toString
  }
  object Expect {
    def apply(expectation: Expectation) = new Expect(expectation)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "expect") Some(Expectations.parseExpectations(keyValue._2)) else None
  }

  class From(val email: Email) extends HttpHeader {
    def value = email.toString
  }
  object From {
    def apply(email: Email) = new From(email)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "from") Some(Emails.parseEmails(keyValue._2)) else None
  }

  class Host(val domain: HttpDomain) extends HttpHeader {
    def value = domain.toString
  }
  object Host {
    def apply(domain: HttpDomain) = new Host(domain)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "host") Some(HttpDomains.parseHttpDomains(keyValue._2)) else None
  }

  class `If-Match`(val tags: EntityTag) extends HttpHeader {
    def value = tags.toString
  }
  object `If-Match` { // going to need a new typ here
    def apply(tags: EntityTag) = new `If-Match`(tags)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "if-match") Some(EntityTags.parseEntityTags(keyValue._2)) else None
  }

  class `If-Modified-Since`(val httpDate: HttpDateTime) extends HttpHeader {
    def value = httpDate.toString
  }
  object `If-Modified-Since` {
    def apply(httpDate: HttpDateTime) = new `If-Modified-Since`(httpDate)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "if-modified-since") Some(HttpDateTimes.parseHttpDateTimes(keyValue._2)) else None
  }

  class `If-None-Match`(val tags: EntityTag) extends HttpHeader {
    def value = tags.toString
  }
  object `If-None-Match` {
    def apply(tags: EntityTag) = new `If-None-Match`(tags)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "if-none-match")
      Some(EntityTags.parseEntityTags(keyValue._2)) else None
  }

  /* If-Range needs to add a way to include the date */
  class `If-Range`(val tags: EntityTag) extends HttpHeader {
    def value = tags.toString
  }
  object `If-Range` {
    def apply(tags: EntityTag) = new `If-Range`(tags)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "if-range")
      Some(EntityTags.parseEntityTags(keyValue._2)) else None
  }

  class `If-Unmodified-Since`(val httpDate: HttpDateTime) extends HttpHeader {
    def value = httpDate.toString
  }
  object `If-Unmodified-Since` {
    def apply(httpDate: HttpDateTime) = new `If-Unmodified-Since`(httpDate)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "if-unmodified-since") 
      Some(HttpDateTimes.parseHttpDateTimes(keyValue._2)) else None
  }

  class `Max-Forwards`(val maxf: Long) extends HttpHeader {
    def value = maxf.toString 
  }
  object `Max-Forwards` {
    def apply(maxf: Long) = new `Max-Forwards`(maxf)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "max-forwards")
      Some(keyValue._2.toLong) else None
  }

  class Pragma(val primeDirective: PragmaDirective) extends HttpHeader {
    def value = primeDirective.toString
  }
  object Pragma {
    def apply(primeDirective: PragmaDirective) = new Pragma(primeDirective)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "pragma")
      Some(PragmaDirectives.parsePragmaDirectives(keyValue._2)) else None
  }

  class `Proxy-Authorization`(val value: String) extends HttpHeader {
  }
  object `Proxy-Authorization` {
    def apply(auth: String) = new `Proxy-Authorization`(auth)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "proxy-authorization")
      Some(keyValue._2) else None
  }

  class Range(val byteRange: ByteRange) extends HttpHeader {
    def value = byteRange.toString
  }
  object Range {
    def apply(byteRange: ByteRange) = new Range(byteRange)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "range")
      Some(ByteRanges.parseByteRange(keyValue._2)) else None
  }

  class Referer(val domain: HttpDomain) extends HttpHeader {
    def value = domain.toString
  }
  object Referer {
    def apply(domain: HttpDomain) = new Referer(domain)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "referer")
      Some(HttpDomains.parseHttpDomains(keyValue._2)) else None
  }

  class TE(val tcodings: TCoding*) extends HttpHeader {
    def value = tcodings.map(_.toString).mkString(", ")
  }
  object TE {
    def apply(tcodings: TCoding*) = new TE(tcodings: _*)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "te")
      Some(TCodings.parseTCodings(keyValue._2)) else None
  }

  class Upgrade(val products: UpgradeProduct*) extends HttpHeader {
    def value = products.map(_.toString).mkString(", ")
  }
  object Upgrade {
    def apply(products: UpgradeProduct*) = new Upgrade(products: _*)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "upgrade")
      Some(UpgradeProducts.parseUpgradeProducts(keyValue._2)) else None
  }

  class `User-Agent`(val product: String) extends HttpHeader {
    def value = product
  }
  object `User-Agent` {
    def apply(product: String) = new `User-Agent`(product)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "user-agent") Some(keyValue._2) else None
  }

  class Via(val value: String) extends HttpHeader {
  }
  object Via {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "via") Some(keyValue._2) else None
  }

  class Warning(val value: String) extends HttpHeader {
  }
  object Warning {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "warning") Some(keyValue._2) else None
  }

  /*********** Responses ************/

  class Age(val value: String) extends HttpHeader 
  object Age {
    def apply(age: Int) = new Age(age.toString)
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "age") Some(keyValue._2) else None
  }

  class Allow(val value: String) extends HttpHeader 
  object Allow {
    def apply(methods: HttpMethod*) = new Allow(methods.map(_.value).mkString(","))
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "allow") Some(keyValue._2) else None
  }

  class `Cache-Control`(val value: String) extends HttpHeader {
  }
  object `Cache-Control` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "cache-control") Some(keyValue._2) else None
  }

  class `Content-Encoding`(val value: String) extends HttpHeader {
  }
  object `Content-Encoding` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "content-encoding") Some(keyValue._2) else None
  }

  class `Content-Language`(val value: String) extends HttpHeader {
  }
  object `Content-Language` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "content-language") Some(keyValue._2) else None
  }

  class `Content-Location`(val value: String) extends HttpHeader {
  }
  object `Content-Location` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "content-location") Some(keyValue._2) else None
  }

  class `Content-Disposition`(val value: String) extends HttpHeader {
  }
  object `Content-Disposition` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "content-disposition") Some(keyValue._2) else None
  }

  class `Content-MD5`(val value: String) extends HttpHeader {
  }
  object `Content-MD5` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "content-md5") Some(keyValue._2) else None
  }

  class `Content-Range`(val value: String) extends HttpHeader {
  }
  object `Content-Range` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "content-range") Some(keyValue._2) else None
  }

  class ETag(val value: String) extends HttpHeader {
  }
  object ETag {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "etag") Some(keyValue._2) else None
  }

  class Expires(val value: String) extends HttpHeader {
  }
  object Expires {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "expires") Some(keyValue._2) else None
  }

  class `Last-Modified`(val value: String) extends HttpHeader {
  }
  object `Last-Modified` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "last-modified") Some(keyValue._2) else None
  }

  class Location(val value: String) extends HttpHeader {
  }
  object Location {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "location") Some(keyValue._2) else None
  }

  class `Proxy-Authenticate`(val value: String) extends HttpHeader {
  }
  object `Proxy-Authenticate` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "proxy-authenticate") Some(keyValue._2) else None
  }

  class Refresh(val value: String) extends HttpHeader {
  }
  object Refresh {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "refresh") Some(keyValue._2) else None
  }

  class `Retry-After`(val value: String) extends HttpHeader {
  }
  object `Retry-After` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "retry-after") Some(keyValue._2) else None
  }

  class Server(val value: String) extends HttpHeader {
  }
  object Server {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "server") Some(keyValue._2) else None
  }

  class `Set-Cookie`(val value: String) extends HttpHeader {
  }
  object `Set-Cookie` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "set-cookie") Some(keyValue._2) else None
  }

  class Trailer(val value: String) extends HttpHeader {
  }
  object Trailer {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "trailer") Some(keyValue._2) else None
  }

  class `Transfer-Encoding`(val value: String) extends HttpHeader {
  }
  object `Transfer-Encoding` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "transfer-encoding") Some(keyValue._2) else None
  }

  class Vary(val value: String) extends HttpHeader {
  }
  object Vary {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "vary") Some(keyValue._2) else None
  }

  class `WWW-Authenticate`(val value: String) extends HttpHeader {
  }
  object `WWW-Authenticate` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "www-authenticate") Some(keyValue._2) else None
  }

  class `X-Frame-Options`(val value: String) extends HttpHeader {
  }
  object `X-Frame-Options` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "x-frame-options") Some(keyValue._2) else None
  }

  class `X-XSS-Protection`(val value: String) extends HttpHeader {
  }
  object `X-XSS-Protection` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "x-xss-protection") Some(keyValue._2) else None
  }

  class `X-Content-Type-Options`(val value: String) extends HttpHeader {
  }
  object `X-Content-Type-Options` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "x-content-type-options") Some(keyValue._2) else None
  }

  class `X-Requested-With`(val value: String) extends HttpHeader {
  }
  object `X-Requested-With` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "x-requested-with") Some(keyValue._2) else None
  }

  class `X-Forwarded-For`(val value: String) extends HttpHeader {
  }
  object `X-Forwarded-For` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "x-forwarded-for") Some(keyValue._2) else None
  }

  class `X-Forwarded-Proto`(val value: String) extends HttpHeader {
  }
  object `X-Forwarded-Proto` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "x-forwarded-proto") Some(keyValue._2) else None
  }

  class `X-Powered-By`(val value: String) extends HttpHeader {
  }
  object `X-Powered-By` {
    def unapply(keyValue: (String, String)) = if (keyValue._1.toLowerCase == "x-powered-by") Some(keyValue._2) else None
  }  
  
  class CustomHeader(override val name: String, val value: String) extends HttpHeader {
  }
}

trait HttpHeaderImplicits {
  import HttpHeaders._
  
  implicit def httpHeader2Tuple(httpHeader: HttpHeader) = (httpHeader._1, httpHeader._2)
  
  implicit def tuple2HttpHeader(keyValue: (String, String)): HttpHeader = keyValue match {
    case Accept(value) => new Accept(value: _*)
    case `Accept-Charset`(value) => new `Accept-Charset`(value: _*)
    case `Accept-Encoding`(value) => new `Accept-Encoding`(value: _*)
    case `Accept-Language`(value) => new `Accept-Language`(value: _*)
    case `Accept-Ranges`(value) => new `Accept-Ranges`(value)
    case Authorization(value) => new Authorization(value)
    case Connection(value) => new Connection(value)
    case Cookie(value) => new Cookie(value)
    case `Content-Length`(value) => new `Content-Length`(value)
    case `Content-Type`(value) => new `Content-Type`(value: _*)
    case Date(value) => new Date(value)
    case Expect(value) => new Expect(value)
    case From(value) => new From(value)
    case Host(value) => new Host(value)
    case `If-Match`(value) => new `If-Match`(value)
    case `If-Modified-Since`(value) => new `If-Modified-Since`(value)
    case `If-None-Match`(value) => new `If-None-Match`(value)
    case `If-Range`(value) => new `If-Range`(value)
    case `If-Unmodified-Since`(value) => new `If-Unmodified-Since`(value)
    case `Max-Forwards`(value) => new `Max-Forwards`(value)
    case Pragma(value) => new Pragma(value)
    case `Proxy-Authorization`(value) => new `Proxy-Authorization`(value)
    case Range(value) => new Range(value)
    case Referer(value) => new Referer(value)
    case TE(value) => new TE(value: _*)
    case Upgrade(value) => new Upgrade(value: _*)
    case `User-Agent`(value) => new `User-Agent`(value)
    case Via(value) => new Via(value)
    case Warning(value) => new Warning(value)

    /** Responses **/
    case Age(value) => new Age(value)
    case Allow(value) => new Allow(value)
    case `Cache-Control`(value) => new `Cache-Control`(value)
    case `Content-Encoding`(value) => new `Content-Encoding`(value)
    case `Content-Language`(value) => new `Content-Language`(value)
    case `Content-Location`(value) => new `Content-Location`(value)
    case `Content-Disposition`(value) => new `Content-Disposition`(value)
    case `Content-MD5`(value) => new `Content-MD5`(value)
    case `Content-Range`(value) => new `Content-Range`(value)
    case ETag(value) => new ETag(value)
    case Expires(value) => new Expires(value)
    case `Last-Modified`(value) => new `Last-Modified`(value)
    case Location(value) => new Location(value)
    case `Proxy-Authenticate`(value) => new `Proxy-Authenticate`(value)
    case Refresh(value) => new Refresh(value)
    case `Retry-After`(value) => new `Retry-After`(value)
    case Server(value) => new Server(value)
    case `Set-Cookie`(value) => new `Set-Cookie`(value)
    case Trailer(value) => new Trailer(value)
    case `Transfer-Encoding`(value) => new `Transfer-Encoding`(value)
    case Vary(value) => new Vary(value)
    case `WWW-Authenticate`(value) => new `WWW-Authenticate`(value)
    case `X-Frame-Options`(value) => new `X-Frame-Options`(value)
    case `X-XSS-Protection`(value) => new `X-XSS-Protection`(value)
    case `X-Content-Type-Options`(value) => new `X-Content-Type-Options`(value)
    case `X-Requested-With`(value) => new `X-Requested-With`(value)
    case `X-Forwarded-For`(value) => new `X-Forwarded-For`(value)
    case `X-Forwarded-Proto`(value) => new `X-Forwarded-Proto`(value)
    case `X-Powered-By`(value) => new `X-Powered-By`(value)
    case (name, value) => new CustomHeader(name, value)
  }
}
object HttpHeaderImplicits extends HttpHeaderImplicits
