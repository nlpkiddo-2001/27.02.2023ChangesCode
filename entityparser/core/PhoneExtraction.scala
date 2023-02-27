package com.zoho.crm.feature.entityparser.core

import com.zoho.crm.feature.entityparser.{EntityExtraction, ExtractedEntity, ParsedEntity}
import com.zoho.crm.feature.entityparser.util.patterns.PhoneNoExtraction
import com.zoho.crm.feature.entityparser.constants.{EntityParserConstants, TextBasedMatching}
import com.zoho.crm.feature.entityparser.outputs.Phone
import com.zoho.crm.feature.entityparser.util.{EntityParserUtil, FaxNoExtraction, TextBasedPatternMatching}
import com.zoho.crm.ml.nlp.constants.SPECIALCHAR


class PhoneExtraction(entityEssentials: ParsedEntity) extends EntityExtraction {

  def extract: ExtractedEntity = { //scalastyle:ignore
  
    try{
      val phoneData = preProcess
      val phoneNoExtraction = process(phoneData)
      val phoneAndFax = postProcess(phoneNoExtraction)
      phoneAndFax
    }catch{
      case e:Exception => 
        logger.info("Phone Extraction Failed "+e.getMessage)
        logger.info(e.getStackTrace.mkString("\n"))
        Phone(Array(), None)
    }
    
  }

  def preProcess: String = {
    val strToRemove = Array(entityEssentials.ipAddress)
    EntityParserUtil.stringRemoval(strToRemove, entityEssentials.textContainingEntities).replaceAll(SPECIALCHAR.DOUBLESPACE, SPECIALCHAR.EMPTYSTRING).toLowerCase
  }

  def process(phoneData: String): (Array[String], String) = { //scalastyle:ignore
    val phoneNumbers = PhoneNoExtraction.getPhoneNo(phoneData)
    val formatedPhoneNumbers: Map[String, String] = phoneNumbers.map(e => (e, e.replaceAll(SPECIALCHAR.SPACE, SPECIALCHAR.EMPTYSTRING))).toMap

    val formatedPhoneData = formatedPhoneNumbers.foldLeft(phoneData)((a, b) => a.replaceAllLiterally(b._1, b._2))

    val faxExtraction = new FaxNoExtraction
    val fax: String = //scalastyle:ignore
      faxExtraction.extractFax(formatedPhoneData) match {
        case Some(x) => x
        case None    => new TextBasedPatternMatching(formatedPhoneData, EntityParserConstants.FAX, TextBasedMatching.FAX_CHARCONSIDERED).extract
      }

    val phoneNumber = phoneNumbers.filterNot(phone => phone.replaceAll(SPECIALCHAR.SPACE, SPECIALCHAR.EMPTYSTRING).equals(fax))
    (phoneNumber, formatedPhoneNumbers.map(e => e._2 -> e._1).getOrElse(fax, fax))
  }

  def postProcess[R](phoneAndFax: (Array[String], String)): Phone = {
     phoneAndFax match {
       case x => Phone(
         if(x._1.length == 0) Array() else x._1,
         if(x._2.equals("")) None else Option(x._2)
       )
     }
  }
}