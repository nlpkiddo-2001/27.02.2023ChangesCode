//scalastyle:off
package com.zoho.crm.feature.entityparser.constants

object EntityParserConstants extends Enumeration {
  val SIGNATURE = "signature"

  val ENQUIRY = "enquiry"

  val NAME = "name"
  val COMPANY = "company"

  val EMAIL = "email"
  val WEBSITE = "website"

  val PHONE = "phone"
  val FAX = "fax"
  val PRIMARYPHONENO = "primaryphoneno"
  val SECONDARYPHONENO = "secondaryphoneno"

  //Social Links
  val SOCIAL = "sociallink"
  val TWITTER = "twitter"
  val FACEBOOK = "facebook"
  val LINKEDIN = "linkedin"

  val YOUTUBE = "youtube"
  val GITHUB = "github"
  val SKYPE = "skype"
  val GPLUS = "gplus"

  val IP_ADDRESS = "ipaddress"
  val VPA = "vpa"

  val STATE_CODE_DATA_PATH = "/data/autoenrichment/statecode.csv"
  val COUNTRY_CODE_DATA_PATH = "/data/autoenrichment/countrycode.csv"
  val NORMALIZED_ABBREVATIONS_DATA_PATH = "/data/autoenrichment/normalizedabbrevations.csv"
}

object SocialLinkRegex extends Enumeration {
  final val INSTAGRAM_REGEX = "((http|https)://)?(\\w+\\.)?(instagram)(\\.com)(/([A-Za-z0-9_](?:(?:[A-Za-z0-9_]|(?:\\.(?!\\.))){0,28}(?:[A-Za-z0-9_]))?))"
  final val FACEBOOK_REGEX = "((http(s)?://)?)?(\\w+\\.)?(facebook|fb)(\\.com/)([A-z 0-9 _ - a-z\\.]{1,})"
  final val LINKEDIN_PROFILE_REGEX = "((?:https?:)?\\/\\/(?:[\\w]+\\.)?linkedin\\.com\\/in\\/([\\w\\-\\_À-ÿ%]+)\\/?|(?:https?:)?\\/\\/(?:[\\w]+\\.)?linkedin\\.com\\/pub\\/([A-z0-9_-]+)(?:\\/[A-z0-9]+){3}\\/?)"
  final val LINKEDIN_COMPANY_REGEX = "((?:https?:)?\\/\\/(?:[\\w]+\\.)?linkedin\\.com\\/company\\/([A-z0-9-\\.]+)\\/?)"
  final val LINKEDIN = s"$LINKEDIN_PROFILE_REGEX|$LINKEDIN_COMPANY_REGEX"
  final val GITHUB_REGEX = "((http|https)://)?(\\w+\\.)?(github)(\\.com)/([A-z 0-9 _ -]+)"
  final val TWITTER_REGEX = "((http(s)?://)?)?(\\w+\\.)?(twitter.com/)([A-z 0-9 _a-z\\.]{1,15})"
  final val GPLUS_REGEX = "((http(s)?://)?)?(\\w+\\.)?(plus.google)(\\.com/)(\\+[^/]+|\\d{21})"
  final val SKYPE_REGEX: String = "(?:(?:callto|skype|live):)([a-z][a-z0-9\\.,\\-_]{5,31})(?:\\?(?:add|call|chat|sendfile|userinfo))?"
}

object TextBasedMatching extends Enumeration {
  final val SOCIALLINK_CHARCONSIDERED = "[^0-9a-zA-z@\\n/].*"
  final val FAX_CHARCONSIDERED = "[^0-9a-zA-z@\\n\\-\\(\\)/\\.\\+].*"
}






