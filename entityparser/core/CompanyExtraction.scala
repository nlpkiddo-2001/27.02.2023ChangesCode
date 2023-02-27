package com.zoho.crm.feature.entityparser.core

import com.zoho.crm.feature.entityparser.{EntityExtraction, ExtractedEntity, ParsedEntity}
import com.zoho.crm.feature.entityparser.util.fuzzywuzzy.FuzzySearch
import com.zoho.crm.feature.entityparser.constants.UnWantedElements
import com.zoho.crm.feature.entityparser.outputs.Company
import com.zoho.crm.feature.entityparser.util.{ApproximateStringMatching, EntityParserUtil, FuzzyBasedStringMatching, TitleExtraction}
import com.zoho.crm.ml.nlp.constants.{SPECIALCHAR, TITLES}

class CompanyExtraction(entityEssentials: ParsedEntity) extends EntityExtraction {

  def extract: ExtractedEntity = { //scalastyle:ignore
    try {
      val titleMatch = preProcess
      val companyNameExtraction = process(titleMatch)
      postProcess(companyNameExtraction)
    } catch {
      case e: Exception =>
        logger.info("Company Extraction failed " + e.printStackTrace())
        Company(None)
    }
  }

  def preProcess: Array[String] = {
    val titleExtraction = new TitleExtraction(entityEssentials, TITLES.ORGANIZATION)
    titleExtraction.extract
  }

  def process(possibleStringMatch: Array[String]): String = { //scalastyle:ignore
    val companyNameFromEmail: String = EntityParserUtil.getPatternFromEmail(entityEssentials.emailId, "company")
    if ((UnWantedElements.DISPOSBALE_EMAIL contains companyNameFromEmail) || (UnWantedElements.MAJORDOMAINS contains companyNameFromEmail))
      possibleStringMatch.filterNot(x => UnWantedElements.DISPOSBALE_EMAIL contains companyNameFromEmail) match {
        case x if x.length == 0 => SPECIALCHAR.EMPTYSTRING
        case x => x(0)
      } else {
      val possibleCompany = possibleStringMatch.filter(FuzzySearch.ratio(companyNameFromEmail, _) > 60)
      if (possibleCompany.length != 0) {
        possibleCompany(0)
      } else {
        val fromEmail = if (!entityEssentials.personName.equals("")) {
          val fuzzyBasedStringExtraction = new FuzzyBasedStringMatching(entityEssentials.personName, companyNameFromEmail)
          fuzzyBasedStringExtraction.extract()
        } else {
          ("", 0.0)
        }
        val fuzzyBasedStringExtraction = new FuzzyBasedStringMatching(entityEssentials.textContainingEntities, companyNameFromEmail)
        val fuzzyMatch = fuzzyBasedStringExtraction.extract()
        if (fromEmail._1.equals("")) {
          if (FuzzySearch.ratio(fuzzyMatch._1, companyNameFromEmail) >= 60)
            fuzzyMatch._1
          else
            companyNameFromEmail
        } else {
          if (fromEmail._2 > 50 && fuzzyMatch._2 > 50) {
            if (fromEmail._2 > fuzzyMatch._2) {
              fromEmail._1
            } else {
              fuzzyMatch._1
            }
          } else {
            if (!fuzzyMatch._1.equals("")) {
              EntityParserUtil.longestCommonSubstring(companyNameFromEmail, fuzzyMatch._1)
            } else {
              companyNameFromEmail
            }

          }
        }
      }

    }

  }

  def postProcess[R](companyName: String): Company = {
    val cleanedName = companyName.replaceAll(
      SPECIALCHAR.NONAPLHANUMERIC,
      SPECIALCHAR.SPACE)
    val companyNameFromEmail: String = EntityParserUtil.getPatternFromEmail(entityEssentials.emailId, "company").replaceAll(
      SPECIALCHAR.NONAPLHANUMERIC,
      SPECIALCHAR.SPACE)
    cleanedName match {
      case x if x.length() == 0 || x.equals("") => Company(None)
      case x =>
//        val originalName = new FuzzyBasedStringMatching(entityEssentials.signature, x).extract(extractFromOriginal = true)
        val originalName = ApproximateStringMatching.firstMatch(entityEssentials.textContainingEntities, x)
        val normalizedName = EntityParserUtil.stringNormalization(originalName)
        if(cleanedName.equals(companyNameFromEmail)) Company(Option(cleanedName.split(SPECIALCHAR.SPACE).map(_.capitalize).mkString(SPECIALCHAR.SPACE))) else Company(Option(normalizedName))
    }


  }
}