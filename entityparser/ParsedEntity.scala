package com.zoho.crm.feature.entityparser

import java.util.regex.Pattern

import com.zoho.crm.feature.entityparser.constants.UnWantedElements
import com.zoho.crm.feature.entityparser.outputs._
import com.zoho.crm.feature.entityparser.util.EntityParserUtil
import com.zoho.crm.feature.entityparser.util.patterns.EmailCleaning
import com.zoho.crm.ml.nlp.common.StanfordSimpleNER
import com.zoho.crm.ml.nlp.constants.{ADDRESS, SPECIALCHAR, TITLES}
import com.zoho.crm.ml.utils.CommonUtil
import org.apache.commons.text.StringEscapeUtils
import play.api.libs.json.{JsObject, Json}

import scala.collection.JavaConversions._

class ParsedEntity(input: EntityParserInput) {
  val textEnrichmentInput: EntityParserInput = input
  val (personNameOption, emailIdOption) = {
    input match {
      case x: EmailInput =>
        if (x.emailId.isDefined)
          EmailCleaning.extractEmailAndName(x.emailId.get)
        else
          (None, None)
      case _ => (None, None)
    }
  }
  val personName: String = personNameOption.getOrElse("")
  val emailId: String = emailIdOption.getOrElse("")
  //  lazy val emailsInSignature: Array[String] = {
  //    val start = System.currentTimeMillis()
  //    val emails = signature.split("\n").flatMap(
  //      AutoEnrichmentUtils.findAllPatternsInString(RegexBuilder.emailPatternAdvance, _, true)
  //    )
  //    val end = System.currentTimeMillis() - start
  //    logger.info("Email Extraction Completed in "+end)
  //    emails
  //  }
  //  lazy val websitesInSignature: Array[String] = {
  //    val start = System.currentTimeMillis()
  //    val websites = emailsInSignature
  //      .foldLeft(signature)((sign, email)=>sign.replaceAll(email, ""))
  //      .split("\n")
  //      .flatMap(
  //        AutoEnrichmentUtils.findAllPatternsInString(Website.WEBSITEREGEX, _, true)
  //      )
  //    val end = System.currentTimeMillis() - start
  //    logger.info("Website Extraction Completed in "+end)
  //    websites
  //  }
  private var _textContainingEntities: String = {
    input match {
      case x: TextInput => EntityParserUtil.normalizeSpaceAndEmptyLines(x.text)
      case _: EmailInput => null
    }
  } //scalastyle:ignore

  private var _phoneNumber: Array[String] = Array[String]()
  private var _fax: Option[String] = None
  private var _name: Option[NameAttributes] = None
  private var _companyName: Option[String] = None
  private var _socialLinks: Option[SocialEntities] = None
  private var _designation: Option[String] = None
  private var _address: Array[AddressEntities] = Array[AddressEntities]()
  private var _emails: Array[String] = Array[String]()
  private var _website: Array[String] = Array[String]()
  private var _ipAddress: Array[String] = Array[String]()
  private var _vpa: Array[String] = Array[String]()

  def textContainingEntities: String = _textContainingEntities //scalastyle:ignore

  def fax: Option[String] = _fax //scalastyle:ignore
  def name: Option[NameAttributes] = _name //scalastyle:ignore
  def companyName: Option[String] = _companyName //scalastyle:ignore
  def socialLinks: Option[SocialEntities] = _socialLinks //scalastyle:ignore
  def address: Array[AddressEntities] = _address //scalastyle:ignore
  def phoneNumber: Array[String] = _phoneNumber //scalastyle:ignore
  def designation: Option[String] = _designation

  def ipAddress: Array[String] = _ipAddress

  def VPA: Array[String] = _vpa

  def emails: Array[String] = _emails

  def websites: Array[String] = _website

