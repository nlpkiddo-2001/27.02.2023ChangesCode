package com.zoho.crm.feature.entityparser.outputs

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{Format, JsPath}

case class NameAttributes(
                           actualName: Option[String],
                           firstName: Option[String],
                           lastName: Option[String],
                           middleName: Option[String])

object NameAttributes{
  implicit val nameAttributesFormat: Format[NameAttributes] = (
    (JsPath \ "actualname").formatNullable[String] and
      (JsPath \ "firstname").formatNullable[String] and
      (JsPath \ "lastname").formatNullable[String] and
      (JsPath \ "middlename").formatNullable[String]
    ) (NameAttributes.apply, unlift(NameAttributes.unapply))

}
