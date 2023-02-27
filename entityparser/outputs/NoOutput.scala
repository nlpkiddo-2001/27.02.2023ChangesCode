package com.zoho.crm.feature.entityparser.outputs

final case class NoOutput(statusCode: Int,
                          private val cause: Throwable = None.orNull) extends  Exception
