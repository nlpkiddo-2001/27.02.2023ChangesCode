package com.zoho.crm.feature.entityparser.core

import com.zoho.crm.feature.entityparser.outputs.IPAddress
import com.zoho.crm.feature.entityparser.{EntityExtraction, ExtractedEntity, ParsedEntity}
import com.zoho.crm.feature.entityparser.util.patterns.Patterns
import com.zoho.crm.feature.entityparser.util.EntityParserUtil



class IPAddressExtraction(entityEssentials: ParsedEntity) extends EntityExtraction{
  override def extract: ExtractedEntity = {
    val start = System.currentTimeMillis()
    val emailsInSignature: Array[String] = entityEssentials.textContainingEntities.split("\n").flatMap(
      EntityParserUtil.findAllPatternsInString(Patterns.IP_ADDRESS, _, useActor = true)
    )
    val end = System.currentTimeMillis() - start
    logger.info("IPAddress Extraction Completed in "+end)
    IPAddress(emailsInSignature)
  }
}
