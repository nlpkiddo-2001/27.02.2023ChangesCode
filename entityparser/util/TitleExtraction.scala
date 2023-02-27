package com.zoho.crm.feature.entityparser.util

import com.zoho.crm.feature.entityparser.ParsedEntity
import com.zoho.crm.ml.nlp.constants.SPECIALCHAR


class TitleExtraction(entityEssentials: ParsedEntity, entityType: String) {

  def extract: Array[String] = { //scalastyle:ignore
    val signatureData = entityEssentials.textContainingEntities.split(SPECIALCHAR.NEWLINE).filter(_ != SPECIALCHAR.EMPTYSTRING)
    val dataToBeParsed = signatureData.map(_.split(" ").map(_.capitalize).mkString(" ")).map(e => {
      if (e.charAt(e.length() - 1).equals(SPECIALCHAR.COMMA)) e else e + SPECIALCHAR.COMMA
    }).mkString(SPECIALCHAR.NEWLINE)
    if (entityEssentials.annotatedMap.contains(entityType)) {
      val annotatedMap = entityEssentials.annotatedMap(entityType).toSet.toList.
        foldLeft(entityEssentials.annotatedMap(entityType).toSet.toList)(
          (entityList, entity) =>
            entityList.filterNot(x =>
              entity.length() > x.length() && !x.equals(entity) &&
                (if (x.length < entity.length) entity.contains(x) else x.contains(entity))))

      getBestEntityMatch(dataToBeParsed, annotatedMap.toArray)
    } else {
      Array[String]()
    }
    //    val person : Array[String] = {
    //      
    //      if(result.contains("PERSON")){
    //        val names = getBestEntityMatch(dataToBeParsed, result("PERSON"))
    //        if(names.length > 1) Array[String](names(0)) else names
    //       
    //      }else{
    //        Array[String](NameExtraction.extractName(dataToBeParsed, emailId, "name"))
    //      }
    //      
    //    }
    //    val company : String = {
    //      if(result.contains("ORGANIZATION")){ 
    //        getBestEntityMatch(dataToBeParsed, result("ORGANIZATION"))(0)
    //      }else{
    //         NameExtraction.extractName(dataToBeParsed, emailId, "company")
    //      }
    //    }
    //    (person.map(humanNameParser(_)),company,person)
    //    result.toMap
  }

  private def getBestEntityMatch(textData: String, entityWords: Array[String]): Array[String] = {

    val filteredEntity = entityWords.map(e => EntityParserUtil.longestCommonSubstring(e.toLowerCase, textData.toLowerCase()))
    EntityParserUtil.extractBestMatch(textData.toLowerCase(), filteredEntity)
  }

}
