//$Id$
package com.zoho.zia.crm.activityextractor;


import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zoho.zia.web.model.activity.*;
import org.json.JSONObject;
import org.nd4j.shade.jackson.databind.ObjectMapper;

import com.zoho.crm.common.utils.CommonUtils;
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.language.english.model.DatesFound;
import com.zoho.zia.crm.activityextractor.actors.ActivityMessages.ActivityActorRequest;
import com.zoho.zia.crm.activityextractor.actors.ActivityMessages.ActivityActorResponse;
import com.zoho.zia.crm.activityextractor.actors.PrimaryActivityActor;
import com.zoho.zia.crm.hawking.HawkingMessages.HawkingInputSingle;
import com.zoho.zia.utils.Constants;
import com.zoho.zia.utils.GlobalActor;
import com.zoho.zia.web.service.HawkingService;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

class EntityUtils {

    private static final Logger LOGGER = Logger.getLogger(EntityUtils.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static ActivityEntities extractor(ExtractorInputBody entityText, NEREntities nerEntities) throws Exception {

        ActivityEntities activityEntities = new ActivityEntities();
        DateTimeEntity dateEntity = new DateTimeEntity();
        DatesFound dates;
        try {
            String dateTimeInput = entityText.getMailContent().replaceAll(Constants.MULTIPLE_NEW_LINE, Constants.NEW_LINE)
                    .replaceAll(Constants.NEW_LINE, Constants.COMMA_STRING_WITH_SPACE)
                    .replaceAll(Constants.MULTIPLE_SPACE, Constants.ONE_SPACE)
                    .replaceAll("\"", Constants.EMPTY_STRING) //No I18N
                    .trim();
            Date referenceDate = new Date(Long.parseLong(entityText.getDateString()));
            HawkingConfiguration configuration = new HawkingConfiguration();
            configuration.setTimeZone(entityText.getTimeZone());
            HawkingInputSingle hawkingInput = new HawkingInputSingle(0,dateTimeInput, referenceDate, configuration);
            dates = HawkingService.getDates(hawkingInput);

            dateEntity = DateTimeExtractor.dateTimeEntity(entityText, dates);
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "EntityFilter :: Exception while executing Combined Extractor", CommonUtils.getExceptionStringWithTrace(e));
        }

        //return null if we have only past event
        if (dateEntity.isOnlyPastEvent()) {
            return null;
        }

        //Adding date time component if it exist
        if (dateEntity.getDates() != null) {
            String startDateFormat = DateTimeConverter.getStartTimeFormat(dateEntity.getDates().getParserOutputs(), 0);
            String endDateFormat = DateTimeConverter.getEndTimeFormat(dateEntity.getDates().getParserOutputs(), 0);
            JSONObject formattedDates = dateFormatter(dateEntity, startDateFormat, endDateFormat);
            activityEntities.setDates(formattedDates);
        } else {
            activityEntities.setDates(new JSONObject(JSONObject.NULL));
        }

        //Adding other details like contact, location, and participants to output
        activityEntities.setDatesPresent(dateEntity.getDatePresent());
        List<String> contactInfo = ExtractorUtils.contactExtractor(entityText.getMailContent());
        String description = " ";
        activityEntities.setContactInfo(contactInfo);
        activityEntities.setLocation(nerEntities.getLocation());
        activityEntities.setActivityParticipants(nerEntities.getParticipants());
        activityEntities.setDescription(description);
        LOGGER.info("CombinedEntityExtractor :: RelatedEntity Module Completed");
        return activityEntities;
    }

    static ActivityV2Entities extractorV2(ExtractorInputBody entityText) throws Exception {

        ActivityV2Entities activityEntities = new ActivityV2Entities();
        DateTimeEntity dateEntity = new DateTimeEntity();
        NEREntities nerEntities = new NEREntities();
        DatesFound dates;
        try {
            String dateTimeInput = entityText.getMailContent().replaceAll(Constants.MULTIPLE_NEW_LINE, Constants.NEW_LINE)
                    .replaceAll(Constants.NEW_LINE, Constants.COMMA_STRING_WITH_SPACE)
                    .replaceAll(Constants.MULTIPLE_SPACE, Constants.ONE_SPACE)
                    .replaceAll("\"", Constants.EMPTY_STRING) //No I18N
                    .trim();
            Date referenceDate = new Date(Long.parseLong(entityText.getDateString()));
            HawkingConfiguration configuration = new HawkingConfiguration();
            configuration.setTimeZone(entityText.getTimeZone());
            HawkingInputSingle hawkingInput = new HawkingInputSingle(0,dateTimeInput, referenceDate, configuration);

            //Calling Hawking Service Method and Location :
            ActorRef mainActivityActor = GlobalActor.globalActorSystem().actorOf(Props.create(PrimaryActivityActor.class));
            ActivityActorRequest message = new ActivityActorRequest(hawkingInput, entityText.getMailBody());
            Timeout timeout = new Timeout(Duration.create(2000, "milliseconds")); // No I18N
            Future<Object> future = Patterns.ask(mainActivityActor, message, timeout);
            ActivityActorResponse activityActorResponse = (ActivityActorResponse) Await.result(future, timeout.duration());
            nerEntities = activityActorResponse.getNerEntities();
            dates = activityActorResponse.getDateTimeEntity();

            dateEntity = DateTimeExtractor.dateTimeEntity(entityText, dates);
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "EntityFilter :: Exception while executing Combined Extractor", CommonUtils.getExceptionStringWithTrace(e));
        }


