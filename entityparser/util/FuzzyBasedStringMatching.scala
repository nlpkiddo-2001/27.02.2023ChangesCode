package com.zoho.crm.feature.entityparser.util

import com.zoho.crm.feature.entityparser.util.fuzzysearch.FuzzySearch
import com.zoho.crm.ml.nlp.common.Tokenizer
import com.zoho.crm.ml.nlp.constants.{NLP, SPECIALCHAR}

import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}


class FuzzyBasedStringMatching(text : String, patternToExtract: String) {
  val SCORE = "score" //scalastyle:ignore
  val START = "start" //scalastyle:ignore
  val END = "end" //scalastyle:ignore
  val RIGHT = "right" //scalastyle:ignore
  val LEFT = "left" //scalastyle:ignore
  val PENALTY = "penalty" //scalastyle:ignore
  val IDX = "idx" //scalastyle:ignore
  val CONSIDER = "consider" //scalastyle:ignore
  val TOKEN = "token" //scalastyle:ignore
  val LINE = "line" //scalastyle:ignore
  val ONE = "1"//scalastyle:ignore
  case class BestMatch(score: Double, start: Int, end: Int, line: Int)

  private def sequentialPermutation(ipStr: String): Array[String] = {
    (0 to ipStr.length).map(start =>
      (start to ipStr.length).map(end =>
        ipStr.slice(start, end))).toArray.flatten.filter(_ != SPECIALCHAR.EMPTYSTRING)
  }

  def extract(extractFromOriginal: Boolean = false): (String, Double) = { //scalastyle:ignore
    val maxEditDistance = 2
    val lowerCasedTokenizedList: Array[Array[String]] = text.toLowerCase()
                                              .split(SPECIALCHAR.LINE)
                                              .map(Tokenizer.getTokens(_, NLP.WHITESPACETOKENS).tokens)
    val tokenizedList: Array[Array[String]] = text.split(SPECIALCHAR.LINE)
      .map(Tokenizer.getTokens(_, NLP.WHITESPACETOKENS).tokens)
    val scoreBoardOfTokens: mutable.ArrayBuffer[mutable.ArrayBuffer[mutable.HashMap[String, Any]]] = //scalastyle:ignore
      scoreCalculator(lowerCasedTokenizedList, patternToExtract, maxEditDistance)
    var bestMatch = BestMatch(0, 0, 0, -1) //scalastyle:ignore
    var score = 0.0 //scalastyle:ignore
    for (i <- scoreBoardOfTokens.indices) {
      for (j <- scoreBoardOfTokens(i)) {
        if (j != null) { //scalastyle:ignore
          if (j(SCORE).toString.toDouble >= bestMatch.score) {
            j("line") = i
            bestMatch = BestMatch(j(SCORE).toString.toDouble, j(START).toString.toInt, j(END).toString.toInt, j("line").toString.toInt)
            score = j(SCORE).toString.toDouble
          }
        }
      }
    }
    if (bestMatch.line.equals(-1)) { //scalastyle:ignore
      (SPECIALCHAR.EMPTYSTRING, -0.0)
    } else {
      if(extractFromOriginal)
        (tokenizedList(bestMatch.line).slice(bestMatch.start, bestMatch.end + 1).mkString(" "), score)
      else
        (lowerCasedTokenizedList(bestMatch.line).slice(bestMatch.start, bestMatch.end + 1).mkString(" "), score)
    }
  }

