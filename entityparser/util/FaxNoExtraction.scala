package com.zoho.crm.feature.entityparser.util

import com.zoho.crm.feature.entityparser.util.patterns.PhoneNoExtraction
import com.zoho.crm.feature.entityparser.constants.EntityParserConstants
import com.zoho.crm.ml.nlp.constants.SPECIALCHAR

class FaxNoExtraction {
  def extractFax(phoneData: String): Option[String] = {
    val patternToSearch = EntityParserConstants.FAX
    val textData = phoneData.toLowerCase().split("\n").reverse.find(_ contains patternToSearch).getOrElse(SPECIALCHAR.EMPTYSTRING)
    val phoneNos = PhoneNoExtraction.getPhoneNo(textData) //entityEssentials.getPhoneNo
    phoneNos.length match {
      case x if x.equals(1) => Some(phoneNos(0))
      case x if x.equals(2) =>
        val phoneAndIndex = EntityParserUtil.extractBestMatch(textData, phoneNos)
        textData.indexOf(patternToSearch) match {
          case x if x <= 4                 => Some(phoneAndIndex(1))
          case x if textData.length() - x <= 7 => Some(phoneAndIndex(1))
          case _ =>
            val splitByFax = textData.toLowerCase().split(patternToSearch)
            if (splitByFax.isEmpty) {
              None
            } else {
              if (splitByFax(0).length() < splitByFax(1).length()) {
                Some(splitByFax(0).trim())
              } else {
                Some(splitByFax(1).trim())
              }
            }
        }
      case _ => None
    }
  }
}