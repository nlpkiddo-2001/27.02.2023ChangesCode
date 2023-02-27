package com.zoho.crm.feature.entityparser

import java.util.logging.{Level, Logger}
import com.zoho.crm.feature.entityparser.constants.EntityParserConstants
import com.zoho.crm.feature.entityparser.outputs.{Entities, EntityExtractionException, NoOutput, Span}
import com.zoho.crm.feature.entityparser.util.EntityParserUtil
import com.zoho.crm.feature.entityparser.util.EntityParserUtil.getAllKeysFromPlayJson
import com.zoho.crm.ml.nlp.constants.{ADDRESS, TITLES}
import com.zoho.crm.model.ModelLoadHandler
import com.zoho.zia.api.textservice.response.ErrorResponse
import org.apache.hadoop.yarn.webapp.hamlet.HamletSpec.ADDRESS
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.collection.mutable

object EntityParser  extends ModelLoadHandler {
  val logLogger: Logger = Logger.getLogger("EntityParser")

  def extract(text: String): java.util.Map[String, java.util.List[String]] = {
    val result = EntityParser.extractFromText(text)
    val anonymousF = (x: String) => Some(x)
    entitiesToMap[String](result, anonymousF)
  }

  def extractWithSpan(text: String): java.util.Map[String, java.util.List[Span]] = {
    val entitiesToExtraction = Array[String](
      EntityParserConstants.IP_ADDRESS,
      EntityParserConstants.PHONE,
      EntityParserConstants.EMAIL,
      EntityParserConstants.SOCIAL,
      EntityParserConstants.WEBSITE
    )
    val result = EntityParser.extractFromText(text, entitiesToExtraction)
    val anonymousF: String => Option[Span] = (x: String) => EntityParserUtil.getSpan(text, x)
    entitiesToMap[Span](result, anonymousF)

  }

  def extractFromText(text: String, entities: Array[String] = Array()) : Either[ErrorResponse, Entities] = {
    logLogger.info("===========================EntityExtraction For Text==============================")
    val start = System.currentTimeMillis()

    try {
      val executionOrder = if (entities.isEmpty)
        Array[String](
          EntityParserConstants.IP_ADDRESS,
          EntityParserConstants.PHONE,
          EntityParserConstants.EMAIL,
          EntityParserConstants.WEBSITE
        )
      else
        Executor.executionOrder(entities)

      val parsedEntity = new ParsedEntity(TextInput(text))
      executionOrder.foreach(execute => Executor(execute, parsedEntity))
      val endTime = System.currentTimeMillis() - start
      logLogger.info("EntityParser Predict Fuction compeleted in :::::::::: " + endTime.toString)
      Right(parsedEntity.getEntities)
    } catch {
      case e: EntityExtractionException =>
        val errorResponse = new ErrorResponse(e.errorCodes, e.getMessage)
        Left(errorResponse)
      case _: NoOutput =>
        Right(
          Entities()
        )
    }
  }

  def extractFromEmail(emailContent: String, emailId: String, entities: Array[String] = Array()): Either[ErrorResponse, Entities] = { //scalastyle:ignore
    logLogger.info("===========================EntityExtraction For Email==============================")
    val start = System.currentTimeMillis()

    try {

      val executionOrder = if (entities.isEmpty)
        Array[String](
          EntityParserConstants.SIGNATURE,
          // added by me
          EntityParserConstants.ENQUIRY,
          EntityParserConstants.PHONE,
          TITLES.DESIGNATION,
          EntityParserConstants.EMAIL,
          EntityParserConstants.WEBSITE,
          EntityParserConstants.SOCIAL,
          EntityParserConstants.NAME,
          EntityParserConstants.COMPANY,
          ADDRESS.ADDRESS
        )
      else
        Executor.executionOrder(entities)

      val parsedEntity = new ParsedEntity(EmailInput(emailContent, Some(emailId)))
      executionOrder.foreach(execute => Executor(execute, parsedEntity))
      val endTime = System.currentTimeMillis() - start
      logLogger.info("EntityParser Predict Fuction compeleted in :::::::::: " + endTime.toString)
      Right(parsedEntity.getEntities)
    } catch {
      case e: EntityExtractionException =>
        val errorResponse = new ErrorResponse(e.errorCodes, e.getMessage)
        Left(errorResponse)
      case _: NoOutput =>
        Right(
          Entities()
        )
    }

  }

