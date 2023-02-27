package com.zoho.crm.feature.entityparser.outputs

import com.zoho.zia.api.textservice.response.ResponseCodes

final case class EntityExtractionException(errorCodes: ResponseCodes,
                                           message : String,
                                           private val cause: Throwable = None.orNull) extends Exception(message, cause)
