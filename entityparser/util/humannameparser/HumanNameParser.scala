//scalastyle:off
package com.zoho.crm.feature.entityparser.util.humannameparser

class HumanNameParser(actualName : String) extends Name(actualName) {
  private val name = actualName
  
  private val suffixes = Array[String] ( "esq", "esquire", "jr", // No I18N
            "sr", "2", "ii", "iii", "iv" )
  private val prefixes = Array[String] (  "bar", "ben", "bin", "da", "dal", // No I18N
                "de la", "de", "del", "der", "di", "ibn", "la", "le", // No I18N
                "san", "st", "ste", "van", "van der", "van den", "vel", // No I18N
                "von" ) // No I18N
  private val suffixesRegex : String = suffixes.mkString("\\.*|") + "\\.*"; //No I18N
  private val prefixesRegex : String = prefixes.mkString(" |") + " "; //No I18N
  
  // The regex use is a bit tricky.  *Everything* matched by the regex will be replaced,
  // but you can select a particular parenthesized submatch to be returned.
  // Also, note that each regex requres that the preceding ones have been run, and matches chopped out.
  private val nicknamesRegex = "(?i) ('|\\\"|\\(\\\"*'*)(.+?)('|\\\"|\\\"*'*\\)) "; //No I18N 
  private val suffixRegex = "(?i),* *(("+suffixesRegex+")$)"; //No I18N
  private val lastRegex = "(?i)(?!^)\\b([^ ]+ y |"+prefixesRegex+")*[^ ]+$"; //No I18N
  private val leadingInitRegex = "(?i)(^(.\\.*)(?= \\p{L}{2}))"; //No I18N 
  private val firstRegex = "(?i)^([^ ]+)";  //No I18N
  
  val nicknames = chopWithRegex(nicknamesRegex,2)
  val suffix = chopWithRegex(suffixRegex, 1)
  flip(",")
  val last = chopWithRegex(lastRegex, 0)
  val leadingInit = chopWithRegex(leadingInitRegex, 1)
  val first = chopWithRegex(firstRegex, 0)
  val middle = getStr()
  
  
}