  def mapEntities(key: String, x: Entities): List[(String, List[String])] = {
    key match{
      case EntityParserConstants.PHONE =>
        List((EntityParserConstants.PHONE, (x.phoneNo ++ x.fax.toList).toList))
      case EntityParserConstants.EMAIL =>
        List((EntityParserConstants.EMAIL,x.email.toList))
      case EntityParserConstants.IP_ADDRESS =>
        List((EntityParserConstants.IP_ADDRESS,x.ipAddress.toList))
      case EntityParserConstants.WEBSITE =>
        List((EntityParserConstants.WEBSITE, x.website.toList))
      case EntityParserConstants.SOCIAL=>
        x.social match {
          case Some(s) =>
            val keys = getAllKeysFromPlayJson(Json.toJson(s))
            keys.flatMap(s => mapEntities(s, x)).toList
          case None => List()
        }
      case EntityParserConstants.SKYPE =>
        List((s"${EntityParserConstants.SOCIAL}/${EntityParserConstants.SKYPE}",
          x.social.get.skype.toList))
      case EntityParserConstants.FACEBOOK =>
        List((s"${EntityParserConstants.SOCIAL}/${EntityParserConstants.FACEBOOK}",
          x.social.get.facebook.toList))
      case EntityParserConstants.LINKEDIN =>
        List((s"${EntityParserConstants.SOCIAL}/${EntityParserConstants.LINKEDIN}",
          x.social.get.linkedin.toList))
      case EntityParserConstants.TWITTER =>
        List((s"${EntityParserConstants.SOCIAL}/${EntityParserConstants.TWITTER}",
          x.social.get.twitter.toList))
      case _ => List()
    }
  }


  def entitiesToMap[A](result: Either[ErrorResponse, Entities], anonymousF: String => Option[A]): java.util.Map[String, java.util.List[A]] ={
    result match {
      case Right(x) =>
        var seq = mutable.ArrayBuffer[(String, java.util.List[A])]()
        val json = Json.toJson(x)
        val keys = getAllKeysFromPlayJson(json)
        keys.foreach(k=>{
          val entities = mapEntities(k, x)
          entities.foreach(x=>{
            val mapped = x._2.map(anonymousF(_))
            seq.+=((x._1, mapped.filter(_.isDefined).map(_.get).asJava))
          })
        })
        seq.toMap.asJava
      case Left(_) => new java.util.HashMap[String, java.util.List[A]]()
    }
  }
  override def isLoad(): Boolean = true

  override def loadModel(): Unit = {}

