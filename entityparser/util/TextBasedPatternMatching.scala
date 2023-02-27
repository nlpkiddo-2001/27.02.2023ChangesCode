//scalastyle:off
package com.zoho.crm.feature.entityparser.util

import scala.util.control.Breaks._

class TextBasedPatternMatching(textData: String, patternToSearch: String, charToConsider: String){

  val patternRegex: String = getTextPattern(patternToSearch)
  def extract: String = {
    val possiblePatternMatch = textData.toLowerCase().split("\n").reverse.filter(_ contains patternToSearch)
    //assuming pattern occur only once and probability is high at last part
    if(possiblePatternMatch.length !=0){
     
        findAndExtractPattern(possiblePatternMatch(0).trim())
      
         
    }else{
      ""
    }
  }
  
  def findAndExtractPattern(possiblePatternMatch: String): String = {

      val delimiterFrequencyMap = possiblePatternMatch.filter(e => (e + "").matches(charToConsider)).groupBy(_.toChar).mapValues(_.length)
      val delimiterChars = {
        val tmpMap = delimiterFrequencyMap.filter(e => e._2 >= 2)
        if (tmpMap.size >= 2) tmpMap else delimiterFrequencyMap
      }

      val minCount = delimiterChars.minBy(_._2)._2
      val secondMin = {
        val tmpMap = delimiterChars.filter(e => e._2 != minCount)
        if (tmpMap.size > 1) tmpMap.minBy(_._2)._2 else delimiterChars.minBy(_._2)._2
      }

      val possibleDelimeter = delimiterChars.filter(_._2 == minCount).maxBy(_._1)._1
      val possibleSecondDelimeter = delimiterChars.filter(_._2 == secondMin).maxBy(_._1)._1

      val (beginingChar, endingChar) = possibleRepeatedChar(possiblePatternMatch, patternToSearch,
        possibleDelimeter, possibleSecondDelimeter)
      val senArr =
        if (possibleDelimeter == beginingChar) {
          possiblePatternMatch.split(beginingChar)

        } else if (endingChar != possibleDelimeter) {
          possiblePatternMatch.split(endingChar)
        } else Array[String]()
      var a = 0

      var patternMatchIndex = -1
      breakable {
        senArr.zipWithIndex foreach (e => {
          if (patternRegex.r.findFirstIn(e._1).isDefined) {
            patternMatchIndex = e._2
            break
          }
        })
      }
      if (patternMatchIndex != -1) {
        var userName = ""
        breakable {
          if (senArr.count(_.length() > 1) == 2) {
            userName = senArr(patternMatchIndex + 1)
          } else
            userName = senArr(patternMatchIndex)

          if (userName.trim.filter(e => (e + "").matches("[^A-Za-z0-9]")).length == 0) {
            //              println("i")
            if (patternMatchIndex < senArr.length - 1) {
              userName = senArr(patternMatchIndex + 1)
            }
          }

          if (userName.length() <= 5) {
            if (patternMatchIndex < senArr.length) {
              userName = senArr(patternMatchIndex + 1)
            } else {
              userName = ""
            }
          }
        }
        
        userName
      } else {
        ""
      }

    }
  //Twitter : zohoCorp | Facebook : zoho | Instagram : Zoho 
  //returns : or |
  //Twitter | zohoCorp | Facebook | zoho | Instagram | Zoho 
  //returns |
  private def possibleRepeatedChar(possiblePatternMatch: String, patternToSearch: String,
    possibleDelimeter: Char, possibleSecondDelimeter: Char): (Char, Char) = {
    val patternIndex = possiblePatternMatch.indexOf(patternToSearch)
    def findRepeatedChar(operation: (Int, Int) => Int,positions : Range,charType : Char): (Char, Boolean) = {
      var inverse = false
      var charAtFirst: Char = '!'
      breakable {
        for (j <- positions) {
          val charAtPos = possiblePatternMatch.charAt(operation(patternIndex, j))
          //              println(possiblePatternMatch.charAt(patternIndex))
          if ((charAtPos == possibleDelimeter) || (charAtPos == possibleSecondDelimeter)) {
            charType match {
              case 'b' => if (charAtPos == possibleSecondDelimeter)
                            inverse = true
              case 'e' => if (charAtPos == possibleDelimeter)
                            inverse = true
              case _ =>
            }
            charAtFirst = charAtPos
            break
          }
        }
      }
      (charAtFirst, inverse)
    }
    val add = (a: Int, b: Int) => a + b
    val sub = (a: Int, b: Int) => a - b
    val (beginChar, inverse1) = if (possiblePatternMatch.indexOf(patternToSearch) != 0) findRepeatedChar(sub,0 to 3,'b' ) else (possibleDelimeter, false)
    val (endChar, inverse2) = if (possiblePatternMatch.indexOf(patternToSearch) != possiblePatternMatch.length() - 1) findRepeatedChar(add,0 until possiblePatternMatch.length()-patternIndex,'e') else ('!', false)
    val inverse = inverse1 | inverse2
    if (inverse) (endChar, beginChar) else (beginChar, endChar)
  }

  def getTextPattern(basePtrn: String): String = "(?i)" + basePtrn + "(?-i).*?"
}