package com.zoho.crm.feature.entityparser.util.patterns

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.zoho.crm.ml.nlp.constants.{ADDRESS, SPECIALCHAR}
import org.apache.commons.lang3.StringUtils
import scala.collection.JavaConverters._

import scala.collection.mutable

object PhoneNoExtraction {
  def getPhoneNo(textData: String): Array[String] = {
    beginPhoneExtractionByCountry(textData)
  }

  private def beginPhoneExtractionByCountry(textData: String): Array[String] = {
    val phonePattern = getPhones(textData)
    val phonePatternMap = phonePattern.map(x=> x._1 -> x._2).toMap
    val phoneNoList = phonePatternMap.keys.toSet.toArray
    val possiblePhonenos = phoneNoList.toSet.toList.foldLeft(phoneNoList)((phoneList, phoneNo) =>
      phoneList.filterNot(x =>
        phoneNo.length() > x.length() && !x.equals(phoneNo) &&
          (if (x.length < phoneNo.length) {
            phoneNo.contains(x)
          } else {
            x.contains(phoneNo)
          }
          ))).filter(e => e.length() >= 7 &&
      (0 until 9).toList.foldLeft(0)((sum, number) => sum + StringUtils.countMatches(e, number.toString)) >= 7)

    if(possiblePhonenos.nonEmpty){
      possiblePhonenos
    }else{
      phoneExtractionBasedOnCountryCode(
        textData,
        phonePattern,
        phoneNoList
      )
    }

  }

  def phoneExtractionBasedOnCountryCode(textData: String, phonePattern: Array[(String, String)], phoneNos: Array[String]): Array[String] ={
    val phonetoCountryMap = phonePattern.foldLeft(mutable.Map[String, Array[String]]())((map, phoneP)=>{
      if(map.contains(phoneP._2)){
        val countryCodes = collection.mutable.ArrayBuffer(map(phoneP._2): _*)
        countryCodes += phoneP._2
        map.update(phoneP._2, countryCodes.toArray)
        map
      }else{
        map.put(phoneP._1, Array(phoneP._2))
        map
      }
    }).toMap
//    val phonetoCountryMap = phonePattern.toMap
    val possiblePhoneNo = mutable.ArrayBuffer[String]()
    textData.split("\n").foreach(sentence=> {
      for(phoneNO <- phoneNos){
        if(phoneNO.length>=7 && sentence.contains(phoneNO)){
          val countryCodes = phonetoCountryMap(phoneNO)
          countryCodes.foreach(countryCode => {
            if(sentence.contains(countryCode)){
              possiblePhoneNo += phoneNO
            }
          })

        }
      }
    })
    if(possiblePhoneNo.length == 0){
      for(phoneNO <- phoneNos){
        if(phoneNO.length>=7){
          possiblePhoneNo += phoneNO
        }
      }
    }
    possiblePhoneNo.toArray.toSet.toArray
  }
  private def beginPhoneExtraction(textData: String): Array[String] = {
    val phonePattern = getPhones(textData)
    val phonePatternMap = phonePattern.map(x=> (x._1 -> x._2)).toMap
    val phoneNo = matchValidCase(phonePattern.map(_._1), textData)
    phoneNo.map(phonePatternMap(_))
  }

  private def matchValidCase(phonePattern: Array[String], textData: String): Array[String] = {
    phonePattern.filter(
      textData.replaceAll(SPECIALCHAR.ALPHANUMERIC, SPECIALCHAR.EMPTYSTRING) contains _.stripPrefix("+")
    )
  }

  def getPhones(textData: String): Array[(String, String)] = {
    ADDRESS.COUNTRYCODES.flatMap(countryCode => {
      getPhones(textData, countryCode)
    })
  }

  def getPhones(textData: String, countryCode: String): Array[(String, String)] = {
    val phoneUtil = PhoneNumberUtil.getInstance()
    phoneUtil.findNumbers(textData, countryCode).asScala.toArray.
      filter(phoneNo =>
        phoneUtil.isValidNumberForRegion(phoneNo.number(), countryCode)).map(phoneNo => {
        val phoneNumber = phoneNo.rawString()
        val phoneNumberWithCountryCode = phoneUtil.parse(phoneUtil.format(phoneNo.number(), PhoneNumberFormat.E164), "")
      (phoneNumber, phoneNumberWithCountryCode.getCountryCode.toString)
      })
  }

}
