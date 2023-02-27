package com.zoho.crm.feature.entityparser

import com.zoho.crm.feature.entityparser.constants.EntityParserConstants
import com.zoho.crm.feature.entityparser.core._
import com.zoho.crm.ml.nlp.constants.{ADDRESS, TITLES}

object Extraction {

  def apply(entity: String, parsedEntity: ParsedEntity): EntityExtraction = { //scalastyle:ignore
    entity match {
      case EntityParserConstants.SIGNATURE   => new SignatureExtraction(parsedEntity)
      // added by me
      case EntityParserConstants.ENQUIRY => new EnquiryExtraction(parsedEntity)
      case ADDRESS.ADDRESS                 => new AddressExtraction(parsedEntity)
      case EntityParserConstants.PHONE           => new PhoneExtraction(parsedEntity)
      case EntityParserConstants.SOCIAL => new SocialLinkExtraction(parsedEntity)
      case EntityParserConstants.NAME           => new NameExtraction(parsedEntity)
      case EntityParserConstants.COMPANY        => new CompanyExtraction(parsedEntity)
      case TITLES.DESIGNATION        =>  new DesignationExtraction(parsedEntity)
      case EntityParserConstants.EMAIL    =>  new EmailExtraction(parsedEntity)
      case EntityParserConstants.WEBSITE    =>  new WebsiteExtraction(parsedEntity)
      case EntityParserConstants.IP_ADDRESS    =>  new IPAddressExtraction(parsedEntity)
      case EntityParserConstants.VPA    =>  new VPAExtraction(parsedEntity)
      case _                               => throw new Exception
    }
  }
}
