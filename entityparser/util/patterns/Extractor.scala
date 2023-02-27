package com.zoho.crm.feature.entityparser.util.patterns

import scala.collection.mutable


object Extractor {
  /**
   * extract url from a string.
   *
   * @param s
   * @return
   */
  def extractUrl(s: String): List[String] = {
    val matcher = Patterns.WEB_URL.matcher(s) //extract URL
    val allMatches = new mutable.ArrayBuffer[String]
    while ( {
      matcher.find
    }) allMatches.append(matcher.group)
    allMatches.toList
  }

  def extractIp(s: String): List[String] = {

    val matcher = Patterns.IP_ADDRESS.matcher(s) //extract IP address
    val allMatches = new mutable.ArrayBuffer[String]
    while ( {
      matcher.find
    }) allMatches.append(matcher.group)
    allMatches.toList
  }

  def extractEmail(s: String): List[String] = {
    val matcher = Patterns.EMAIL_ADDRESS.matcher(s) //extract email address
    val allMatches = new mutable.ArrayBuffer[String]
    while ( {
      matcher.find
    }) allMatches.append(matcher.group)
    allMatches.toList
  }

  def extractVPA(s: String): List[String] = {
    val matcher = Patterns.VPA.matcher(s) //extract email address
    val allMatches = new mutable.ArrayBuffer[String]
    while ( {
      matcher.find
    }) allMatches.append(matcher.group)
    allMatches.toList
  }
}
