package com.example.demo

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation
import edu.stanford.nlp.util.CoreMap

import java.util.Properties
import scala.collection.JavaConverters._

  class EnquiryExtraction {

    private final val ENQUIRY_EXTRACTION_WORDS: Array[String] = Array(
      "enquiry", "inquiry", "question", "request", "concern", "use", "access", "demo", "problem",
      "guide", "guidance", "issue", "facing", "support", "complaince"
    )

    private final val PRODUCT_WORDS: Array[String] = Array(
      "ABC product1", "ABC product2”,”ABC product3"
    ) //example products

    def enquiryExtract(emailBody: String): String = {
      if (emailBody.length == 0 || emailBody.length < 5)
        return " "
      val fromNER = extractWithNER(emailBody)
      val fromList = extractWithList(emailBody)
      if (fromNER.nonEmpty) fromNER
      else fromList
    }

    private def extractWithNER(emailBody: String): String = {
      val props = new Properties()
      props.setProperty("annotators", "tokenize,ssplit,pos,depparse")
      props.setProperty("ner.useSUTime", "false")
      val pipeLine = new StanfordCoreNLP(props)
      val document = new Annotation(emailBody)
      pipeLine.annotate(document)

      val sentences = document.get(classOf[CoreAnnotations.SentencesAnnotation])
      val relevantSentence = sentences.asScala.filter(sentenceContainsEnquiryWords)
      val relevantToken = relevantSentence.flatMap(_.get(classOf[CoreAnnotations.TokensAnnotation]).asScala)
        .filter(token => token.ner() == "PERSON" || token.ner() == "ORGANIZATION" || token.ner() == "MISC")
      relevantToken.map(_.word()).mkString(" ")
    }

    private def extractWithList(emailBody: String): String = {
      val sentence = emailBody.split("[\\.\\?!]").map(_.trim)
      val relevantSentences = sentence.filter(sentenceContainsEnquiryWords1)
      relevantSentences.mkString(" ")
    }


    private def sentenceContainsEnquiryWords(sentence: CoreMap): Boolean = {
      val words = sentence.get(classOf[CoreAnnotations.TokensAnnotation]).asScala.map(_.originalText().toLowerCase())
      words.exists(word => ENQUIRY_EXTRACTION_WORDS.contains(word)) || words.exists(word => PRODUCT_WORDS.contains(word))
    }

    private def sentenceContainsEnquiryWords1(sentence: String): Boolean = {
      val words = sentence.split("\\s+").map(_.toLowerCase())
      words.exists(word => ENQUIRY_EXTRACTION_WORDS.contains(word)) || words.exists(word => PRODUCT_WORDS.contains(word))
    }

  }