  //  def getWebsite: Option[String] = {
  //    val start = System.currentTimeMillis()
  //    val urlPattern = Pattern.compile("^(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$")
  //    val email = UnWantedElements.EMAIL.toString.r.findAllIn(signature).toList.foldLeft(signature)((i, j)=>i.replaceAll(j, "")).split("\n").flatMap(x=> {
  //      val matcher: Matcher = urlPattern.matcher(x)
  //      val urlList: ArrayBuffer[String] = ArrayBuffer()
  //      while(matcher.find())
  //        urlList += x.slice(matcher.start(1), matcher.end)
  //      urlList.toList
  //    }).headOption
  //    val end = System.currentTimeMillis() - start
  //    logger.info("Website Extraction Completed in "+end)
  //    email
  //  }
  //
  //  def getEmail(index: Int): Option[String] = {
  //    val start = System.currentTimeMillis()
  //    val email = UnWantedElements.EMAIL.toString.r.findAllIn(signature).toList.lift(index)
  //    val end = System.currentTimeMillis() - start
  //    logger.info("Email Extraction Completed in "+end)
  //    email
  //  }

  lazy val annotatedMap: Map[String, Array[String]] = {
    val entityTypes = Array[String](TITLES.PERSON, TITLES.ORGANIZATION)
    val signatureAnnotation = StanfordSimpleNER.entityFinder(entityDetectionPreProcessing(textContainingEntities, this), entityTypes)
    val personAndOrg = collection.mutable.Map(signatureAnnotation.toSeq: _*)
    val locEntities = CommonUtil.findEntities(textContainingEntities)
    if (locEntities.containsKey(TITLES.ORGANIZATION)) {
      val arr = personAndOrg(TITLES.ORGANIZATION)
      var entities = collection.mutable.ArrayBuffer(arr: _*)
      locEntities.foreach(l => {
        if (!l._1.equals(ADDRESS.CITY)) {
          entities = entities.filterNot(e => l._2.contains(e))
        }
      })
      personAndOrg.update(TITLES.ORGANIZATION, entities.toArray)
    }
    personAndOrg.toMap
  }


  //  private def getDesignation = {
  //    val start = System.currentTimeMillis()
  //    val modelTypes: Array[String] = Array(TITLES.DESIGNATION)
  //    val entityDetected = CommonUtil.findEntities(signature.split("\n+").map(_.replaceAll("[^a-zA-Z .]", "")).mkString("\n"), modelTypes, NLP.SIMPLE)
  //    val end = System.currentTimeMillis() - start
  //    logger.info("Designations Extraction Completed in "+end)
  //    if (entityDetected.contains(TITLES.DESIGNATION)) {
  //      extractBestMatch(signature.split("\n+").map(_.replaceAll("[^a-zA-Z .]", "")).mkString("\n"), entityDetected(TITLES.DESIGNATION).asScala.toArray) match {
  //        case x if x.length == 0 => entityDetected(TITLES.DESIGNATION).headOption
  //        case x => x.headOption
  //      }
  //    } else {
  //      None
  //    }
  //  }

  def getEntities: Entities = {
    Entities(
      emails,
      websites,
      fax,
      companyName,
      designation,
      phoneNumber,
      socialLinks,
      name,
      address,
      ipAddress,
      VPA
    )
    //    val primaryPhone = if(phonenumber.length >= 1) phonenumber(0) else ""
    //    val secondaryPhone = if(phonenumber.length >= 2) phoneNumber(1) else ""
    //    val strValues = Seq(
    //        "email" -> emailId,
    //        com.zoho.crm.feature.autoenrichment.constant.Phone.FAX -> fax,
    //        Titles.COMPANY -> companyName,
    //        TITLES.DESIGNATION -> designation,
    //        com.zoho.crm.feature.autoenrichment.constant.Phone.PRIMARYPHONENO -> primaryPhone,
    //        com.zoho.crm.feature.autoenrichment.constant.Phone.SECONDARYPHONENO -> secondaryPhone
    //    ).filter(!_._2.equals("")).map(x => (x._1, JsString(x._2)))
    //
    //    val mapValues = Seq(
    //       SocialLink.SOCIALLINK -> socialLinks.toMap,
    //       Titles.NAME -> name.toMap
    //    ).filter(_._2.size != 0).map(x => (x._1, Json.toJson(x._2)))
    //
    //    val listValues = Seq(
    //       ADDRESS.ADDRESS -> address
    //    ).filter(_._2.length != 0).map(x=> (x._1, Json.toJson(x._2.map(i => Json.toJson(i.toMap)))))

    //    JsObject((strValues.filter(!_._2.equals("")) ++ mapValues.filter(_._2.size != 0))).map(f => (f._1, JsString(f._2))))
    //    Json.obj(
    //        "status" -> 200,
    //        "person" -> JsObject(strValues ++ mapValues ++ listValues)
    //     )
    //    Json.obj(
    //      "email" -> emailId,
    //      constants.Phone.PRIMARYPHONENO  -> Json.toJson(phonenumber),
    //      constants.Phone.FAX -> fax,
    //      constants.SocialLink.SOCIALLINK -> Json.toJson(socialLinks.toMap),
    //      constants.Titles.NAME -> Json.toJson(name),
    //      constants.Titles.COMPANY -> companyName,
    //      ADDRESS.ADDRESS -> Json.toJson(address.map(i => Json.toJson(i.toMap))),
    //      TITLES.DESIGNATION -> designation
    //    )
  }