  override def refreshModel(): Unit = {
    try {
      val emailBody = "&lt;!DOCTYPE html PUBLIC &quot;-//W3C//DTD HTML 4.01 Transitional//EN&quot;&gt;&lt;html&gt;&lt;head&gt;&lt;meta content=3D&quot;text/html;charset=3DUTF-8&quot; http-equiv=3D&quot;Content-Type&quot;&gt;&lt;/head&gt;&lt;body &gt;&lt;div style=3D'\\''font-size:10pt;font-family:Verdana,Arial,Helvetica,sans-serif;color:#00000;'\\''&gt;&lt;div&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px;font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;Hi,&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;      This is the sample email sent to myself for showing the working of email parser and I might add some random stuffs.&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;      Agreed joy vanity regret met may ladies oppose who. Mile fail as left as hard eyes. Meet made call in mean four year it to. Prospect so branched wondered sensible of up. For gay consistedresolving pronounce sportsman saw discovery not. Northward or household as      conveying we earnestly believing. No in up contrasted discretion inhabiting excellence. Entreaties we collecting unpleasant at everything conviction.&lt;span&gt; &lt;/span&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal;font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&amp;nbsp;      Knowledge nay estimable questions repulsive daughters boy. Solicitude gay way unaffected expression for. His mistress ladyship required off horrible disposed rejoiced. Unpleasing pianoforte unreserved as oh he unpleasant no inquietude insipidity. Advantages can       discretion possession add favourablecultivated admiration far. Why rather assure how esteem end hunted nearer and before. By an truth after heard going early given he. Charmed to it excited females whether at examine. Him abilities suffering may are yet dependent.&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;Thank You,&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2;word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;Venkatraman A,&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing:0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255);text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;Member Technical Staff,&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px;-webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&lt;span class=3D&quot;font&quot; style=3D&quot;font-family:proxima_nova_rgregular, Arial, Helvetica, sans-serif&quot;&gt;&lt;span class=3D&quot;size&quot; style=3D&quot;font-size:16px&quot;&gt;&lt;span class=3D&quot;colour&quot;style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt;Zoho Corporation Pvt. Ltd.,&lt;/span&gt;&lt;/span&gt;&lt;span&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt; &lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px;-webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&lt;span class=3D&quot;font&quot; style=3D&quot;font-family:proxima_nova_rgregular, Arial, Helvetica, sans-serif&quot;&gt;&lt;span class=3D&quot;size&quot; style=3D&quot;font-size:16px&quot;&gt;&lt;span class=3D&quot;colour&quot;style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt;Estancia IT Park,&lt;/span&gt;&lt;/span&gt;&lt;span&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt; &lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&lt;span class=3D&quot;font&quot; style=3D&quot;font-family:proxima_nova_rgregular, Arial, Helvetica, sans-serif&quot;&gt;&lt;span class=3D&quot;size&quot; style=3D&quot;font-size:16px&quot;&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt;Plot No. 140 &amp; 151, GST Road,&lt;/span&gt;&lt;/span&gt;&lt;span&gt;&lt;spanclass=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt; &lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400;letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&lt;span class=3D&quot;font&quot; style=3D&quot;font-family:proxima_nova_rgregular, Arial, Helvetica, sans-serif&quot;&gt;&lt;span class=3D&quot;size&quot; style=3D&quot;font-size:16px&quot;&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt;Vallancherry Village,&lt;/span&gt;&lt;/span&gt;&lt;span&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt; &lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&lt;span class=3D&quot;font&quot; style=3D&quot;font-family:proxima_nova_rgregular, Arial, Helvetica, sans-serif&quot;&gt;&lt;span class=3D&quot;size&quot; style=3D&quot;font-size:16px&quot;&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt;Chengalpattu Taluk,&lt;/span&gt;&lt;/span&gt;&lt;span&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt; &lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform:none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style:initial; text-decoration-color: initial;&quot;&gt;&lt;span class=3D&quot;font&quot; style=3D&quot;font-family:proxima_nova_rgregular, Arial, Helvetica, sans-serif&quot;&gt;&lt;span class=3D&quot;size&quot; style=3D&quot;font-size:16px&quot;&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt;Kanchipuram District 603 202, INDIA&lt;/span&gt;&lt;/span&gt;&lt;span&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt; &lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&lt;span class=3D&quot;font&quot; style=3D&quot;font-family:proxima_nova_rgregular, Arial, Helvetica, sans-serif&quot;&gt;&lt;span class=3D&quot;size&quot; style=3D&quot;font-size:16px&quot;&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt;Phone :&lt;/span&gt;&lt;/span&gt;&lt;span&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt; &lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;a style=3D&quot;color: rgb(102, 102, 102); cursor: default; overflow-wrap: break-word; word-break: break-word; margin: 0px; padding: 0px; border: none; font-style: normal; font-weight: 400; font-stretch: inherit; font-size: 16px; line-height: inherit; font-family: proxima_nova_rgregular, Arial, Helvetica, sans-serif; vertical-align: baseline; outline: 0px; text-decoration: none; background: rgb(249, 249, 249); float: none; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px;&quot;&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0,0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt;044 - 67447070&lt;/span&gt;&lt;/span&gt;&lt;/a&gt;&lt;span class=3D&quot;font&quot; style=3D&quot;font-family:proxima_nova_rgregular, Arial, Helvetica, sans-serif&quot;&gt;&lt;span class=3D&quot;size&quot;style=3D&quot;font-size:16px&quot;&gt;&lt;span&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt; &lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255,255)&quot;&gt;/&lt;/span&gt;&lt;/span&gt;&lt;span&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0,0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt; &lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;a style=3D&quot;color: rgb(102, 102,102); cursor: default; overflow-wrap: break-word; word-break: break-word; margin: 0px; padding: 0px; border: none; font-style: normal; font-weight: 400; font-stretch: inherit; font-size: 16px; line-height: inherit; font-family: proxima_nova_rgregular, Arial, Helvetica, sans-serif; vertical-align: baseline; outline: 0px; text-decoration: none; background: rgb(249, 249, 249); float: none; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px;&quot;&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt;044 - 71817070&lt;/span&gt;&lt;/span&gt;&lt;/a&gt;&lt;span class=3D&quot;font&quot; style=3D&quot;font-family:proxima_nova_rgregular, Arial, Helvetica, sans-serif&quot;&gt;&lt;span class=3D&quot;size&quot; style=3D&quot;font-size:16px&quot;&gt;&lt;span&gt;&lt;span class=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt; &lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0);font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&lt;span class=3D&quot;font&quot; style=3D&quot;font-family:proxima_nova_rgregular, Arial,Helvetica, sans-serif&quot;&gt;&lt;span class=3D&quot;size&quot; style=3D&quot;font-size:16px&quot;&gt;&lt;spanclass=3D&quot;colour&quot; style=3D&quot;color:rgb(0, 0, 0)&quot;&gt;&lt;span class=3D&quot;highlight&quot; style=3D&quot;background-color:rgb(255, 255, 255)&quot;&gt;Fax : 044 67447172&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;/span&gt;&lt;br&gt;&lt;/div&gt;&lt;div style=3D&quot;color: rgb(0, 0, 0); font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 13.3333px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight:400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-style: initial; text-decoration-color: initial;&quot;&gt;&lt;a href=3D&quot;twitter.com/zoho&quot; target=3D&quot;_blank&quot;&gt;twitter&lt;/a&gt;&lt;/div&gt;&lt;/div&gt;&lt;br&gt;&lt;/body&gt;&lt;/html&gt;"
      val emailId = "\"venkatraman.a <venkatraman.a@zohocorp.com>\""
      extractFromEmail(emailBody, emailId)
    } catch {
      case e: Throwable =>
        logLogger.log(
          Level.SEVERE,
          "Exception occurred in scheduler while processing AutoEnrichment :" + e.getMessage
        )
        logLogger.log(
          Level.SEVERE,
          e.getStackTrace.mkString("\n")
        )

    }
  }
}
