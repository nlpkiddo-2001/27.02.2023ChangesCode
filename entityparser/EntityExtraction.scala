package com.zoho.crm.feature.entityparser

import java.util.logging.Logger

trait EntityExtraction {
  val logger: Logger = Logger.getLogger(this.getClass.getName)

  def extract: ExtractedEntity
}



