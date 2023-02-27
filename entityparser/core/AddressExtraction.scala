package com.zoho.crm.feature.entityparser.core

import java.util.logging.Level

import com.zoho.crm.feature.entityparser.outputs.{Address, AddressEntities}
import com.zoho.crm.feature.entityparser.util.{AddressPreProcessing, ApproximateStringMatching, EntityParserUtil}
import com.zoho.crm.feature.entityparser.{EntityExtraction, ExtractedEntity, ParsedEntity}
import com.zoho.crm.ml.Libpostal.LibPostalModel
import com.zoho.crm.ml.nlp.constants.{ADDRESS, NLP, SPECIALCHAR}
import com.zoho.crm.ml.utils.CommonUtil

import scala.collection.JavaConverters._
import scala.collection.mutable

class AddressExtraction(entityEssentials: ParsedEntity) extends EntityExtraction {

  val modelTypes: Array[String] = Array(ADDRESS.COUNTRY, ADDRESS.CITY, ADDRESS.STATE, ADDRESS.STATECODE)
  val addressFields: Array[String] = Array(ADDRESS.CITY, ADDRESS.COUNTRY, ADDRESS.POSTCODE, ADDRESS.STATE)

  def extract: ExtractedEntity = { //scalastyle:ignore
    try{
      val addressData = preProcess
      val addressExtraction = process(addressData)
      postProcess(addressExtraction)
    }catch{
      case e:Throwable => 
        logger.info("Address Extraction Failed "+e.toString)
        logger.log(Level.SEVERE, e.getStackTrace.mkString("\n"))
        Address(Array())

      case e:Exception =>
        logger.info("Address Extraction Failed "+e.toString)
        logger.log(Level.SEVERE, e.getStackTrace.mkString("\n"))
        Address(Array())

    }

  }

  def preProcess: String = {
    val removeUnWantedString = new AddressPreProcessing(entityEssentials)
    val cleanedText = removeUnWantedString.extract.replaceAll("""[^a-zA-z0-9#.,;:'°/^&()\n ]""", SPECIALCHAR.SPACE)
    cleanedText
  }

  def process(addressData: String): List[Map[String, String]] = { //scalastyle:ignore

    def addressExtraction(addressEntities: List[Map[String, String]], parsedEntites: Map[String, List[String]]): List[Map[String, String]] = {
        if (addressEntities.isEmpty) {

        parsedEntites match {
          case x if (x.contains(ADDRESS.COUNTRY) &&
            (x.contains(ADDRESS.STATE) || x.contains(ADDRESS.STATECODE))) ||
            (x.contains(ADDRESS.COUNTRY) && x.contains(ADDRESS.CITY)) ||
            (x.contains(ADDRESS.COUNTRY) && x.contains(ADDRESS.CITY) &&
              (x.contains(ADDRESS.STATE) || x.contains(ADDRESS.STATECODE))) ||
              (x.contains(ADDRESS.STATE) && x.contains(ADDRESS.CITY)) =>
            val parsedAddress = LibPostalModel.parseAddress(addressData, addressFields)
            if (parsedAddress.contains(ADDRESS.COUNTRY) ||
              parsedAddress.contains(ADDRESS.STATE) ||
              parsedAddress.contains(ADDRESS.CITY)) {
              List(parsedAddress)
            } else {
              addressEntities
            }
          case x if x.contains(ADDRESS.CITY) =>
            val parsedAddress = LibPostalModel.parseAddress(addressData, addressFields)
            if (parsedAddress.contains(ADDRESS.CITY)) {
              List(parsedAddress)
            } else {
              addressEntities
            }
          case x if x.contains(ADDRESS.STATE) =>
            val parsedAddress = LibPostalModel.parseAddress(addressData, addressFields)
            if (parsedAddress.contains(ADDRESS.STATE)) {
              List(parsedAddress)
            } else {
              addressEntities
            }
          case _ => addressEntities

        }

      } else {
        addressEntities
      }
    }

    val (found, finalResult) = onlyWithLibpostal(addressData)
    if(found){
      List(finalResult)
    }else{
      val possibleAddressAndEntities = possibleAddressLines(addressData.split("\n").filter(e => e != "" || e.length() > 2).mkString("\n"))
      val parsedEntites = possibleAddressAndEntities._2
      val addressEntities = getAddressEntities(possibleAddressAndEntities._1)
      val result = addressExtraction(addressEntities, parsedEntites)
      if(possibleAddressAndEntities._3){
        val parsedEntites = possibleAddressAndEntities._2
        val addressEntities = getAddressEntities(possibleAddressAndEntities._1)
        val result = addressExtraction(addressEntities, parsedEntites)
        val possibleAddressAndEntities_1 = possibleAddressLines(addressData.split("\n").filter(e => e != "" || e.length() > 2).mkString(""))
        val parsedEntites_1 = possibleAddressAndEntities_1._2
        val addressEntities_1 = getAddressEntities(possibleAddressAndEntities_1._1)
        //      logger.info(addressExtraction(addressEntities_1, parsedEntites_1).map(_.map(_.productIterator).mkString(":")).mkString("\n"))
        result
      }else{
        val possibleAddressAndEntities = possibleAddressLines(addressData.split("\n").filter(e => e != "" || e.length() > 2).mkString(""))
        val parsedEntites = possibleAddressAndEntities._2
        val addressEntities = getAddressEntities(possibleAddressAndEntities._1)
        addressExtraction(addressEntities, parsedEntites)
      }
    }
  }