        //Adding date time component if it exist
        if (dateEntity.getDates() != null && !dateEntity.isOnlyPastEvent()) {
            String startDateFormat = DateTimeConverter.getStartTimeFormat(dateEntity.getDates().getParserOutputs(), 0);
            String endDateFormat = DateTimeConverter.getEndTimeFormat(dateEntity.getDates().getParserOutputs(), 0);
            JSONObject formattedDates = dateFormatter(dateEntity, startDateFormat, endDateFormat);
            activityEntities.setDates(formattedDates);
            activityEntities.setDatesPresent(dateEntity.getDatePresent());
        } else {
            activityEntities.setDates(new JSONObject(JSONObject.NULL));
            activityEntities.setDatesPresent(false);
        }

        //Adding other details like contact, location, and participants to output
        List<ContactValidationResponse> contactInfo = ExtractorUtils.contactExtractorV2(entityText.getMailContent());
        String description = " ";
        activityEntities.setContactInfo(contactInfo);
        activityEntities.setActivityParticipants(nerEntities.getParticipants());
        activityEntities.setDescription(description);
        LOGGER.info("CombinedEntityExtractor :: RelatedEntity Module Completed");
        return activityEntities;
    }

  public static JSONObject extractDatesFromText(String text, String refDateString, String timeZone) {
    // extracts date entities from text using hawking service
      Date referenceDate = new Date(Long.parseLong(refDateString));
      HawkingConfiguration configuration = new HawkingConfiguration();
      configuration.setTimeZone(timeZone);
      DatesFound dates = HawkingService.getDates(new HawkingInputSingle(0, text, referenceDate, configuration));
      ExtractorInputBody entityText = new ExtractorInputBody(" ", text, refDateString, text, timeZone);
      DateTimeEntity dateEntity = DateTimeExtractor.dateTimeEntity(entityText, dates);

      if (dateEntity.isOnlyPastEvent()) {
          return null;
      }

      //Adding date time component if it exist
      if (dateEntity.getDates() != null) {
          String startDateFormat = DateTimeConverter.getStartTimeFormat(dateEntity.getDates().getParserOutputs(), 0);
          String endDateFormat = DateTimeConverter.getEndTimeFormat(dateEntity.getDates().getParserOutputs(), 0);
          try {
              return dateFormatter(dateEntity, startDateFormat, endDateFormat);
          } catch (Exception e) {
              LOGGER.severe("ActivityExtraction EntityUtils ::: Error while formatting date for text: " + text); //No I18N
              return new JSONObject(JSONObject.NULL);
          }
      } else {
          return new JSONObject(JSONObject.NULL);
      }
    }

    private static JSONObject dateFormatter(DateTimeEntity dateEntity, String startDateFormat, String endDateFormat) throws Exception {
        JSONObject jsonObject = new JSONObject(MAPPER.writeValueAsString(dateEntity.getDates())).getJSONArray("parserOutputs").getJSONObject(0);  // No I18N
        jsonObject.remove("parserEndIndex");  // No I18N
        jsonObject.remove("parserLabel");  // No I18N
        jsonObject.remove("id");  // No I18N
        jsonObject.remove("parserStartIndex");  // No I18N
        boolean isTZPresent = dateEntity.getDates().getParserOutputs().get(0).getIsTimeZonePresent();
        String timeZone = isTZPresent ? dateEntity.getDates().getParserOutputs().get(0).getTimezoneOffset() : null;
        jsonObject.put("timeZone", timeZone != null ? timeZone : JSONObject.NULL);  // No I18N
        jsonObject.remove("timezoneOffset");  // No I18N
        jsonObject.remove("isTimeZonePresent"); // No I18N
        jsonObject.getJSONObject("dateRange").remove("matchType");  // No I18N
        jsonObject.getJSONObject("dateRange").remove("start");  // No I18N
        jsonObject.getJSONObject("dateRange").remove("end");     // No I18N
        if (startDateFormat != null) {
            jsonObject.getJSONObject("dateRange").put("startDateFormat", startDateFormat);
        } else {
            jsonObject.getJSONObject("dateRange").put("startDateFormat", JSONObject.NULL);
        }
        if (endDateFormat != null) {
            jsonObject.getJSONObject("dateRange").put("endDateFormat", endDateFormat);
        } else {
            jsonObject.getJSONObject("dateRange").put("endDateFormat", JSONObject.NULL);
        }

        return jsonObject;
    }
}
