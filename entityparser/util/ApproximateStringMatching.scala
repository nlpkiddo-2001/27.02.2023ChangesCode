package com.zoho.crm.feature.entityparser.util

import java.util.Locale

import org.apache.commons.codec.language.Soundex
import org.apache.commons.text.similarity.{FuzzyScore, JaroWinklerDistance}

object ApproximateStringMatching {

  def parseTexts(text: String, patternsToMatch: Array[String]): List[List[(String, String, Double)]] ={
    patternsToMatch.map(x => {
      x.split(" ").indices.map(len=> {
        text.split("\n").map(
          x => x.split(' ').filter(_.nonEmpty).sliding(len+1).map(p => p.mkString(" ").trim)).toList.flatten
      }).toList.flatten.map(y=> {
        (x, y, new JaroWinklerDistance().apply(y, x)
          + new FuzzyScore(Locale.getDefault()).fuzzyScore(y, x)
          + new Soundex().difference(y, x)/4)
      })
    }).toList
  }

  def parseText(text: String, patternToMatch: String): List[(String, String, Double)] ={
    this.parseTexts(text, Array[String](patternToMatch)).head
  }

  def firstMatch(text: String, patternToMatch: String): String = {
    this.parseTexts(text, Array[String](patternToMatch)).head.maxBy(_._3)._2
  }

}
