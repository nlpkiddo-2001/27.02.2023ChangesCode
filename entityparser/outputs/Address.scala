package com.zoho.crm.feature.entityparser.outputs

import com.zoho.crm.feature.entityparser.ExtractedEntity

case class Address(address: Array[AddressEntities]) extends ExtractedEntity
