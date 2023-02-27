package com.zoho.crm.feature.entityparser.core

import com.zoho.crm.feature.entityparser.constants.{EntityParserConstants, SocialLinkRegex, TextBasedMatching, UnWantedElements}
import com.zoho.crm.feature.entityparser.outputs.{Social, SocialEntities}
import com.zoho.crm.feature.entityparser.util.{EntityParserUtil, TextBasedPatternMatching}
import com.zoho.crm.feature.entityparser._
import com.zoho.crm.ml.nlp.constants.SPECIALCHAR
import com.zoho.crm.utils.{DetectHTML, HTMLCleaning}
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils

import scala.collection.immutable.HashMap




class SocialLinkExtraction(entityEssentials: ParsedEntity) extends EntityExtraction {

  def extract: ExtractedEntity = { //scalastyle:ignore
    try{
       val socailLinksData = preProcess
        val socialLinkExtraction = process(socailLinksData)
        postProcess(socialLinkExtraction)
    }catch{
      case e: Exception => 
        logger.info("Social Extraction Failed"+e.printStackTrace())
        Social(None)
    }
   
  }

  def preProcess: HashMap[String, String] = {

    HashMap(
      (EntityParserConstants.FACEBOOK, SocialLinkRegex.FACEBOOK_REGEX),
      (EntityParserConstants.TWITTER, SocialLinkRegex.TWITTER_REGEX),
      (EntityParserConstants.SKYPE, SocialLinkRegex.SKYPE_REGEX),
      (EntityParserConstants.LINKEDIN, SocialLinkRegex.LINKEDIN)
    )
  }

  def process(socialRegex: HashMap[String, String]): Map[String, Array[String]] = { //scalastyle:ignore
    val socialLinks =
      socialRegex.map(social => {
        val links: List[String] = entityEssentials.textEnrichmentInput match {
          case x: EmailInput =>
            if(DetectHTML.isHtml(x.emailContent)){
              val HTMLContent = HTMLCleaning.extractTextFromHTML(
                StringUtils.strip(StringEscapeUtils.unescapeHtml4(x.emailContent), "\\n\\t ")
              )
              EntityParserUtil.findAllPattern(HTMLContent, social._2.r)
            }else{
              val cleanedSignature = entityEssentials.websites.foldLeft(entityEssentials.textContainingEntities)(
                (signature, website) => signature.replaceAll(website, "")
              )
              EntityParserUtil.findAllPattern(cleanedSignature, social._2.r)
            }
          case x: TextInput => EntityParserUtil.findAllPattern(x.text, social._2.r)
        }
        if (links.nonEmpty) {
          social._1 -> links.toArray
        } else {
          val textBasedPatternMatching = new TextBasedPatternMatching(entityEssentials.textContainingEntities, social._1, TextBasedMatching.SOCIALLINK_CHARCONSIDERED)
          val extractedSocialMedia = EntityParserUtil.stringRemoval(
            Array(UnWantedElements.SOCIALMEDIA), textBasedPatternMatching.extract).
            replaceAll(SPECIALCHAR.NONAPLHANUMERIC, SPECIALCHAR.EMPTYSTRING)
          if (extractedSocialMedia.split(" ").length > 1 || extractedSocialMedia.length() < 4) social._1 -> Array[String]() else {
            social._1 -> Array(extractedSocialMedia)
          }
        }
      })
    socialLinks
  }

  def postProcess(socialLinks: Map[String, Array[String]]): Social = {
    val processedSocialLinks = socialLinks.filter(_._2.nonEmpty)
    processedSocialLinks match {
      case x if x.isEmpty => Social(None)
      case x => Social(
          Option(SocialEntities(
            x.getOrElse(EntityParserConstants.SKYPE, Array()),
            x.getOrElse(EntityParserConstants.FACEBOOK, Array()),
            x.getOrElse(EntityParserConstants.TWITTER, Array()),
            x.getOrElse(EntityParserConstants.LINKEDIN, Array())
          ))
      )
    }
    
  }
}