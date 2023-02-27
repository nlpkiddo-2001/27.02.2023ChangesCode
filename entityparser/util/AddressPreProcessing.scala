package com.zoho.crm.feature.entityparser.util

import java.io.File
import java.util.logging.Logger
import java.util.regex.Pattern

import com.zoho.common.components.sas.PropertyUtil
import com.zoho.crm.feature.entityparser.ParsedEntity
import com.zoho.crm.feature.entityparser.constants.{EntityParserConstants, UnWantedElements}
import com.zoho.crm.feature.entityparser.outputs.{NameAttributes, SocialEntities}
import com.zoho.crm.ml.nlp.constants.SPECIALCHAR
import com.zoho.crm.model.ModelLoadHandler
import com.zoho.crm.utils.SignatureExtraction
import play.api.libs.json.{JsObject, Json}


/**
CODE UPDATES: Date:11-11-2019 
 In process function added split(" ") in the removeUnwantedString string iteration to remove the identified entities
 
 Example signature:
 	regards,
 	john doe
	sales & marketing director
	16 freedom st, deer hill, 58-500, poland
	
	Previously if Sales Marketing Director is the identified entity then there is no match with the signature to remove that.
	Hence by splitting the entities sales, marketing and director can be removed from the signature
**/

class AddressPreProcessing(entityEssentials: ParsedEntity) {
  val elementsToRemove: Array[String] = Array(entityEssentials.fax, entityEssentials.designation,
    entityEssentials.companyName, entityEssentials.name
    //  ,entityEssentials.primaryEmail,entityEssentials.secondaryEmail
    //  ,entityEssentials.ExtractedWebsites
  ).map {
    case Some(x) => x match {
      case x: String => x
      case x: NameAttributes => x.actualName match {
        case Some(x) => x
        case None => ""
      }
    }
    case None => ""

  }.++(entityEssentials.socialLinks match {
    case Some(x) => Json.toJson(x)(SocialEntities.socialFormat).as[JsObject].value.flatMap(_._2.as[Array[String]])
    case None => List()
    })
    .++(entityEssentials.emails)
    .++(entityEssentials.websites).map(_.toLowerCase.trim).sortWith( _.length > _.length )
    
    
    //.getOrElse("actualName", "")) ++ entityEssentials.socialLinks.values
  def extract: String = { //scalastyle:ignore
    val preProcessed = preProcess
    val processed = process(preProcessed) //scalastyle:ignore
    val postProcessed = postProcess(processed)
    postProcessed
  }

  private def preProcess: String = {

    entityEssentials.textContainingEntities.replaceAll(SPECIALCHAR.DOUBLESPACE, SPECIALCHAR.EMPTYSTRING).toLowerCase()
  }

  private def process(signature: String): String = { //scalastyle:ignore
    //regex to remove web:, phone:, e: etc
    val attributesRegex = "(^[\\w\\d].{0,3}(([^\\n\\s\\w\\d,]+$)))"
    var cleanedSignature = removeUnwantedPhoneNos(signature) //scalastyle:ignore
    cleanedSignature = elementsToRemove.foldLeft(cleanedSignature)((a, b) => removeUnwantedString(a, b.split(" "), SPECIALCHAR.EMPTYSTRING))
    cleanedSignature = removeUnWantedElements(cleanedSignature)
    cleanedSignature.split("\n").map(_.replaceAll(attributesRegex, "")).mkString("\n")
  }

  private def postProcess(cleanedStr: String): String = {
    AddressPreProcessing.NORMALISED_ADDRESS.foldLeft(cleanedStr)((x, f) => x.replaceAll("\\b" + f._1 + "\\b", f._2))
  }
  private def removeUnwantedString(baseStr: String, strToRemove: Array[String], replacementChar: String): String = {
    strToRemove.foldLeft(baseStr.toLowerCase())((a, b) => a.replaceAll(Pattern.quote(b.toLowerCase()), replacementChar))
  }

  private def removeUnwantedString(baseStr: String, strToRemove: String, replacementChar: String): String = {
    baseStr.toLowerCase().replaceAll(Pattern.quote(strToRemove.toLowerCase()), SPECIALCHAR.EMPTYSTRING)
  }

