package com.zoho.crm.feature.entityparser.core

import com.zoho.crm.feature.entityparser.outputs.Designation
import com.zoho.crm.feature.entityparser.{EntityExtraction, ExtractedEntity, ParsedEntity}
import com.zoho.crm.feature.entityparser.util.{ApproximateStringMatching, EntityParserUtil}
import com.zoho.crm.ml.nlp.constants.{NLP, TITLES}
import com.zoho.crm.ml.utils.CommonUtil

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._




//Uses extract postprocess and process method to extract the designation from signature
class DesignationExtraction(entityEssentials: ParsedEntity) extends EntityExtraction {
 
  val signature: String = entityEssentials.textContainingEntities
  
  //extract calls the process method and get the extracted designation and post process it for removing unwanted characters
   def extract :ExtractedEntity = { //scalastyle:ignore
      val start = System.currentTimeMillis()
      val designationExtract : List[String] = process(signature)
      if (designationExtract.nonEmpty)
      {
        val end = System.currentTimeMillis() - start
        logger.info("Designations Extraction Completed in "+end)
        postProcess(designationExtract)
      }
      else{
       Designation(None)
      }
  
  }


  def process(signature: String) :List[String] = { //scalastyle:ignore    
    val modelTypes: Array[String] = Array(TITLES.DESIGNATION)
    val preprocessedSignature: String = signature.split("\n+").map(_.replaceAll("[^a-zA-Z .]", "")).mkString("\n")
    val entityDetected = CommonUtil.findEntities(preprocessedSignature, modelTypes, NLP.SIMPLE)
     if (entityDetected.contains(TITLES.DESIGNATION)) {
      entityDetected(TITLES.DESIGNATION).asScala.toList
       
     }
     else{
      List[String]()
     }
  }

  def postProcess(designationData: List[String]): Designation = {
    EntityParserUtil.extractBestMatch(signature.split("\n+").map(_.replaceAll("[^a-zA-Z .]", "")).mkString("\n"), designationData.toArray) match {
        case x if x.length == 0 =>
          if(designationData.headOption.isEmpty)
            Designation(designationData.headOption)
          else {
            val originalName = ApproximateStringMatching.firstMatch(entityEssentials.textContainingEntities, designationData.head)
            val normalization = EntityParserUtil.stringNormalization(originalName)
            Designation(Some(normalization))
          }
        case x =>
          if(x.headOption.isEmpty)
            Designation(x.headOption)
          else{
            val originalName = ApproximateStringMatching.firstMatch(entityEssentials.textContainingEntities, x.headOption.get)
            val normalization = EntityParserUtil.stringNormalization(originalName)
            Designation(Some(normalization))
          }
      }
  }


}