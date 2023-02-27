package com.zoho.crm.feature.entityparser

trait EntityParserInput {

}

case class TextInput(text: String) extends EntityParserInput

case class EmailInput(emailContent: String, emailId: Option[String]) extends EntityParserInput