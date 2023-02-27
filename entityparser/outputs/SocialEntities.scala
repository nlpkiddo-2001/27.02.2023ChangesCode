package com.zoho.crm.feature.entityparser.outputs

import com.zoho.crm.feature.entityparser.constants.EntityParserConstants
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{Format, JsPath}

case class SocialEntities(
                           skype: Array[String] = Array(),
                           facebook: Array[String] = Array(),
                           twitter: Array[String] = Array(),
                           linkedin: Array[String] = Array()
                         )

object SocialEntities{

  implicit val socialFormat: Format[SocialEntities] = (
      (JsPath \ EntityParserConstants.SKYPE).format[Array[String]] and
      (JsPath \ EntityParserConstants.FACEBOOK).format[Array[String]] and
      (JsPath \ EntityParserConstants.TWITTER).format[Array[String]] and
        (JsPath \ EntityParserConstants.LINKEDIN).format[Array[String]]
    ) (SocialEntities.apply, unlift(SocialEntities.unapply))

}
