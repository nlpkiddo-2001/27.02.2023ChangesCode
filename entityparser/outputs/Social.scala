package com.zoho.crm.feature.entityparser.outputs

import com.zoho.crm.feature.entityparser.ExtractedEntity

case class Social(socialEntities: Option[SocialEntities]) extends ExtractedEntity
