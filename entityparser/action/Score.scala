//scalastyle:off
//package com.zoho.crm.feature.entityparser.action
//
//import java.io.{BufferedReader, FileReader}
//import java.util.logging.{Level, Logger}
//
//import com.adventnet.crm.common.actions.CrmActionSupport
//import com.zoho.crm.feature.entityparser.util.fuzzywuzzy.FuzzySearch
//import com.zoho.crm.feature.entityparser.EntityParser
//import com.zoho.zia.api.textservice.response.v1.ErrorResponse
//import com.zoho.zia.crm.api.ErrorCodes
//import org.apache.commons.lang3.StringEscapeUtils
//import play.api.libs.functional.syntax._
//import play.api.libs.json.Reads._
//import play.api.libs.json.{JsPath, Json, _}
//
//import scala.collection.mutable.ListBuffer
//import scala.util.control.Breaks._
//
//trait formats {
//  case class NameEntities(
//      actualName: Option[String],
//      firstName: Option[String],
//      lastName: Option[String],
//      middleName: Option[String])
//    case class AddressEntities(
//      postcode: Option[String],
//      city: Option[String],
//      country: Option[String],
//      state: Option[String])
//    case class SocialEntities(
//      gplus: Option[String],
//      skype: Option[String],
//      facebook: Option[String],
//      twitter: Option[String])
//    case class ParsedEntities(
//      name: Option[NameEntities],
//      company: Option[String],
//      designation: Option[String],
//      primaryphoneno: Option[String],
//      secondaryphoneno : Option[String],
//      fax: Option[String],
//      socialLinks: Option[SocialEntities],
//      address: Option[Array[AddressEntities]])
//
//      case class ParsedEntities_(
//    name: Option[NameEntities],
//    company: Option[String],
//    designation: Option[String],
//    phoneno: Option[Array[String]],
//    fax: Option[String],
//    socialLinks: Option[SocialEntities],
//    address: Option[Array[AddressEntities]])
//
//
//
//    case class Extracted(
//      status : Int,
//      person : Option[ParsedEntities]
//    )
//
//
//    implicit val nameReads: Reads[NameEntities] = (
//        (JsPath \ "actualName").readNullable[String] and
//        (JsPath \ "firstName").readNullable[String] and
//        (JsPath \ "lastName").readNullable[String] and
//        (JsPath \ "middleName").readNullable[String])(NameEntities.apply _)
//
//      implicit val addressReads: Reads[AddressEntities] = (
//        (JsPath \ "postcode").readNullable[String] and
//        (JsPath \ "city").readNullable[String] and
//        (JsPath \ "country").readNullable[String] and
//        (JsPath \ "state").readNullable[String])(AddressEntities.apply _)
//
//      implicit val socialReads: Reads[SocialEntities] = (
//        (JsPath \ "gplus").readNullable[String] and
//        (JsPath \ "skype").readNullable[String] and
//        (JsPath \ "facebook").readNullable[String] and
//        (JsPath \ "twitter").readNullable[String])(SocialEntities.apply _)
//
//      implicit val parsedEntities: Reads[ParsedEntities] = (
//        (JsPath \ "name").readNullable[NameEntities] and
//        (JsPath \ "company").readNullable[String] and
//        (JsPath \ "designation").readNullable[String] and
//        (JsPath \ "primaryphoneno").readNullable[String] and
//        (JsPath \ "secondaryphoneno").readNullable[String] and
//        (JsPath \ "fax").readNullable[String] and
//        (JsPath \ "socialLinks").readNullable[SocialEntities] and
//        (JsPath \ "address").readNullable[Array[AddressEntities]])(ParsedEntities.apply _)
//
//        implicit val parsedEntities1: Reads[ParsedEntities_] = (
//      (JsPath \ "name").readNullable[NameEntities] and
//      (JsPath \ "company").readNullable[String] and
//      (JsPath \ "designation").readNullable[String] and
//      (JsPath \ "phoneno").readNullable[Array[String]] and
//      (JsPath \ "fax").readNullable[String] and
//      (JsPath \ "socialLinks").readNullable[SocialEntities] and
//      (JsPath \ "address").readNullable[Array[AddressEntities]])(ParsedEntities_.apply _)
//
//      implicit val extracted: Reads[Extracted] = (
//          (JsPath \ "status").read[Int] and
//          (JsPath \ "person").readNullable[ParsedEntities]
//      )(Extracted.apply _)
//}
//
//
//class Score extends CrmActionSupport with formats{
//  val logger = Logger.getLogger(this.getClass.getName)
//  override def execute(): String = {
//    val score = scoring()
//    responseWriter(200, score.toString)
//    null
//  }
//
//  def responseWriter(status: Int, responeBody: String){
//    response.setStatus(status)
//    response.setContentType("application/json")
//    val writer = response.getWriter
//    writer.write(responeBody)
//    writer.close()
//  }
//  def scoring() : Double = {
//    val csvFile = "expectedData.csv"
//
//    var line = ""
//    val cvsSplitBy = "\t"
//    val content = ListBuffer[String]()
//
//
//    try {
//
//      val br = new BufferedReader(new FileReader(csvFile))
//      val list = new ListBuffer[Array[String]]
//      breakable {
//        while ((line = br.readLine()) != null) {
//
//          if (line != null) {
//
//            val values = line.split(cvsSplitBy)
//            list.+=(values.map(StringEscapeUtils.unescapeJson))
//
//          } else {
//            break
//          }
//
//        }
//      }
//
//
//
//      var sum = 0.0
//      list.foreach(f = data => {
//        val body = data(0).replaceAll("\\n", "\n")
//        logger.info("bodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybody")
//        logger.info(body)
//        logger.info("bodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybodybody")
//        val emailId = data(1)
//        logger.info("emailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemail")
//        logger.info(emailId)
//        logger.info("emailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemailemail")
//        logger.info("expjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjson")
//        logger.info(data(2))
//        logger.info("expjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjsonexpjson")
//        val expectedData = Json.parse(data(2))
//
//          val extractedData =
//            try{
//              EntityParser.extractFromEmail(body, emailId)
//            }catch{
//              case e: Exception =>
//                 logger.log(Level.SEVERE, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++>>>>>>>>>>>>>>>")
//                logger.log(Level.INFO, e.getMessage)
//                logger.log(Level.INFO, e.getStackTrace.mkString("\n"))
//              Left(new ErrorResponse(ErrorCodes.BAD_REQUEST))
//            }
//        logger.info("*****************************************************************")
//        logger.info("\n\n\n\n\n\n\n\n\n"+expectedData + " Expected\n\n\n\n\n\n\n\n\n")
//
//        logger.info("********************Expected*************************************")
//          logger.info("\n\n\n\n\n\n\n\n\n"+Json.toJson(extractedData) + "Extracted\n\n\n\n\n\n\n\n\n")
//        logger.info("*********************Extracted*************************************")
//        (expectedData.validate[ParsedEntities_], extractedData) match {
//          case (s: JsSuccess[ParsedEntities_], Right(x)) =>
//            val expected: ParsedEntities_ = s.get
//            val extracted = x
//
//            var total = 0
//            var correct = 0
//            if (expected.name.isDefined) {
//
//              if (extracted.name.isDefined) {
//
//                if (expected.name.get.actualName.isDefined) {
//                  total += 1
//                  if (extracted.name.get.actualName.isDefined &&
//                    FuzzySearch.ratio(expected.name.get.actualName.get.toLowerCase(), extracted.name.get.actualName.get.toLowerCase()) >= 50)
//                    correct += 1
//                }
//
//                if (expected.name.get.firstName.isDefined) {
//                  total += 1
//                  if (extracted.name.get.firstName.isDefined &&
//                    FuzzySearch.ratio(expected.name.get.firstName.get.toLowerCase(), extracted.name.get.firstName.get.toLowerCase()) >= 50)
//                    correct += 1
//                }
//
//                if (expected.name.get.lastName.isDefined) {
//                  total += 1
//                  if (extracted.name.get.lastName.isDefined &&
//                    FuzzySearch.ratio(expected.name.get.lastName.get.toLowerCase(), extracted.name.get.lastName.get.toLowerCase()) >= 50)
//                    correct += 1
//                }
//
//                if (expected.name.get.middleName.isDefined) {
//                  total += 1
//                  if (extracted.name.get.middleName.isDefined &&
//                    FuzzySearch.ratio(expected.name.get.middleName.get.toLowerCase(), extracted.name.get.middleName.get.toLowerCase()) >= 50)
//                    correct += 1
//                }
//
//              }
//            }
//
//            if (expected.company.isDefined) {
//              total += 1
//              if (extracted.company.isDefined &&
//                (FuzzySearch.ratio(expected.company.get.toLowerCase(), extracted.company.get.toLowerCase()) >= 20 || expected.company.get.toLowerCase().contains(extracted.company.get.toLowerCase())))
//                correct += 1
//            }
//
//            if (expected.address.isDefined) {
//              val expectedAddress = expected.address.get(0)
//              if (extracted.address.nonEmpty) {
//                val extractedAddress = extracted.address.head
//
//                if (expectedAddress.city.isDefined) {
//                  total += 1
//                  if (extractedAddress.city.isDefined && extractedAddress.city.get.replaceAll("[^A-za-z0-9\n\\s]$", "").toLowerCase().equals(expectedAddress.city.get.toLowerCase()))
//                    correct += 1
//                }
//
//                if (expectedAddress.state.isDefined) {
//                  total += 1
//                  if (extractedAddress.state.isDefined && extractedAddress.state.get.replaceAll("[^A-za-z0-9\n\\s]$", "").toLowerCase().equals(expectedAddress.state.get.toLowerCase()))
//                    correct += 1
//                }
//
//                if (expectedAddress.country.isDefined) {
//                  total += 1
//                  if (extractedAddress.country.isDefined && extractedAddress.country.get.replaceAll("[^A-za-z0-9\n\\s]$", "").toLowerCase().equals(expectedAddress.country.get.toLowerCase()))
//                    correct += 1
//                }
//
//                if (expectedAddress.postcode.isDefined) {
//                  total += 1
//                  if (extractedAddress.postcode.isDefined && extractedAddress.postcode.get.replaceAll("[^A-za-z0-9\n\\s]$", "").toLowerCase().equals(expectedAddress.postcode.get.toLowerCase()))
//                    correct += 1
//                }
//              }
//            }
//
//            //            val socialLinks = extracted.social
//            //            if(expected.socialLinks != None){
//            //              if(expected.socialLinks.get.facebook != None){
//            //
//            //              }
//            //            }
//
//            if (expected.designation.isDefined) {
//              total += 1
//              if (extracted.designation.isDefined && FuzzySearch.ratio(expected.designation.get.replaceAll("[^A-za-z0-9\n\\s]$", "").toLowerCase(), extracted.designation.get.toLowerCase()) >= 60)
//                correct += 1
//            }
//
//            if (expected.fax.isDefined) {
//              total += 1
//              if (extracted.fax.isDefined && expected.fax.get.toLowerCase().equals(extracted.fax.get.toLowerCase()))
//                correct += 1
//            }
//
//            if (expected.phoneno.isDefined) {
//              total += 1
//              if (extracted.phoneNo.nonEmpty && expected.phoneno.get.foldLeft(false)((x, c) => x || extracted.phoneNo.contains(c)))
//                correct += 1
//            }
//            logger.info(correct + " Correct")
//            logger.info(total + " Total")
//            sum += correct / total.toDouble
//            logger.info((correct / total.toDouble).toString())
//
//          case (_: JsError, Left(z)) =>
//            // error handling flow
//            logger.info("Errors: " + z)
//
//
//          case (JsError(_), Right(_)) =>
//            print()
//
//          case (JsSuccess(_, _), Left(_)) => print()
//        }
//
//      })
//
//      logger.info(sum + " " + list.length)
//
//      logger.info("Over All " + (sum / list.length.toDouble))
//
//      val result = sum / list.length.toDouble
//      logger.info(result.toString())
//      result
//    } catch {
//      case e: IllegalArgumentException => e.printStackTrace()
//        1.0
//    }
//  }
////  // Final Score 0.9375116713352005
////
//}