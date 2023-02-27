//$Id$
package com.zoho.zia.crm.activityextractor;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zoho.zia.web.model.activity.*;
import org.springframework.stereotype.Component;

import com.zoho.crm.common.utils.CommonUtils;
import com.zoho.crm.maildata.LanguageDetector;
import com.zoho.crm.utils.SignatureExtraction;
import com.zoho.zia.api.textservice.annotation.TextFeature;
import com.zoho.zia.api.textservice.request.TextServiceInput;
import com.zoho.zia.api.textservice.request.params.ParamInterface;
import com.zoho.zia.api.textservice.response.ErrorResponse;
import com.zoho.zia.api.textservice.response.FeatureResponse;
import com.zoho.zia.api.textservice.response.ResponseCodes;
import com.zoho.zia.api.textservice.response.TextServiceResponse;
import com.zoho.zia.api.textservice.response.v1.Features;
import com.zoho.zia.api.textservice.utils.ReturnEither;
import com.zoho.zia.api.textservice.utils.TextServiceFeatures;
import com.zoho.zia.api.textservice.utils.TextServiceRegistrable;
import com.zoho.zia.crm.activityextractor.actors.BulkActivityActor;
import com.zoho.zia.crm.activityextractor.classify.ActivityClassifier;
import com.zoho.zia.utils.ConfigVariables;
import com.zoho.zia.utils.GlobalActor;
import com.zoho.zia.utils.preprocessing.LanguageDetectionUtils;
import com.zoho.zia.web.model.python.PythonRequest;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

@Component
public class ActivityParser implements TextServiceRegistrable {

	private static final Logger LOGGER = Logger.getLogger(ActivityParser.class.getName());

	/**
	 * This method will extract activity in given mail content.
	 *
	 * @param extractorInput
	 *            JSON input for activity extraction
	 * @return
	 */
	public static TextServiceResponse parse(ExtractorInput extractorInput) {
		// Pre processing part

		// remove html if input is in html format
		extractorInput.setMailContent(ExtractorUtils.getProcessedMail(extractorInput.getMailContent()));

		// Python call for language Detection Return 400 if input language is
		// not supported
		String language = LanguageDetectionUtils.detectLanguage(extractorInput.getMailContent());
		if (language == null) {
			LOGGER.info("EntityFilter :: Python call failed. Using java language detector ");
			language = LanguageDetector.detectLanguage(extractorInput.getMailContent());
		}
		// Extract signature from input
		String bodyContent = ExtractorUtils.getProcessedMailBody(
				SignatureExtraction.extract(extractorInput.getMailContent(), false).getBody(),
				extractorInput.getMailContent());

		// Python call for activity
		List<PythonRequest> messages = new ArrayList<>();
		messages.add(new PythonRequest("0", bodyContent));
		Map<String, ActivityV1PythonResponse> activityTypeId;
		try {
			activityTypeId = ActivityClassifier.predictActivity(messages);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "ActivityExtraction :: Exception while executing Activity Python Call",  // no i18n
					Arrays.toString(e.getStackTrace()));
			return new ErrorResponse(ResponseCodes.INTERNAL_SERVER_ERROR, "internal server error unknown exception"); // no i18n
		}
		ActivityV1PythonResponse activityResponse = activityTypeId != null ? activityTypeId.get("0") : null;
		String activityType = activityResponse != null ? activityResponse.getType() : "Others";  // no i18n
		List<String> participants = activityResponse != null ? activityResponse.getParticipants() : null;
		List<String> location = activityResponse != null ? activityResponse.getLocation() : null;

