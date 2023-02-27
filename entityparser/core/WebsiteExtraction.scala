package com.zoho.crm.feature.entityparser.core

import com.zoho.crm.feature.entityparser.util.patterns.Patterns
import com.zoho.crm.feature.entityparser
import com.zoho.crm.feature.entityparser.{EmailInput, EntityExtraction, ExtractedEntity, ParsedEntity}
import com.zoho.crm.feature.entityparser.constants.EntityParserConstants
import com.zoho.crm.feature.entityparser.outputs.{SocialEntities, Website}
import com.zoho.crm.feature.entityparser.util.EntityParserUtil
import com.zoho.crm.ml.nlp.constants
import com.zoho.zia.utils.preprocessing.RegexBuilder
import play.api.libs.json.{JsObject, Json}

class WebsiteExtraction(entityEssentials: ParsedEntity) extends EntityExtraction {
  override def extract: ExtractedEntity = {
    val start = System.currentTimeMillis()
    val cleanedText = preProcess
    val websites = process(cleanedText)
    val end = System.currentTimeMillis() - start
    logger.info("Website Extraction Completed in "+end)
    Website(websites)
  }

  def preProcess: String = {
    val elementsToRemove =
      entityEssentials.textEnrichmentInput match{
        case _: EmailInput => Array(EntityParserConstants.EMAIL)
        case _ => Array(EntityParserConstants.EMAIL, EntityParserConstants.IP_ADDRESS, EntityParserConstants.VPA, EntityParserConstants.SOCIAL)
      }
    val strToRemove = elementsToRemove.map {
      case constants.TITLES.PERSON =>
        entityEssentials.annotatedMap.getOrElse(constants.TITLES.PERSON, Array[String]())
      case constants.TITLES.ORGANIZATION =>
        entityEssentials.annotatedMap.getOrElse(constants.TITLES.ORGANIZATION, Array[String]())
      case EntityParserConstants.EMAIL =>
        if (entityEssentials.emails != null) {
          entityEssentials.emails
        } else {
          entityEssentials.textContainingEntities.split("\n").flatMap(
            EntityParserUtil.findAllPatternsInString(RegexBuilder.emailPatternAdvance, _, useActor = true)
          )
        }
      case EntityParserConstants.IP_ADDRESS => entityEssentials.ipAddress
      case EntityParserConstants.VPA => entityEssentials.VPA
      case EntityParserConstants.SOCIAL => entityEssentials.socialLinks match {
        case Some(x) => Json.toJson(x)(SocialEntities.socialFormat).as[JsObject].value.flatMap(_._2.as[Array[String]]).toArray
        case None => Array[String]()
      }
    }
    EntityParserUtil.stringRemoval(strToRemove, entityEssentials.textContainingEntities)
  }

  def process(text: String): Array[String] = {
    text.split("\n")
      .flatMap(EntityParserUtil.findAllPatternsInString(Patterns.WEB_URL, _, useActor = true)
      )
  }
}

