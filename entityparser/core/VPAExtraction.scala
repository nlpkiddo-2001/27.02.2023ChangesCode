package com.zoho.crm.feature.entityparser.core

import com.zoho.crm.feature.entityparser.outputs.VPA
import com.zoho.crm.feature.entityparser.{EntityExtraction, ExtractedEntity, ParsedEntity}
import com.zoho.crm.feature.entityparser.util.patterns.Patterns
import com.zoho.crm.feature.entityparser.util.EntityParserUtil



class VPAExtraction(entityEssentials: ParsedEntity) extends EntityExtraction{
  override def extract: ExtractedEntity = {
    val start = System.currentTimeMillis()
    val emailsInSignature: Array[String] = entityEssentials.textContainingEntities.split("\n").flatMap(
      EntityParserUtil.findAllPatternsInString(Patterns.VPA, _, useActor = true)
    )
    val end = System.currentTimeMillis() - start
    logger.info("VPA Extraction Completed in "+end)
    VPA(emailsInSignature)
  }
}