		// Using Bulk activity Method for TextServiceResponse
		ExtractorBulkInput extractorBulkInput = new ExtractorBulkInput(extractorInput.getId(),
				extractorInput.getSubject(), extractorInput.getMailContent(), extractorInput.getDateString(),
				extractorInput.getTimeZone(), language, bodyContent, activityType, participants, location);
		return parseBulkActivity(extractorBulkInput);
	}

	public static TextServiceResponse parseBulkActivity(ExtractorBulkInput extractorInput) {

		try {
			// Pre processing part
			long start = System.currentTimeMillis();
			FeatureResponse<ExtractorResponseReturn> textServiceResponse = new FeatureResponse<>();
			textServiceResponse.setId(extractorInput.getId());
			textServiceResponse.setStatusCode(204);

			String language = extractorInput.getLanguage();

			if (!language.equals("English")) {
				ErrorResponse errorResponse = new ErrorResponse(ResponseCodes.LANGUAGE_NOT_SUPPORTED);
				errorResponse.setMessage(language + " is not supported"); // No I18N
				errorResponse.setId(extractorInput.getId());
				return errorResponse;
			}

			ExtractorResponseReturn extractorResponse = new ExtractorResponseReturn();
			String bodyContent = extractorInput.getMailBody();

			// Return 400 if body of the mail length is greater than configured.
			if (bodyContent.length() > ConfigVariables.LENGTH_LIMIT_FOR_ACTIVITY) {
				ErrorResponse errorResponse = new ErrorResponse(ResponseCodes.TEXT_LENGTH_EXCEEDED);
				errorResponse.setMessage("Mail body Length is greater than 5000"); // No I18N
				errorResponse.setId(extractorInput.getId());
				return errorResponse;
			}

			// getting time zone from input
			String timeZone = extractorInput.getTimeZone() != null ? extractorInput.getTimeZone()
					: ZoneId.systemDefault().toString();
			ExtractorInputBody extractorInputBody = new ExtractorInputBody(extractorInput.getSubject(),
					extractorInput.getMailContent(), extractorInput.getDateString(), bodyContent, timeZone);

			String activityType = extractorInput.getActivityType();
			List<ActivityResponse> activityResponses = new ArrayList<>();

			if (activityType != null && !activityType.equals("Others")) {
				NEREntities nerEntities = new NEREntities(extractorInput.getLocation(), extractorInput.getParticipants());
				ActivityEntities activityEntities = EntityUtils.extractor(extractorInputBody, nerEntities);
				if (activityEntities != null) {
					ActivityResponse activity = ExtractorUtils.getActivityResponse(activityType, bodyContent,
							activityEntities, extractorInput.getSubject());
					activityResponses.add(activity);
					extractorResponse.setActivityExtractor(activityResponses);
					textServiceResponse.setStatusCode(200);
					textServiceResponse.setResponse(extractorResponse);
				}
			}
			LOGGER.info("EntityFilter :: Total Time Taken for Entity extraction :: "
					+ (System.currentTimeMillis() - start));
			return textServiceResponse;
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, "EntityFilter :: Exception while executing Activity Extraction",
					CommonUtils.getExceptionStringWithTrace(e));
			ErrorResponse errorResponse = new ErrorResponse(ResponseCodes.INTERNAL_SERVER_ERROR);
			errorResponse.setMessage("Error while processing request"); // No I18N
			errorResponse.setId(extractorInput.getId());
			return errorResponse;
		}
	}

	public Features parse(List<ExtractorBulkInput> extractorInputs) throws Exception {
		LOGGER.info("BulkActivityExtraction :: Calling Actors for bulk prediction ");
		ActorRef activityBulkActor = GlobalActor.globalActorSystem().actorOf(Props.create(BulkActivityActor.class));
		Timeout timeout = new Timeout(Duration.create(3000, "milliseconds")); // No I18N
		Future<Object> future = Patterns.ask(activityBulkActor, new ActivityBulkInput(extractorInputs), timeout);
		return (Features) Await.result(future, timeout.duration());
	}

	@TextFeature(feature = TextServiceFeatures.ActivityExtraction)
	public ReturnEither<ErrorResponse, List<TextServiceResponse>> bulkPredict(
			List<TextServiceInput> textServiceRequests, HashMap<String, ParamInterface<Object>> featureParams) {
		List<ExtractorBulkInput> extractorInputs = new ArrayList<>();
		List<ExtractorBulkInput> extractorBulkInputList = new ArrayList<>();
		List<PythonRequest> messages = new ArrayList<>();
		Map<String, ActivityV1PythonResponse> activityTypeList;

		for (TextServiceInput textServiceRequest : textServiceRequests) {
			ExtractorBulkInput extractorInput = ExtractorUtils.getExtractorBulkInput(textServiceRequest);
			messages.add(new PythonRequest(extractorInput.getId(), extractorInput.getMailBody()));
			extractorInputs.add(extractorInput);
		}
		try {
			activityTypeList = ActivityClassifier.predictActivity(messages);
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, "BulkActivityExtraction :: Exception while executing Activity Python Call", // no i18n
					Arrays.toString(e.getStackTrace()));
			return ReturnEither.left(
					new ErrorResponse(ResponseCodes.INTERNAL_SERVER_ERROR, "internal server error unknown exception")); // no i18n
		}

		for (ExtractorBulkInput extractorBulkInput : extractorInputs) {
			String id = extractorBulkInput.getId();
			ActivityV1PythonResponse activityResponse = activityTypeList != null ? activityTypeList.get(id) : null;
			String activityType = activityResponse != null ? activityResponse.getType() : "Others"; // no i18n
			List<String> participants = activityResponse != null ? activityResponse.getParticipants() : null;
			List<String> location = activityResponse != null ? activityResponse.getLocation() : null;
			extractorBulkInput.setActivityType(activityType);
			extractorBulkInput.setParticipants(participants);
			extractorBulkInput.setLocation(location);
			extractorBulkInputList.add(extractorBulkInput);
		}

		try {
			return ReturnEither.right(parse(extractorBulkInputList).getFeatures());
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, "BulkActivityExtraction :: Exception while executing Activity Extraction", // no i18n
					Arrays.toString(e.getStackTrace()));
			return ReturnEither.left(
					new ErrorResponse(ResponseCodes.INTERNAL_SERVER_ERROR, "internal server error unknown exception")); // no i18n
		}
	}

}
