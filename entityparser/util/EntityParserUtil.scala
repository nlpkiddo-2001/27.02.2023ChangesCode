package com.zoho.crm.feature.entityparser.util

import java.util.regex.{Matcher, Pattern}

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.zoho.crm.feature.entityparser.outputs.Span
import com.zoho.crm.ml.nlp.constants.SPECIALCHAR
import com.zoho.zia.utils.GlobalActor
import play.api.libs.json.{JsArray, JsObject, JsValue}

import scala.collection.mutable.ArrayBuffer
import scala.collection.{SeqView, mutable}
import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}
import scala.language.postfixOps
import scala.util.matching.Regex

object EntityParserUtil{

  def normalizeSpaceAndEmptyLines(text: String): String = {
    text.split("\n").map(_.trim).filter(_.nonEmpty).map(_.replaceAll("\\s+", " ")).mkString("\n")
  }

  def findAllPatternsInString(pattern: Pattern, text: String) : List[String] = {
    val matcher: Matcher = pattern.matcher(text)
    val matchedSubString: ArrayBuffer[String] = ArrayBuffer()
    while(matcher.find())
      matchedSubString += text.slice(matcher.start(), matcher.end)
    matchedSubString.toList
  }

  def findAllPatternsInString(pattern: Pattern, text: String, useActor: Boolean): List[String]={
    val functionComputation = GlobalActor.globalActorSystem.actorOf(Props[FunctionComputation])
    implicit val timeout: Timeout = Timeout(20 milliseconds)
    val future = functionComputation ? (pattern, text)
    try{
      Await.result(future, timeout.duration).asInstanceOf[List[String]]
    }catch {
      case _: TimeoutException =>
        functionComputation.tell(PoisonPill.getInstance, ActorRef.noSender)
        List[String]()
    }
  }

  def extractBestMatch(text: String, patterns: Array[String]): Array[String] = {
    patterns.map(e => (text.toLowerCase().indexOf(e.toLowerCase()), e)).sorted.filterNot(_._1 == -1).map(_._2)
  }

  //Regex Pattern Matching
  def findAllPattern(data: String, pattern: Regex): List[String] = {
    pattern.findAllIn(data).toList
    //    pattern.findAllIn(data).toSet.filter(_!= "").toList
  }

  def getPatternFromEmail(email: String, entity: String): String = entity match {
    case "name" => email.slice(0, email.indexOf(SPECIALCHAR.ATTHERATE)).replaceAll("[^A-Za-z]", "")
    case "company" =>

      val company = email.slice(email.indexOf("@") + 1, email.lastIndexOf("."))
      if (company.contains(".")) {
        val company = email.slice(email.indexOf("@"), email.lastIndexOf(".")).split("\\.")
        val companyName = company.slice(0, company.length - 1).mkString("").replaceAll("[^A-Za-z0-9]", "")
        companyName
      } else {
        company
      }
  }

  def longestCommonSubstring(a: String, b: String): String = {
    @scala.annotation.tailrec
    def loop(m: Map[(Int, Int), Int], bestIndices: List[Int], i: Int, j: Int): String = {
      if (i > a.length) {
        b.substring(bestIndices(1) - m((bestIndices.head, bestIndices(1))), bestIndices(1))
      } else if (i == 0 || j == 0) {
        loop(m + ((i, j) -> 0), bestIndices, if (j == b.length) i + 1 else i, if (j == b.length) 0 else j + 1)
      } else if (a(i - 1) == b(j - 1) && math.max(m((bestIndices.head, bestIndices(1))), m((i - 1, j - 1)) + 1) == (m((i - 1, j - 1)) + 1)) {
        loop(
          m + ((i, j) -> (m((i - 1, j - 1)) + 1)),
          List(i, j),
          if (j == b.length) i + 1 else i,
          if (j == b.length) 0 else j + 1)
      } else {
        loop(m + ((i, j) -> 0), bestIndices, if (j == b.length) i + 1 else i, if (j == b.length) 0 else j + 1)
      }
    }
    loop(Map[(Int, Int), Int](), List(0, 0), 0, 0)
  }

  def stringRemoval(stringsToRemove: Array[Array[String]], baseStr: String): String = {
    stringsToRemove.foldLeft(baseStr.toLowerCase())((str, removal) => {

      removal.foldLeft(str.trim())((str, removeWord) => {
        str.split("\n").map(line=> {
          line.split(" ").map(_.trim).map(word => {
            if (word.contains(removeWord) &&
              word.indexOf(removeWord) == 0 &&
              removeWord.length <= word.length()) {
              word.replace(removeWord, SPECIALCHAR.EMPTYSTRING)
            } else {
              word
            }
          }).mkString(" ").replace(removeWord, SPECIALCHAR.EMPTYSTRING)
        }).mkString("\n")
      })
    })
  }

  def findDanglingCharacters(text: String): Array[(Char, Int)] = {
    val parenthesis: Map[Char, Char] = Map('}' -> '{', ')' -> '(', ']' -> '[', '>' -> '<', '''-> ''', '"' -> '"', '`' -> '`')

    val isParenthesis: Char => Boolean = c => parenthesis exists { case (closed, opened) => closed == c || opened == c }
    val isClosing: Char => Boolean = parenthesis.contains
    val openingFor: Char => Option[Char] = parenthesis.get
    text.toCharArray.zipWithIndex
      .filter(x=>isParenthesis(x._1))
      .foldLeft(mutable.Stack[(Char, Int)]())((stack, p) => {
        if (isClosing(p._1) && stack.nonEmpty){
          val isPresent = stack.map(_._1).headOption == openingFor(p._1)
          if(isPresent) stack.pop else stack.push(p)
          stack
        }
        else{
          stack.push(p)
          stack
        }
      }).toArray
  }

  def stringNormalization(text: String): String = {
    val stripLeadAndTrailingSpecialChar = "(^[^\\w\\d]+)|([^\\w\\d]+$)"
    val stripSpecialChar = "([^\\w\\d\\(\\)\\{\\}\\[\\]'\"><\\s`]+)"
    var cleanedText: String = Asciifier.apply(text)
    cleanedText = cleanedText.replaceAll(stripLeadAndTrailingSpecialChar, "")
    val view : SeqView[Char, Seq[_]] = (cleanedText: Seq[Char]).view
    val danglingChars = findDanglingCharacters(cleanedText)
    cleanedText = danglingChars.map(_._2).toList.foldLeft(view)((str, i)=> str.updated(i, ' ')).mkString
    cleanedText = cleanedText.replaceAll(stripSpecialChar, " ")
    cleanedText
  }

  def getSpan(sourceText: String, extractedEntity: String): Option[Span] = {
    val startIndex = sourceText.indexOf(extractedEntity)
    val endIndex = startIndex + extractedEntity.length
    if(startIndex != -1){
      Some(Span(extractedEntity, startIndex, endIndex))
    }else{
      None
    }
  }


  def getAllKeysFromPlayJson(json: JsValue): collection.Set[String] = json match {
    case o: JsObject => o.keys
    case JsArray(as) => as.flatMap(getAllKeysFromPlayJson).toSet
    case _ => Set()
  }

}


class FunctionComputation extends Actor{
  override def receive: Receive = {
    case params: (_, _) => sender() ! EntityParserUtil.findAllPatternsInString(params._1.asInstanceOf[Pattern], params._2.asInstanceOf[String])
  }
}