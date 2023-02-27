package com.zoho.crm.feature.entityparser.outputs

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json._

case class AddressEntities(
                            rank: Int,
                            postcode: Option[String],
                            city: Option[String],
                            country: Option[String],
                            state: Option[String]
                          )

object AddressEntities {
  implicit val addressWrites: Writes[AddressEntities] = (
    (JsPath \ "rank").write[Int] and
      (JsPath \ "zipcode").writeNullable[String] and
      (JsPath \ "city").writeNullable[String] and
      (JsPath \ "country").writeNullable[String] and
      (JsPath \ "state").writeNullable[String]
    ) (unlift(AddressEntities.unapply))

  implicit val addressArrWrites: Writes[Array[AddressEntities]] = new Writes[Array[AddressEntities]] {
    override def writes(o: Array[AddressEntities]): JsValue = {
      JsArray(o.map(Json.toJson(_)(addressWrites)))
    }
  }
}