  private def scoreCalculator(tokenizedList: Array[Array[String]],
                              patternToExtract: String, maxEditDistance: Int): mutable.ArrayBuffer[mutable.ArrayBuffer[mutable.HashMap[String, Any]]] = { //scalastyle:ignore
    val tokenizedLen = tokenizedList.map(_.length).max
    val scoreBoardOfTokens: mutable.ArrayBuffer[mutable.ArrayBuffer[mutable.HashMap[String, Any]]] = //scalastyle:ignore
      mutable.ArrayBuffer.tabulate(tokenizedList.length, tokenizedLen)((_, _) => null) //scalastyle:ignore
    for (lineNo <- tokenizedList.indices) {
      val tokenizedLine = tokenizedList(lineNo)
      for (tokenNo <- tokenizedLine.indices) {
        val token = tokenizedLine(tokenNo) //scalastyle:ignore
        val fuzzyRes = FuzzySearch.find_near_matches(token, patternToExtract, max_l_dist = Option(maxEditDistance))
        if (fuzzyRes.nonEmpty) {
          val tmpPatternToExtract = patternToExtract.slice(0, fuzzyRes.head.start.get) +
            Array.fill[String](fuzzyRes.head.end.get - fuzzyRes.head.start.get)(ONE).
            mkString(SPECIALCHAR.EMPTYSTRING) +
            patternToExtract.slice(fuzzyRes.head.end.get, patternToExtract.length())
          val remParArr = mutable.ArrayBuffer[String](tmpPatternToExtract.slice(0, tmpPatternToExtract.indexOf(ONE)),
            tmpPatternToExtract.slice(tmpPatternToExtract.lastIndexOf(ONE) + 1, tmpPatternToExtract.length()))
          //scalastyle:ignore

          val tmpSurroundings = mutable.ArrayBuffer[mutable.HashMap[String, Any]](null, null, null, null) //scalastyle:ignore

          surroundingWords(tokenizedLine, (tokenNo - 1 until -1 by -1) toArray, tmpSurroundings, LEFT)
          surroundingWords(tokenizedLine, (tokenNo + 1 until tokenizedLine.length) toArray, tmpSurroundings, RIGHT)

          val surroundingsTokens = tmpSurroundings.filter(_ != null) //scalastyle:ignore
          for (surrToken <- surroundingsTokens) {
            for (idx <- remParArr.indices) { //scalastyle:ignore
              val remParToken = remParArr(idx)
              if (!remParToken.contains(ONE) && !remParToken.trim().equals(SPECIALCHAR.EMPTYSTRING)) {
                if (remParToken.length() <= 2) {
                  val wrdPermutation = sequentialPermutation(remParToken)
                  breakable {
                    wrdPermutation.foreach(perm => {
                      if (surrToken(TOKEN).toString.startsWith(perm)) {
                        remParArr(idx) = remParToken.replaceFirst(perm, Array.fill[String](perm.length())(ONE).mkString(SPECIALCHAR.EMPTYSTRING))
                        surrToken.update(CONSIDER , 1)
                        break
                      }
                    })
                  }
                } else {
                  val secondaryEditMax = {
                    remParToken.length() match {
                      case x if x < 2 => 0
                      case x if x < 5 => 1
                      case _ => 2
                    }
                  }
                  val secondaryFuzzyResult =
                    FuzzySearch.find_near_matches(surrToken(TOKEN).toString, remParToken, max_l_dist = Option(secondaryEditMax))
                  if (secondaryFuzzyResult.nonEmpty) {
                    surrToken.update(CONSIDER, 1)
                    surrToken.update(PENALTY , secondaryFuzzyResult.head.dist.get.toFloat / 2.toFloat)
                    val tmpVar = new mutable.ArrayBuffer[String]()
                    tmpVar.append(remParToken.slice(0, secondaryFuzzyResult.head.start.get))
                    tmpVar.append(Array.fill[String](
                      secondaryFuzzyResult.head.end.get - secondaryFuzzyResult.head.start.get)(ONE).mkString(SPECIALCHAR.EMPTYSTRING))
                    tmpVar.append(remParToken.slice(secondaryFuzzyResult.head.end.get, remParToken.length()))

                    remParArr(idx) = tmpVar.toString()
                  }
                }
              }

            }
          }

          surroundingsTokens.append(mutable.HashMap(IDX-> tokenNo, CONSIDER -> 1, PENALTY -> (fuzzyRes.head.dist.get.toFloat / 2.0)))
          val surroundingTokensConsidered = surroundingsTokens.filter(_(CONSIDER).equals(1)).toArray //scalastyle:ignore
          val idxsToBeConsidered: Array[Int] = surroundingTokensConsidered.map(_(IDX).toString.toInt).sorted

          val penalty = surroundingTokensConsidered.map(_(PENALTY)).map(_.toString.toDouble).sum //scalastyle:ignore

          val extractedPatternBuff = new StringBuffer()
          extractedPatternBuff.append(remParArr(0)).append(tmpPatternToExtract.slice(extractedPatternBuff.indexOf(ONE),
            tmpPatternToExtract.lastIndexOf('1'))).append(remParArr(1))

          val extractedPattern = extractedPatternBuff.toString
          val score = (extractedPattern.count(_.equals('1')) - penalty) / extractedPattern.length() //scalastyle:ignore
          scoreBoardOfTokens(lineNo)(tokenNo) = mutable.HashMap(SCORE -> score, START -> idxsToBeConsidered(0),
            END -> idxsToBeConsidered(idxsToBeConsidered.length - 1))
        }
      }
    }
    scoreBoardOfTokens
  }

  private def surroundingWords(tokenizedLine: Array[String], wordWindow: Array[Int],
    tmpSurroundings: mutable.ArrayBuffer[mutable.HashMap[String, Any]], wordPos: String): Unit = { //scalastyle:ignore
    var count = 0 //scalastyle:ignore
    breakable {
      for (ix <- wordWindow) {
        val pattern = """^[_\W]+$""".r //scalastyle:ignore
        if (pattern.findFirstIn(tokenizedLine(ix)).getOrElse(None).equals(None)) { //scalastyle:ignore
          count = count + 1
          wordPos match {
            case LEFT => tmpSurroundings(2 - count) = mutable.HashMap(IDX-> ix, TOKEN -> tokenizedLine(ix), CONSIDER -> 0, PENALTY -> 0f)
            case RIGHT => tmpSurroundings(1 + count) = mutable.HashMap(IDX-> ix, TOKEN -> tokenizedLine(ix), CONSIDER -> 0, PENALTY -> 0f)
          }
        }
        if (count.equals(2)) { //scalastyle:ignore
          break
        }
      }
    }
  }
}