  private def removeUnwantedString(baseStr: String, strToRemove: Map[String, Array[String]], replacementChar: String): String = {
    strToRemove.foldLeft(baseStr.toLowerCase())((a, b) =>
      b._2.foldLeft(a)((a, j) =>
        a.toLowerCase.replace(Pattern.quote(j.toLowerCase()), SPECIALCHAR.EMPTYSTRING)))
    // formatedPhoneNumbers.foldLeft(phoneData)((a, b) => a.replaceAllLiterally(b._1, b._2))
  }

  private def removeUnWantedElements(baseStr: String): String = {
    val removeUnWantedElements = removeUnWantedWords(baseStr)
    removeUnWantedElements.split(SPECIALCHAR.NEWLINE).map(line => {
      line.split(SPECIALCHAR.SPACE).filterNot(word => word.length().equals(1) && !word.matches("[A-Za-z0-9]+")).map(e => e).mkString(SPECIALCHAR.SPACE)
    }).mkString(SPECIALCHAR.NEWLINE)
  }

  private def removeUnWantedWords(baseStr: String): String = {
    val wordToRemove = Array[Array[String]](UnWantedElements.SOCIALMEDIA, UnWantedElements.CONTACT_LABELS, UnWantedElements.COMPLIMENTARYWORDS, SignatureExtraction.signatureWords)

    EntityParserUtil.stringRemoval(wordToRemove, baseStr)
  }

  private def removeUnwantedPhoneNos(baseStr: String): String = {
    entityEssentials.phoneNumber.foldLeft(baseStr)((a, b) => a.replaceAll("\\Q" + b.trim + "\\E", SPECIALCHAR.EMPTYSTRING))
  }

}

object AddressPreProcessing extends ModelLoadHandler{
  val loggerAddressPreprocessing: Logger = Logger.getLogger(this.getClass.getName)
  def loadFromFile(filePath: String): List[String] = {
    val file = scala.io.Source.fromFile(new File(filePath))
    val lines = file.getLines().map(x=> x).toList
    file.close()
    lines
  }
  lazy val STATE_CODE: List[String] = try {
    val lines = loadFromFile(PropertyUtil.getProperty("server.dir") + EntityParserConstants.STATE_CODE_DATA_PATH)
    lines.map({
      x =>
        val y = x.split(",")
        y(0).toLowerCase().trim()
    })

  } catch {
    case _: Exception =>
      loggerAddressPreprocessing.severe("EmailIE State code file reading Error")
      List[String]()
  }

  lazy val COUNTRY_CODE: List[String] = try {
    val lines = loadFromFile(PropertyUtil.getProperty("server.dir") + EntityParserConstants.COUNTRY_CODE_DATA_PATH)
    lines.map({
      x =>
        val y = x.split(",")
        y(0).toLowerCase().trim()
    })
  } catch {
    case _: Exception =>
      loggerAddressPreprocessing.severe("EmailIE Country code file reading Error")
      List[String]()
  }

  lazy val NORMALISED_ADDRESS: Map[String, String] = try {
    val lines = loadFromFile(PropertyUtil.getProperty("server.dir") + EntityParserConstants.NORMALIZED_ABBREVATIONS_DATA_PATH)
    lines.map({
      x =>
        val y = x.split(",")
        if ((!STATE_CODE.contains(y(1).toLowerCase().trim())) && (!COUNTRY_CODE.contains(y(1).toLowerCase().trim()))) {
          y(1).toLowerCase().trim() -> y(0).toLowerCase().trim()
        } else {
          y(1).toLowerCase().trim() -> y(1).toLowerCase().trim()
        }
    }).toMap
  } catch {
    case _: Exception =>
      loggerAddressPreprocessing.severe("EmailIE normalized abbrevation file reading Error")
      Map[String, String]()
  }

  override def isLoad(): Boolean = {
    true
  }

  override def loadModel(): Unit = {
    STATE_CODE
    COUNTRY_CODE
    NORMALISED_ADDRESS
  }
}