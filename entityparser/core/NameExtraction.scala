package com.zoho.crm.feature.entityparser.core

import com.zoho.crm.feature.entityparser.constants.UnWantedElements
import com.zoho.crm.feature.entityparser.outputs.{Name, NameAttributes}
import com.zoho.crm.feature.entityparser.util.fuzzywuzzy.FuzzySearch
import com.zoho.crm.feature.entityparser.util.humannameparser.HumanNameParser
import com.zoho.crm.feature.entityparser.util.{ApproximateStringMatching, EntityParserUtil, FuzzyBasedStringMatching, TitleExtraction}
import com.zoho.crm.feature.entityparser.{EntityExtraction, ExtractedEntity, ParsedEntity}
import com.zoho.crm.ml.nlp.constants.{SPECIALCHAR, TITLES}


class NameExtraction(entityEssentials: ParsedEntity) extends EntityExtraction {

  def extract: ExtractedEntity = { //scalastyle:ignore
  try{
    val titleMatch =
    val nameExtraction = process(titleMatch)
    postProcess(nameExtraction)
  }catch{
    
    case e: Throwable =>
      logger.info("Name Extraction Failed "+e.toString)
      Name(None)
    
    case e: Exception => 
      logger.info("Name Extraction Failed "+e.toString)
      Name(None)
     
  }
    
  }

  def preProcess: Array[String] = {
    val titleExtraction = new TitleExtraction(entityEssentials, TITLES.PERSON)
    val possibleNameMatch = titleExtraction.extract
    possibleNameMatch
  }

  def process(possibleNameMatch: Array[String]): String = { //scalastyle:ignore
    val nameFromEmail = EntityParserUtil.getPatternFromEmail(entityEssentials.emailId, "name")
    val companyFromEmail = EntityParserUtil.getPatternFromEmail(entityEssentials.emailId, "company")
    if (EntityParserUtil.longestCommonSubstring(nameFromEmail, companyFromEmail).length() >= 4 || FuzzySearch.ratio(nameFromEmail, companyFromEmail) >= 70) {
      if (possibleNameMatch.length != 0) {
        possibleNameMatch.length match {
          case 1 => possibleNameMatch(0)
          case x if x > 1 => possibleNameMatch.length match {
            case 1 => possibleNameMatch(0)
            case x if x > 1 => possibleNameMatch.filter(x => {
              val longSeq = EntityParserUtil.longestCommonSubstring(nameFromEmail, x)
              longSeq.equals(nameFromEmail) || longSeq.contains(nameFromEmail) || nameFromEmail.contains(longSeq)
            })(0)
          }
        }
      } else {
        SPECIALCHAR.EMPTYSTRING
      }
    } else {
      if (UnWantedElements.ROLEBASED_EMAIL contains nameFromEmail) {
        if (possibleNameMatch.length != 0) {
          possibleNameMatch.length match {
            case 1 => possibleNameMatch(0)
            case x if x > 1 =>
              possibleNameMatch.filter(x => {
                val longSeq = EntityParserUtil.longestCommonSubstring(nameFromEmail, x)
                longSeq.equals(nameFromEmail) || longSeq.contains(nameFromEmail) || nameFromEmail.contains(longSeq)
              })(0)
          }
        } else {
          SPECIALCHAR.EMPTYSTRING
        }
      } else {
        if(!entityEssentials.personName.equals("")){
          val fuzzyBasedStringExtraction = new FuzzyBasedStringMatching(entityEssentials.personName, nameFromEmail).extract()
          if (fuzzyBasedStringExtraction._2 >= 0.30) {
             fuzzyBasedStringExtraction._1
           }else{
             val fuzzyBasedStringExtraction = new FuzzyBasedStringMatching(entityEssentials.textContainingEntities, nameFromEmail).extract()
              if (fuzzyBasedStringExtraction._2 >= 0.30) {
                val fuzzyMatch = possibleNameMatch.filter(_.contains(fuzzyBasedStringExtraction._1))
                if (fuzzyMatch.length.equals(0)) {
                  fuzzyBasedStringExtraction._1
                } else {
                  possibleNameMatch.length match {
                    case x if x == 1 => possibleNameMatch(0)
                    case x if x > 1 => possibleNameMatch.filter(x => {
                      val longSeq = EntityParserUtil.longestCommonSubstring(nameFromEmail, x)
                      longSeq.equals(nameFromEmail) || longSeq.contains(nameFromEmail) || nameFromEmail.contains(longSeq)
                    })(0)
                  }
                }
              } else {
                if (possibleNameMatch.length != 0) {
                  possibleNameMatch.length match {
                    case 1 => possibleNameMatch(0)
                    case x if x > 1 => possibleNameMatch.filter(x => {
                      val longSeq = EntityParserUtil.longestCommonSubstring(nameFromEmail, x)
                      longSeq.equals(nameFromEmail) || longSeq.contains(nameFromEmail) || nameFromEmail.contains(longSeq)
                    })(0)
                  }
                } else {
                  SPECIALCHAR.EMPTYSTRING
                }
              }
           }
        }else{
          val fuzzyBasedStringExtraction = new FuzzyBasedStringMatching(entityEssentials.textContainingEntities, nameFromEmail).extract()
          if (fuzzyBasedStringExtraction._2 >= 0.30 && fuzzyBasedStringExtraction._1.length() >= 4) {
            val fuzzyMatch = possibleNameMatch.filter(_.contains(fuzzyBasedStringExtraction._1))
            if (fuzzyMatch.length.equals(0)) {
              fuzzyBasedStringExtraction._1
            } else {
              possibleNameMatch.length match {
                case 1 => possibleNameMatch(0)
                case x if x > 1 => possibleNameMatch.filter(x => {
                  val longSeq = EntityParserUtil.longestCommonSubstring(nameFromEmail, x)
                  longSeq.equals(nameFromEmail) || longSeq.contains(nameFromEmail) || nameFromEmail.contains(longSeq)
                })(0)
              }
            }
          } else {
            if (possibleNameMatch.length != 0) {
              possibleNameMatch.length match {
                case 1 => possibleNameMatch(0)
                case x if x > 1 => possibleNameMatch.filter(x => {
                  val longSeq = EntityParserUtil.longestCommonSubstring(nameFromEmail, x)
                  longSeq.equals(nameFromEmail) || longSeq.contains(nameFromEmail) || nameFromEmail.contains(longSeq)
                })(0)
              }
            } else {
              SPECIALCHAR.EMPTYSTRING
            }
          }
        }
      }
    }

  }

  def postProcess(extractedName: String): Name = {
    val cleanedName = extractedName.replace(SPECIALCHAR.NONAPLHANUMERIC, SPECIALCHAR.EMPTYSTRING)
    val originalName = ApproximateStringMatching.firstMatch(entityEssentials.textContainingEntities, cleanedName)
    val normalizedName = EntityParserUtil.stringNormalization(originalName)
    val nameParse = new HumanNameParser(normalizedName)
    cleanedName match {
      case x if x.isEmpty || x.equals("") => Name(None)
      case _ => Name(Option(
                      NameAttributes(
                         Option(normalizedName),
                         isEmpty(nameParse.first), 
                         isEmpty(nameParse.last), 
                         isEmpty(nameParse.middle)
                     )))
    }
    
  }
  
  def isEmpty(str: String) : Option[String] = str match {
    case str if str.equals("") => None
    case str => Option(str)
  }
}