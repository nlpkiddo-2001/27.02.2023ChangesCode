package com.zoho.crm.feature.entityparser.outputs

import com.zoho.crm.feature.entityparser.constants.EntityParserConstants
import com.zoho.crm.ml.nlp.constants.{ADDRESS, TITLES}
import com.zoho.zia.api.textservice.response.ErrorResponse
import com.zoho.zia.api.textservice.utils.TextServiceFormats
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{JsPath, JsValue, Json, Writes}

case class Entities(
                     email: Array[String]=Array(),
                     website: Array[String]=Array(),
                     fax: Option[String]=None,
                     company: Option[String]=None,
                     designation: Option[String]=None,
                     phoneNo: Array[String]=Array(),
                     social: Option[SocialEntities]=None,
                     name: Option[NameAttributes]=None,
                     address: Array[AddressEntities]=Array(),
                     ipAddress: Array[String]=Array(),
                     vpa: Array[String]=Array()
                   )

object Entities extends TextServiceFormats{
  implicit val entitiesWrites: Writes[Entities] = (
      (JsPath \ EntityParserConstants.EMAIL).write[Array[String]] and
      (JsPath \ EntityParserConstants.WEBSITE).write[Array[String]] and
      (JsPath \ EntityParserConstants.FAX).writeNullable[String] and
      (JsPath \ EntityParserConstants.COMPANY).writeNullable[String] and
      (JsPath \ TITLES.DESIGNATION).writeNullable[String] and
      (JsPath \ EntityParserConstants.PHONE).write[Array[String]] and
      (JsPath \ EntityParserConstants.SOCIAL).writeNullable[SocialEntities] and
      (JsPath \ EntityParserConstants.NAME).writeNullable[NameAttributes] and
      (JsPath \ ADDRESS.ADDRESS).write[Array[AddressEntities]] and
      (JsPath \ EntityParserConstants.IP_ADDRESS).write[Array[String]] and
      (JsPath \ EntityParserConstants.VPA).write[Array[String]]
    ) (unlift(Entities.unapply))

  implicit val eitherWriters: Writes[Either[ErrorResponse, Entities]] = new Writes[Either[ErrorResponse, Entities]] {
    override def writes(o : Either[ErrorResponse, Entities]): JsValue = o match {
      case Right(x) => Json.toJson(x)
      case Left(x) => Json.toJson(x)
    }
  }
}