  def setEntity(entityData: ExtractedEntity): Unit = {

    entityData match {
      case x: Signature =>
        this._textContainingEntities = StringEscapeUtils.unescapeJava(x.signature).
          replaceAll(UnWantedElements.REMOVEQUOTES, SPECIALCHAR.EMPTYSTRING)

      case y: Phone =>
        this._phoneNumber = y.PhoneNo
        this._fax = y.Fax match {
          case Some(x) => if (x.equals("")) None else Option(x)
          case None => None
        }

      case y: Social => this._socialLinks = y.socialEntities match {
        case Some(x) => Option(x)
        case None => None
      }

      case y: Name =>
        this._name = y.name match {
          case Some(x) => Option(x)
          case None => None
        }

      case x: Company => this._companyName = x.company

      case x: Address =>
        this._address = x.address

      case x: Designation => this._designation = x.designation
      case x: Email => this._emails = x.email
      case x: Website => this._website = x.website
      case x: IPAddress => this._ipAddress = x.ipAddress
      case x: VPA => this._vpa = x.vpa
      case _ => throw new Exception("No entity found")
    }
  }



  // TODO Remove the util from ParsedEntity
  private def entityDetectionPreProcessing(signData: String, entityEssentials: ParsedEntity): String = {
    def removeUnwantedString(baseStr: String, strToRemove: Array[String], replacementChar: String): String = {
      strToRemove.foldLeft(baseStr.toLowerCase())((a, b) => a.replaceAll(Pattern.quote(b.toLowerCase()), replacementChar))
    }
    //    val signatureData = signData.split(SPECIALCHAR.NEWLINE).filter(_ != "")
    //    signatureData.slice(0, if (signatureData.length > 5) 5 else signatureData.length).map(e => { //scalastyle:ignore
    //      if (e.charAt(e.length() - 1) == SPECIALCHAR.COMMA) e else e + SPECIALCHAR.COMMA //scalastyle:ignore
    //    }).mkString(SPECIALCHAR.NEWLINE)
    val stripSpecialChar = "([^\\w\\d\\(\\)\\{\\}\\[\\]'\"><\\s`.]+)"
    val elementsToRemove: Array[String] = Array(entityEssentials.fax, entityEssentials.designation
      //  ,entityEssentials.primaryEmail,entityEssentials.secondaryEmail
      //  ,entityEssentials.ExtractedWebsites
    ).map {
      case Some(x) => x
      case None => ""
  
    }.++(entityEssentials.socialLinks match {
      case Some(x) => Json.toJson(x)(SocialEntities.socialFormat).as[JsObject].value.flatMap(_._2.as[Array[String]])
      case None => List()
      })
      .++(entityEssentials.emails)
      .++(entityEssentials.websites).map(_.toLowerCase.trim).sortWith( _.length > _.length )
    elementsToRemove.filter(_.nonEmpty).foldLeft(signData)((a, b) => removeUnwantedString(a, b.split(" "), SPECIALCHAR.EMPTYSTRING))
    .replaceAll(stripSpecialChar, "").split(SPECIALCHAR.NEWLINE).filter(_ != SPECIALCHAR.EMPTYSTRING).mkString(SPECIALCHAR.NEWLINE)

  }


}