  def postProcess(addressEntities: List[Map[String, String]]): Address = {

   val address = if(addressEntities.nonEmpty){
     addressEntities.map(
         e => e.map(e => e._1 -> e._2.replaceAll(
             SPECIALCHAR.NONAPLHANUMERIC,
             SPECIALCHAR.EMPTYSTRING)
             )
     ).filter(x=>x.nonEmpty) match{
       //Option(x.get.zipWithIndex.map(x => (x._1 + ("rank" -> (x._2 + 1).toString))).toList)
       case x if x.isEmpty => addressDetectionBasedONNER
       case x => Address(
           x.zipWithIndex.map(x =>
         AddressEntities(
             x._2 + 1,
             x._1.get(ADDRESS.POSTCODE),
             x._1.get(ADDRESS.CITY),
             x._1.get(ADDRESS.COUNTRY),
             x._1.get(ADDRESS.STATE)
         )

     ).toArray)
     }
   }else{
     addressDetectionBasedONNER
   }

    if(address.address.nonEmpty){
      Address(
        address.address.map(x=>{
          AddressEntities(
            x.rank,
            x.postcode,
            extractOriginalText(x.city),
            extractOriginalText(x.country),
            extractOriginalText(x.state)
          )
        }
      ))
    }else
      address

  }


  def extractOriginalText(pattern: Option[String]): Option[String] = {
    if(pattern.nonEmpty){
//      val originalName = new FuzzyBasedStringMatching(entityEssentials.signature, pattern.get).extract(extractFromOriginal = true)
      val originalName = ApproximateStringMatching.firstMatch(entityEssentials.textContainingEntities, pattern.get)
      val normalizedName = EntityParserUtil.stringNormalization(originalName)
      Some(normalizedName)
    }else
      pattern
  }
  private def onlyWithLibpostal(textData: String): (Boolean, Map[String, String]) = {
  //  val postcode = textData.split("[,\n]").map(x => LibPostalModel.parseAddress(x, Array("pincode"))).filter(y => !y.isEmpty)
    //val newTextData = textData.slice(0, textData.indexOf(postcode(0).head._2))
   // var parsedAddress = Map[String, String]()
   // val parsedAddress_old = textData.split("[,\n]").map(x => LibPostalModel.parseAddress(x, addressFields)).filter(y => !y.isEmpty)
   // val m = parsedAddress_old.map(x => parsedAddress = parsedAddress ++ x)
    val parsedAddress = LibPostalModel.parseAddress(textData, addressFields)
    val count = addressFields.foldLeft(0)((count, field)=> if(parsedAddress.contains(field))count+1 else count)
    if(count >= 2){
      (true, parsedAddress)
    }else{
      (false, parsedAddress)
    }
  }


