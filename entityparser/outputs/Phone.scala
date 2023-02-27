package com.zoho.crm.feature.entityparser.outputs

import com.zoho.crm.feature.entityparser.ExtractedEntity

case class Phone(PhoneNo: Array[String], Fax: Option[String]) extends ExtractedEntity
