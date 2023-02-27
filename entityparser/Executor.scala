package com.zoho.crm.feature.entityparser

import java.util.logging.Logger

import com.zoho.crm.feature.entityparser.constants.EntityParserConstants
import com.zoho.crm.ml.nlp.constants.{ADDRESS, TITLES}

object Executor { //scalastyle:ignore
  val LOGGER: Logger = Logger.getLogger(this.getClass.getName)
  def apply(entityType: String, parsedEntity: ParsedEntity): Unit = { //scalastyle:ignore
    val start = System.currentTimeMillis()
    val entityData = Extraction(entityType, parsedEntity).extract
    parsedEntity.setEntity(entityData)
    val end = System.currentTimeMillis() - start
    LOGGER.info(entityType+ " Completed in "+end)
  }

  def executionOrder(entitiesOrder: Array[String]): Array[String] ={
    val executionOrd = Array[String](
      EntityParserConstants.SIGNATURE,
      EntityParserConstants.ENQUIRY,
      TITLES.DESIGNATION,
      EntityParserConstants.EMAIL,
      EntityParserConstants.VPA,
      EntityParserConstants.IP_ADDRESS,
      EntityParserConstants.PHONE,
      EntityParserConstants.SOCIAL,
      EntityParserConstants.WEBSITE,
      EntityParserConstants.NAME,
      EntityParserConstants.COMPANY,
      ADDRESS.ADDRESS
    )

    entitiesOrder.sortWith((a, b)=> executionOrd.indexOf(a) < executionOrd.indexOf(b))

  }
}
