package com.zoho.crm.feature.entityparser.core

import com.zoho.crm.feature.entityparser.outputs.{EntityExtractionException, NoOutput, Signature}
import com.zoho.crm.feature.entityparser.util.EntityParserUtil
import com.zoho.crm.feature.entityparser.{EmailInput, EntityExtraction, ExtractedEntity, ParsedEntity}
import com.zoho.crm.ml.nlp.constants.{NLP, SPECIALCHAR}
import com.zoho.crm.utils.{DetectHTML, EmailThreads, HTMLCleaning}
import com.zoho.zia.api.textservice.response.ResponseCodes
import edu.stanford.nlp.simple.Sentence
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils

import scala.collection.JavaConversions.asScalaBuffer


class SignatureExtraction(entityEssentials: ParsedEntity) extends EntityExtraction {
  val signatureWords: Array[String] = Array("regards","thanks","thank","faithfully","sincerely","--",
      "rgds","best","Cheers","affectionately","best",
      "best regards","best wishes","best","bgif",
      "cheers","ciao","cordially","cordially yours","cordially",
      "fond regards","kind regards","many thanks","my best",
      "regards","regards","respectfully yours","respectfully",
      "sent from my iphone ","sincerely","sincerely yours",
      "sincerely","talk soon","thank you","thank you","thanking you",
      "thanks","thankyou","very truly yours","warm regards","warm regards",
      "warm wishes","warmly","with appreciation","with gratitude",
      "with sincere appreciation","with sincere thanks","with thanks",
      "yours respectfully","yours sincerely","yours truly","yours truly","kindly","cheers","regards")

  val NONASCII_REGEX = "[^\\p{ASCII}]"
  override def extract: ExtractedEntity = { //scalastyle:ignore
      val signatureExtraction = process(entityEssentials.textEnrichmentInput.asInstanceOf[EmailInput].emailContent)
      val signatureData = postProcess(signatureExtraction)
      signatureData
  }


  
  def process(emailContent: String): String = { //scalastyle:ignore
    val textEmailContent: String = if (DetectHTML.isHtml(emailContent)) {
      HTMLCleaning.extractTextFromHTML(
        StringUtils.strip(StringEscapeUtils.unescapeHtml4(emailContent), "\\n\\t ")
      )
    } else {
      emailContent
    }

    if(textEmailContent == null){
      throw EntityExtractionException(ResponseCodes.BAD_REQUEST, "EmailBody is empty")
    }

    val topThread = EmailThreads.getTopThread(textEmailContent)
    if(topThread.isEmpty){
      throw NoOutput(204)
    }
    val emailBody: String = StringUtils.stripAccents(topThread.get.text.split(SPECIALCHAR.NEWLINE)
      .map(e => if (!e.isEmpty) e.replaceAll("\\p{C}", "") else e)
      .mkString(SPECIALCHAR.NEWLINE)).trim().replaceAll("(?m)^[ \t]*\r?\n", "")

    if(emailBody.isEmpty || emailBody.split("\n").length ==0 || StringUtils.isEmpty(emailBody)){
      throw EntityExtractionException(ResponseCodes.BAD_REQUEST, "EmailBody is empty")
    }
    val signatureExtractor = com.zoho.crm.utils.SignatureExtraction.extract(emailBody)
    if(signatureExtractor == null || x.getSignature.equals("")){
      throw NoOutput(204)
    }
    signatureExtractor.getSignature
  }

  def postProcess(signatureData: String): Signature = {
    val start: Long = System.currentTimeMillis()
    val processedString = signatureData.replaceAll(NONASCII_REGEX, SPECIALCHAR.EMPTYSTRING).
      replaceAll(SPECIALCHAR.QUESTIONMARK, SPECIALCHAR.EMPTYSTRING)
    val cleanedSignature = disclaimerRemoval(processedString).split(SPECIALCHAR.NEWLINE).filter(!_.isEmpty).mkString(SPECIALCHAR.NEWLINE)
    if (cleanedSignature.isEmpty) {
      throw NoOutput(204)
    }
    val end = System.currentTimeMillis() - start
    logger.info("#################### Post Process Completed "+end)
    Signature(EntityParserUtil.normalizeSpaceAndEmptyLines(cleanedSignature))
  }

  private def disclaimerRemoval(signatureData: String): String = {
    val stringBlocks = {
      signatureData.split(SPECIALCHAR.BLOCKSEPARATION) match {
        case x if x.length.equals(1) => signatureData.split(SPECIALCHAR.NEWLINE)
        case x                  => x
      }
    }.filterNot(e => e.matches(SPECIALCHAR.NONAPLHANUMERIC))

    val possibleNoisySignature = stringBlocks.filterNot(e => e.split(SPECIALCHAR.SPACE).length > 25 &&
      new Sentence(e).posTags().count(tags => tags.contains(NLP.VERB)) >= 7)
    val cleanedSignature = disclaimerDetector(possibleNoisySignature.reverse).mkString(SPECIALCHAR.NEWLINE)
    cleanedSignature
  }

  private def disclaimerDetector(signatureData: Array[String]): Array[String] = {
    val signatureBlocks = signatureData.filterNot(e =>
      StringUtils.containsOnly(e, SPECIALCHAR.ALLSPECIALCHAR) || StringUtils.isBlank(e)).map(block => {
      val annotatedSen = new Sentence(block)
      val posTag = annotatedSen.posTags()
      val vCount = posTag.count(_.contains(NLP.VERB))
      val wCount = posTag.count(!_.matches("[^A-Za-z0-9]"))
      val sentenceVerbPercentage = vCount / wCount.toDouble
      !(vCount >= 3 && sentenceVerbPercentage >= 0.08 && sentenceVerbPercentage <= 0.35)
    })
    signatureData.filterNot(e =>
      StringUtils.containsOnly(e, SPECIALCHAR.ALLSPECIALCHAR) || StringUtils.isBlank(e)).zipWithIndex.zip(signatureBlocks).
      filter(e => e._2 || e._1._2 >= 3).map(e => e._1._1).reverse.filterNot(e => e.isEmpty || e.length() <= 2)
  }

}