  private def addressValidation(locEntities: mutable.Map[String, Array[String]]): mutable.Map[String, Array[String]] = {
    val filterList: (mutable.Map[String, Array[String]], String, String) => Array[String] =
      (map: mutable.Map[String, Array[String]], a: String, b: String) => map(a).filter(!map(b).contains(_))
    if (locEntities.contains(ADDRESS.CITY)) {
      if (locEntities.contains(ADDRESS.COUNTRY)) {
        locEntities(ADDRESS.CITY) = filterList(locEntities, ADDRESS.CITY, ADDRESS.COUNTRY)
      }

      if (locEntities.contains(ADDRESS.COUNTRYCODE)) {
        locEntities(ADDRESS.CITY) = filterList(locEntities, ADDRESS.CITY, ADDRESS.COUNTRYCODE)
      }

      if (locEntities.contains(ADDRESS.STATE)) {
        locEntities(ADDRESS.STATE) = filterList(locEntities, ADDRESS.STATE, ADDRESS.CITY)
      }
      if (locEntities.contains(ADDRESS.STATECODE)) {
        locEntities(ADDRESS.STATECODE) = filterList(locEntities, ADDRESS.STATECODE, ADDRESS.CITY)
      }
    }

    locEntities
  }
  private def addressDetectionBasedONNER: Address = {
    val cleanedSignature = preProcess
    var locEntities: mutable.Map[String, Array[String]] = CommonUtil.findEntities(cleanedSignature, modelTypes, NLP.TOKENS).asScala.map(x => (x._1, x._2.asScala.toArray))
    locEntities = mutable.Map(locEntities.map(x=>(x._1, x._2.filterNot(_.exists(_.isDigit)))).toSeq: _*)
    val possibleFormats = Array(
      Array(ADDRESS.CITY, ADDRESS.STATE, ADDRESS.COUNTRY),
      Array(ADDRESS.CITY, ADDRESS.STATE),
      Array(ADDRESS.CITY, ADDRESS.COUNTRY),
      Array(ADDRESS.STATE, ADDRESS.COUNTRY)
    )
    var found = false
    var selectedFormat: Array[String] = Array()

      for(format <- possibleFormats.zipWithIndex){
        var entityFound = 0
        if(!found) {
          for (entity <- format._1) {
            entity match {
              case ADDRESS.CITY =>
                if (locEntities.contains(ADDRESS.CITY) && locEntities.get(ADDRESS.CITY).size == 1) {
                  entityFound += 1
                }

              case ADDRESS.STATE =>
                if (locEntities.contains(ADDRESS.STATE)
                  && locEntities.get(ADDRESS.STATE).size == 1) {
                  entityFound += 1
                }

              case ADDRESS.COUNTRY =>
                if (locEntities.contains(ADDRESS.COUNTRY)
                  && locEntities.get(ADDRESS.COUNTRY).size == 1) {
                  entityFound += 1
                }
            }
          }
          if (entityFound == format._1.length) {
            found = true
            locEntities = addressValidation(locEntities)
            selectedFormat = format._1
          }
        }
    }

    if(found) {
      Address(
          Array(
            AddressEntities(
              1,
              None,
              if(selectedFormat.contains(ADDRESS.CITY))  locEntities(ADDRESS.CITY).headOption else None,
              if(selectedFormat.contains(ADDRESS.COUNTRY)){
                if(locEntities.contains(ADDRESS.COUNTRY)){
                  locEntities(ADDRESS.COUNTRY).headOption
                }else{
                  locEntities(ADDRESS.COUNTRYCODE).headOption
                }
              }else{
                None
              },
              if(selectedFormat.contains(ADDRESS.STATE)){
                if(locEntities.contains(ADDRESS.STATE)){
                  locEntities(ADDRESS.STATE).headOption
                }else{
                  locEntities(ADDRESS.STATECODE).headOption
                }
              }else{
                None
              }
            )
          )
      )

    }else{
      Address(Array())
    }

  }

  private def getAddressEntities(possibleAddress: List[String]): List[Map[String, String]] = {
    @scala.annotation.tailrec
    def addressEntities(addList: List[String], addFields: List[Map[String, String]]): List[Map[String, String]] = addList match {
      case Nil => addFields
      case x :: tail =>
        val parsedAddress = LibPostalModel.parseAddress(x, addressFields)
        if (parsedAddress.keySet.size > 3) addressEntities(tail, parsedAddress :: addFields) else addressEntities(tail, addFields)
    }
    addressEntities(possibleAddress, List())
  }

  private def possibleAddressLines(addressData: String): (List[String], Map[String, List[String]], Boolean) = {
    val postalAddress: Array[String] = addressData.split(SPECIALCHAR.LINE)
    val possibleAddress: mutable.ListBuffer[String] = mutable.ListBuffer[String]()
    var foundExactAddressLine = false
    val addressEntities = new mutable.HashMap[String, List[String]]
    postalAddress.filter(_.length() <= 300).foreach(address => {
      val parsedAddress: Map[String, List[String]] = CommonUtil.findEntities(address, modelTypes, NLP.TOKENS).asScala.toMap.map(x=>x._1 -> x._2.asScala.toList)
      if (parsedAddress.nonEmpty) {
        if(parsedAddress.size >= 3){
          foundExactAddressLine = true
        }
        if (isAddress(parsedAddress)) {
          possibleAddress.+=(address.trim)
        }
      }
      parsedAddress foreach (e => {
        if (addressEntities.contains(e._1)) {
          addressEntities.update(e._1, (addressEntities(e._1) ++ e._2).distinct)
        } else {
          addressEntities.put(e._1, e._2)
        }

      })
    })
    (possibleAddress.toList, addressEntities.toMap, foundExactAddressLine)

  }

  private def isAddress(parsedAddress: Map[String, List[String]]): Boolean = {
    val dataCount: Int = parsedAddress.count(e =>
      e._1.equals(ADDRESS.CITY) && e._2.length <= 3 ||
        e._1.equals(ADDRESS.COUNTRY) && e._2.length <= 1 ||
        e._1.equals(ADDRESS.STATE) && e._2.length <= 2 ||
        e._1.equals(ADDRESS.STATECODE) && e._2.length <= 2 ||
        e._2.length <= 1)
    dataCount >= 2
  }
}