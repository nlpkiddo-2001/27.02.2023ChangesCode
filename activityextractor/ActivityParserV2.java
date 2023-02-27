//$Id$
package com.zoho.zia.crm.activityextractor;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zoho.zia.crm.activityextractor.actors.*;
import com.zoho.zia.web.model.activity.*;
import com.zoho.zia.web.model.preprocess.SignatureExtractor;
import org.json.JSONObject;
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
import com.zoho.zia.utils.ConfigVariables;
import com.zoho.zia.utils.GlobalActor;
import com.zoho.zia.utils.preprocessing.LanguageDetectionUtils;
import com.zoho.zia.web.model.python.PythonRequest;
import com.zoho.zia.crm.activityextractor.actors.PythonRequestMessages.*;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

@Component
public class ActivityParserV2 implements TextServiceRegistrable {
	private static final Logger LOGGER = Logger.getLogger(ActivityParserV2.class.getName());

	/**
	 * This method will extract activity in given mail content.
	 *
	 * @param extractorInput
	 *            JSON input for activity extraction
	 * @return
	 */
	public static TextServiceResponse parseV2(ExtractorInputV2 extractorInput) {
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
		SignatureExtractor signatureExtractionOutput = SignatureExtraction.extract(extractorInput.getMailContent(), false);
		String bodyContent = ExtractorUtils.getProcessedMailBody(
				signatureExtractionOutput.getBody(),
				extractorInput.getMailContent());

		// Python call for activity
		if (!language.equals("English")) {
			ErrorResponse errorResponse = new ErrorResponse(ResponseCodes.LANGUAGE_NOT_SUPPORTED);
			errorResponse.setMessage(language + " is not supported"); // No I18N
			errorResponse.setId(extractorInput.getId());
			return errorResponse;
		}

		if (bodyContent.trim().length()==0)  {
			FeatureResponse<ActivityResponseV2> textServiceResponse = new FeatureResponse<>();
			LOGGER.info("Activity Extraction V2: Found empty message with id" + extractorInput.getId());
			textServiceResponse.setId(extractorInput.getId());
			textServiceResponse.setStatusCode(204);
			return textServiceResponse;
		} else if (bodyContent.length() > ConfigVariables.LENGTH_LIMIT_FOR_ACTIVITY) {
			ErrorResponse errorResponse = new ErrorResponse(ResponseCodes.TEXT_LENGTH_EXCEEDED);
			errorResponse.setMessage("Mail body Length is greater than 5000"); // No I18N
			errorResponse.setId(extractorInput.getId());
			return errorResponse;
		}
		List<PythonRequest> messages = new ArrayList<>();
		String signatureBody = signatureExtractionOutput.getBody();
		String emailBody = extractorInput.getMailContent();

		messages.add(new PythonRequest("0", (signatureBody.length() > 1) ? signatureBody : emailBody));
		ClassificationRequestInput activityActorInput = new ClassificationRequestInput(messages);
		PythonRequestActorResponse ActorResponse = null;

		if (extractorInput.getIsCommitmentRequired()!= null && extractorInput.getIsCommitmentRequired()){
			ClassificationRequestInput commitmentActorInput = new ClassificationRequestInput(messages);
			PythonRequestActorInput actorRequestMessage = new PythonRequestActorInput(activityActorInput, commitmentActorInput);

			//Calling Python call to predict commitment and activity:
			ActorRef mainActivityActor = GlobalActor.globalActorSystem().actorOf(Props.create(ClassificationRequestActor.class));
			Timeout timeout = new Timeout(Duration.create(1000, "milliseconds")); // No I18N
			Future<Object> future = Patterns.ask(mainActivityActor, actorRequestMessage, timeout);
			try {
				ActorResponse = (PythonRequestActorResponse) Await.result(future, timeout.duration());
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "PythonRequestActor :: Exception while executing python request actor",
						CommonUtils.getExceptionStringWithTrace(e));
				return new ErrorResponse(ResponseCodes.INTERNAL_SERVER_ERROR, "internal server error unknown exception"); // no i18n
			}
		} else{
			ActorRef mainActivityActor = GlobalActor.globalActorSystem().actorOf(Props.create(ActivityV2RequestActor.class));
			Timeout timeout = new Timeout(Duration.create(1000, "milliseconds")); // No I18N
			Future<Object> future = Patterns.ask(mainActivityActor, activityActorInput, timeout);
			ActivityClassificationResponse activityActorResponse;
			try {
				activityActorResponse = (ActivityClassificationResponse) Await.result(future, timeout.duration());
				ActorResponse = new PythonRequestActorResponse(activityActorResponse,
								new CommitmentClassificationResponse(new HashMap<>()));
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "PythonRequestActor :: Exception while executing python request actor",
						CommonUtils.getExceptionStringWithTrace(e));
				return new ErrorResponse(ResponseCodes.INTERNAL_SERVER_ERROR, "internal server error unknown exception"); // no i18n
			}
		}

		Map<String, ActivityPythonSingleResponse> activityOutput = ActorResponse.getActivityResponse().getResponse();
		Map<String, CommitmentSingleResponse> commitmentOutput = ActorResponse.getCommitmentResponse().getResponse();

		ActivityPythonSingleResponse activityOutputDetails = activityOutput.getOrDefault("0", null);
		Boolean activityPresent = activityOutputDetails != null && activityOutputDetails.getIsActivityPresent();
		List<ActivityAttributes> activityAttributes = activityOutputDetails != null ? activityOutputDetails.getActivityAttributes() : null;

		CommitmentSingleResponse commitmentOutputDetails = commitmentOutput.getOrDefault("0", null);
		Boolean commitmentPresent = commitmentOutputDetails != null && commitmentOutputDetails.getIsCommitmentPresent();
		CommitmentAttributes commitmentAttributes = commitmentOutputDetails != null ? commitmentOutputDetails.getCommitmentAttributes() : null;

		// Using Bulk activity Method for TextServiceResponse
		ExtractorBulkInputV2 extractorBulkInput = new ExtractorBulkInputV2(extractorInput.getId(),
				extractorInput.getSubject(), extractorInput.getMailContent(), extractorInput.getDateString(),
				extractorInput.getTimeZone(), language, bodyContent, extractorInput.getIsCommitmentRequired() != null ?extractorInput.getIsCommitmentRequired() : false, activityPresent,
				activityAttributes, commitmentPresent, commitmentAttributes);
		return parseBulkActivityV2(extractorBulkInput);
	}

	public static TextServiceResponse parseBulkActivityV2(ExtractorBulkInputV2 extractorInput) {

		try {
			// Pre processing part
			long start = System.currentTimeMillis();
			FeatureResponse<ActivityResponseV2> textServiceResponse = new FeatureResponse<>();
			textServiceResponse.setId(extractorInput.getId());
			textServiceResponse.setStatusCode(204);

			String language = extractorInput.getLanguage();

			if (!language.equals("English")) {
				ErrorResponse errorResponse = new ErrorResponse(ResponseCodes.LANGUAGE_NOT_SUPPORTED);
				errorResponse.setMessage(language + " is not supported"); // No I18N
				errorResponse.setId(extractorInput.getId());
				return errorResponse;
			}

			String bodyContent = extractorInput.getMailBody();

			if (bodyContent.trim().length()==0)  {
				LOGGER.info("Activity Extraction V2: Found empty message with id" + extractorInput.getId());
				return textServiceResponse;
			}
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

			Boolean activityPresent = extractorInput.getIsActivityPresent();
			Boolean isCommitmentPresent = extractorInput.getIsCommitmentPresent();

			if ((activityPresent == null || !activityPresent ) && (isCommitmentPresent == null || !isCommitmentPresent)) {
				textServiceResponse.setStatusCode(204);
				return textServiceResponse;
			}

			List<ActivitySingleOutput> activityResponses = new ArrayList<ActivitySingleOutput>();
			if ( activityPresent != null && extractorInput.getIsActivityPresent()){
				ActivityV2Entities activityEntities = EntityUtils.extractorV2(extractorInputBody);
				ActivitySingleOutput activityOutput = ExtractorUtils.getActivityResponseV2(extractorInput.getActivityAttributes(), extractorInputBody,
						activityEntities, extractorInput.getSubject());

				if (!activityOutput.getActivityAttributes().isEmpty()){
					activityResponses.add(activityOutput);
				}
			}


			// add commitment outputs to response
			CommitmentAttributes commitmentAttributes = extractorInput.getCommitmentAttributes();
			String refDateString = extractorInputBody.getDateString();
			List<CommitmentSingleOutput> commitmentOutputsList = new ArrayList<>();
			if (commitmentAttributes != null) {
				// get date entities in each sentence with commitment and add to
				// response
				for (String sentence : commitmentAttributes.getSentences()) {
					JSONObject dates = EntityUtils.extractDatesFromText(sentence, refDateString, timeZone);
					if (dates != null) {
						CommitmentSingleOutput cmtSingleOutput = new CommitmentSingleOutput(sentence, dates,
								commitmentAttributes.getSummary());
						commitmentOutputsList.add(cmtSingleOutput);
					}
				}
			}
			if (activityResponses.isEmpty() && commitmentOutputsList.isEmpty()){
				textServiceResponse.setStatusCode(204);
				return textServiceResponse;
			}

			ActivityResponseV2 activity = new ActivityResponseV2();
			LOGGER.info("########################activity is being created_---------------------#########");
			activity.setActivityOutput(activityResponses);
			LOGGER.info("activity response has been set########" + activityResponses);
			activity.setCommitmentOutput(commitmentOutputsList);
			LOGGER.info("commitmentoutput has been set#####   ");
			textServiceResponse.setStatusCode(200);
			textServiceResponse.setResponse(activity);
			LOGGER.info("EntityFilter :: Total Time Taken for Entity extraction :: "
					+ (System.currentTimeMillis() - start));
			LOGGER.info("returning textservice response  " + textServiceResponse);
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

	public Features parse(List<ExtractorBulkInputV2> extractorInputs) throws Exception {
		LOGGER.info("BulkActivityExtractionV2 :: Calling Actors for bulk prediction ");
		ActorRef activityBulkActor = GlobalActor.globalActorSystem().actorOf(Props.create(BulkActivityActorV2.class));
		Timeout timeout = new Timeout(Duration.create(3000, "milliseconds")); // No I18N
		Future<Object> future = Patterns.ask(activityBulkActor, new ActivityBulkInputV2(extractorInputs), timeout);
		return (Features) Await.result(future, timeout.duration());
	}

	@TextFeature(feature = TextServiceFeatures.ActivityExtractionV2)
	public ReturnEither<ErrorResponse, List<TextServiceResponse>> bulkPredict(
			List<TextServiceInput> textServiceRequests, HashMap<String, ParamInterface<Object>> featureParams) {
		List<ExtractorBulkInputV2> extractorInputs = new ArrayList<>();
		List<PythonRequest> activityMessages = new ArrayList<>();
		List<PythonRequest> commitmentMessages = new ArrayList<>();
		Map<String, ActivityPythonSingleResponse> activityTypeList;
		Map<String, CommitmentSingleResponse> commitmentOutput;

		for (TextServiceInput textServiceRequest : textServiceRequests) {
			ExtractorBulkInputV2 SingleExtractorInput = ExtractorUtils.getExtractorBulkInputV2(textServiceRequest);
			// Languages other than English won't be added into extractorInputs and also message will be
			// empty only if language is other than English
			if (!SingleExtractorInput.getMailBody().isEmpty() || (SingleExtractorInput.getLanguage().equals("English"))) {
				activityMessages.add(new PythonRequest(SingleExtractorInput.getId(), SingleExtractorInput.getMailContent()));
				if (SingleExtractorInput.getIsCommitmentRequired() != null && SingleExtractorInput.getIsCommitmentRequired()) {
					commitmentMessages.add(new PythonRequest(SingleExtractorInput.getId(), SingleExtractorInput.getMailBody()));
				}
			}
			extractorInputs.add(SingleExtractorInput);
		}

		PythonRequestActorResponse ActorResponse = null;
		if (activityMessages.isEmpty() && commitmentMessages.isEmpty()){
			ActorResponse = new PythonRequestActorResponse(new ActivityClassificationResponse(new HashMap<>()), new CommitmentClassificationResponse(new HashMap<>()));
		} else if (commitmentMessages.isEmpty()){
			ActorRef mainActivityActor = GlobalActor.globalActorSystem().actorOf(Props.create(ActivityV2RequestActor.class));
			Timeout timeout = new Timeout(Duration.create(1000, "milliseconds")); // No I18N
			Future<Object> future = Patterns.ask(mainActivityActor, new ClassificationRequestInput(activityMessages), timeout);
			ActivityClassificationResponse activityActorResponse;
			try {
				activityActorResponse = (ActivityClassificationResponse) Await.result(future, timeout.duration());
				ActorResponse = new PythonRequestActorResponse(activityActorResponse, new CommitmentClassificationResponse(new HashMap<>()));
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "PythonRequestActor :: Exception while executing python request actor",
						CommonUtils.getExceptionStringWithTrace(e));
				return ReturnEither.left(
						new ErrorResponse(ResponseCodes.INTERNAL_SERVER_ERROR, "internal server error unknown exception")); // no i18n
			}
		}
		else{
			PythonRequestActorInput actorRequestMessage = new PythonRequestActorInput(
					new ClassificationRequestInput(activityMessages),
					new ClassificationRequestInput(commitmentMessages)
			);

			//Calling Python call to predict commitment and activity:
			ActorRef mainActivityActor = GlobalActor.globalActorSystem().actorOf(Props.create(ClassificationRequestActor.class));
			Timeout timeout = new Timeout(Duration.create(1000, "milliseconds")); // No I18N
			Future<Object> future = Patterns.ask(mainActivityActor, actorRequestMessage, timeout);
			try {
				ActorResponse = (PythonRequestActorResponse) Await.result(future, timeout.duration());
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "PythonRequestActor :: Exception while executing python request actor",
						CommonUtils.getExceptionStringWithTrace(e));
				return ReturnEither.left(
						new ErrorResponse(ResponseCodes.INTERNAL_SERVER_ERROR, "internal server error unknown exception")); // no i18n
			}
		}


		activityTypeList = ActorResponse.getActivityResponse().getResponse();
		commitmentOutput = ActorResponse.getCommitmentResponse().getResponse();

		for (ExtractorBulkInputV2 extractorBulkInput : extractorInputs) {
			String id = extractorBulkInput.getId();
			ActivityPythonSingleResponse singleResponse = activityTypeList.getOrDefault(id, null);
			Boolean activityPresent = singleResponse != null && singleResponse.getIsActivityPresent();
			extractorBulkInput.setIsActivityPresent(activityPresent);
			List<ActivityAttributes> activityAttributes = singleResponse != null ? singleResponse.getActivityAttributes() : null;
			extractorBulkInput.setActivityAttributes(activityAttributes);

			CommitmentSingleResponse commitmentSingleResponse = commitmentOutput.getOrDefault(id, null);
			Boolean commitmentPresent = commitmentSingleResponse != null && commitmentSingleResponse.getIsCommitmentPresent();
			extractorBulkInput.setIsCommitmentPresent(commitmentPresent);
			extractorBulkInput.setCommitmentAttributes(commitmentSingleResponse != null ? commitmentSingleResponse.getCommitmentAttributes() : null);
		}

		try {
			return ReturnEither.right(parse(extractorInputs).getFeatures());
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, "BulkActivityExtractionV2 :: Exception while executing Activity Extraction", // no i18n
					Arrays.toString(e.getStackTrace()));
			return ReturnEither.left(
					new ErrorResponse(ResponseCodes.INTERNAL_SERVER_ERROR, "internal server error unknown exception")); // no i18n
		}
	}

}
