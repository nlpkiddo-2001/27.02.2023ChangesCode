package com.zoho.crm.feature.entityparser.core

import com.zoho.crm.feature.entityparser.outputs.{EntityExtractionException, NoOutput}
import com.zoho.crm.feature.entityparser.{EmailInput, EntityExtraction, ExtractedEntity, ParsedEntity}
import com.zoho.crm.ml.nlp.constants.SPECIALCHAR
import com.zoho.crm.utils.{DetectHTML, EmailThreads, HTMLCleaning}
import com.zoho.zia.api.textservice.response.ResponseCodes
import org.apache.commons.text.StringEscapeUtils
import org.apache.commons.lang3.StringUtils
import com.zoho.crm.feature.entityparser.outputs.Enquiry
import com.zoho.crm.feature.entityparser.util.EntityParserUtil

class EnquiryExtraction(entityEssentials: ParsedEntity) extends EntityExtraction {

  private final val ENQUIRY_EXTRACTION_WORDS: Array[String] = Array(
    "enquiry","inquiry","question","request","concern","use","access","demo","problem",
    "guide" ,"guidance", "issue", "facing","support", "complaince"
  )
  // a simple array to extract the enquiry

  val NONASCII_REGEX = "[^\\p{ASCII}]"


  override def extract: ExtractedEntity = {
    val enquiryExtraction = process(entityEssentials.textEnrichmentInput.asInstanceOf[EmailInput].emailContent)
    val enquiryData = postProcess(enquiryExtraction)
    enquiryData
  }

  private def process(emailBody: String):String = {
    val textEmailBody: String = if(DetectHTML.isHtml(emailBody)) {
      HTMLCleaning.extractTextFromHTML(
        StringUtils.strip(StringEscapeUtils.unescapeHtml4(emailBody), "\\n\\t ")
      )
    }else{
      emailBody
    }
    if(textEmailBody==null){
      throw EntityExtractionException(ResponseCodes.BAD_REQUEST, "EmailBody is empty")
    }
    val topThread = EmailThreads.getTopThread(textEmailBody)
    if (topThread.isEmpty) {
      throw NoOutput(204)
    }

    val emailBody: String = StringUtils.stripAccents(topThread.get.text.split(SPECIALCHAR.NEWLINE)
      .map(e => if (!e.isEmpty) e.replaceAll("\\p{C}", "") else e)
      .mkString(SPECIALCHAR.NEWLINE)).trim().replaceAll("(?m)^[ \t]*\r?\n", "")

    if (emailBody.isEmpty || emailBody.split("\n").length == 0 || StringUtils.isEmpty(emailBody)) {
      throw EntityExtractionException(ResponseCodes.BAD_REQUEST, "EmailBody is empty")
    }

    val enquiryExtractor = com.zoho.crm.utils.EnquiryExtraction.extract(emailBody=emailBody)
    if(enquiryExtractor == null){
      throw NoOutput(204)
    }
    enquiryExtractor.getEnquiry

    

  }
  def postProcess(enquiryData:String):Enquiry = {
    val start: Long = System.currentTimeMillis()
    val processedString = enquiryData.replaceAll(NONASCII_REGEX, SPECIALCHAR.EMPTYSTRING).
      replaceAll(SPECIALCHAR.QUESTIONMARK, SPECIALCHAR.EMPTYSTRING)
    if(processedString.isEmpty){
      throw NoOutput(204)
    }
    val end = System.currentTimeMillis() - start
    logger.info("############ Post Process Completed "+ end)
    Enquiry(EntityParserUtil.normalizeSpaceAndEmptyLines(processedString))
  }
}
