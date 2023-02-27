package com.zoho.crm.feature.entityparser.util.patterns

import java.util.regex.Pattern

object EmailCleaning {
  val ptr = """([a-zA-z .@<>"']{2,100} )?(([^<>()\[\]|_\-\.,;:\s@\"]+(\.[^<>()\[\]\.,;:\s@\"]+)*)|(\".+\"))+@+(([^<>\(\)\[\]\.,;:\s@\"])+(\.+[^<>\(\)\[\]\.,;:\s@\"][a-zA-Z.]{1,20})?)""".r
  val emailRegexPtr: Pattern = ptr.pattern
  //"""([a-zA-z .@<>"']{2,100} )?<?(([^<>()\[\]\.,;:\s@\"]+(\.[^<>()\[\]\.,;:\s@\"]+)*)|(\".+\"))+@+(([^<>\(\)\[\]\.,;:\s@\"]+\.)+[^<>\(\)\[\]\.,;:\s@\"]{2,})>?""".r

  /*
   * Pattern matches
   * 	Zoho Corp <xxxxx.yyyyy@zohocorp.com>
   *
   * 	xxxxx.yyyyy@zohocorp.com
   *
   *
   * 	"Zoho Corp" <xxxxx.yyyyy@zohocorp.com>*/

  def extractEmailAndName(emailWithName : String) : (Option[String], Option[String]) = {
    val values = ptr.findAllIn(emailWithName).matchData.toList map{
      m => (Option(m.group(1)), Option(m.group(2)+ "@" + m.group(6)))
    }
    values.head
  }

}
