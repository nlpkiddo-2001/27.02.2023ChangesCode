//$Id$
package com.zoho.zia.crm.activityextractor.classify;

import java.util.*;
import java.util.logging.Logger;

import com.zoho.zia.web.model.activity.ActivityV1PythonResponse;
import com.zoho.zia.web.model.activity.CommitmentSingleResponse;

import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zoho.crm.common.utils.RebrandUtil;
import com.zoho.crm.core.util.CrmIRequest;
import com.zoho.crm.core.util.CrmIResponse;
import com.zoho.crm.data.PythonUtil;
import com.zoho.zia.crm.activityextractor.actors.PythonRequestMessages.*;
import com.zoho.zia.utils.Constants;
import com.zoho.zia.web.model.activity.ActivityPythonSingleResponse;
import com.zoho.zia.web.model.python.PythonAPIRequest;
import com.zoho.zia.web.model.python.PythonRequest;

public class ActivityClassifier {

	private static final Logger LOGGER = Logger.getLogger(ActivityClassifier.class.getName());
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Gson GSON = new Gson();

	public static Map<String, ActivityV1PythonResponse> predictActivity(List<PythonRequest> messages) throws Exception {

		long start = System.currentTimeMillis();
		Map<String, ActivityV1PythonResponse> activityTypeOutput = new HashMap<>();
		PythonAPIRequest classificationRequest = new PythonAPIRequest(messages);
		String requestBody = MAPPER.writeValueAsString(classificationRequest);
		CrmIRequest<String> classifierRequest = new CrmIRequest<>(
				RebrandUtil.getRebrandKeys("PY_CRMINTELLIGENCE_DOMAIN") + Constants.ACTIVITY_CLASSIFIER_V1_END_POINT, // No I18N
				null, (Map<String, String>) null, ContentType.APPLICATION_JSON, requestBody);


		CrmIResponse<String> classifierResponse = PythonUtil.sendPostRequest(1L, classifierRequest, false);

		if (classifierResponse.statusCode() == 200) {
			JsonObject jsonObject = GSON.fromJson(classifierResponse.data(), JsonObject.class);
			ArrayList<ActivityV1PythonResponse> response = GSON.fromJson(jsonObject.get("response"),
					new TypeToken<ArrayList<ActivityV1PythonResponse>>() {}.getType());
			for (ActivityV1PythonResponse singleResponse : response) {
				activityTypeOutput.put(singleResponse.getId(), singleResponse);
				LOGGER.info("ActivityClassifierV1 :: ID :: " + singleResponse.getId() + "::Type::" + singleResponse.getType());
			}
			LOGGER.info("ActivityClassifierV1 :: Time Taken For Activity Classifier Bulk ::: "
					+ (System.currentTimeMillis() - start));
			return activityTypeOutput;
		} else {
			LOGGER.severe(
					"ActivityClassifierV1 :: Error when getting data from python server" + classifierResponse.data()); // No I18N
			return null;
		}
	}

	public static ActivityClassificationResponse predictActivityV2(List<PythonRequest> messages) throws Exception {

		long start = System.currentTimeMillis();
		Map<String, ActivityPythonSingleResponse> activityTypeOutput = new HashMap<>();
		PythonAPIRequest classificationRequest = new PythonAPIRequest(messages);
		String requestBody = MAPPER.writeValueAsString(classificationRequest);
		CrmIRequest<String> classifierRequest = new CrmIRequest<>(
				PythonUtil.getGpuURL(Constants.ACTIVITY_CLASSIFIER_V2_END_POINT),
				null, (Map<String, String>) null, ContentType.APPLICATION_JSON, requestBody);

		CrmIResponse<String> classifierResponse = PythonUtil.sendPostRequest(1L, classifierRequest, false);
		if (classifierResponse.statusCode() == 200) {
			JsonObject jsonObject = GSON.fromJson(classifierResponse.data(), JsonObject.class);
			ArrayList<ActivityPythonSingleResponse> response = GSON.fromJson(jsonObject.get("response"),
					new TypeToken<ArrayList<ActivityPythonSingleResponse>>() {}.getType());
			for (ActivityPythonSingleResponse singleResponse : response) {
				activityTypeOutput.put(singleResponse.getId(), singleResponse);
				LOGGER.info("ActivityClassifierV2 :: ID :: " + singleResponse.getId() + "::isActivityPresent::" + singleResponse.getIsActivityPresent());
			}
			LOGGER.info("ActivityClassifierV2 :: Time Taken For Activity Classifier Bulk ::: " + (System.currentTimeMillis() - start));
		} else {
			LOGGER.severe("ActivityClassifierV2 :: Error when getting data from python server" + classifierResponse.data()); // No I18N
		}
		return new ActivityClassificationResponse(activityTypeOutput);
	}

	public static CommitmentClassificationResponse predictCommitment(List<PythonRequest> messages) throws Exception{
		long start = System.currentTimeMillis();
		Map<String, CommitmentSingleResponse> commitmentOutput = new HashMap<>();
		PythonAPIRequest classificationRequest = new PythonAPIRequest(messages);
		String requestBody = MAPPER.writeValueAsString(classificationRequest);
		CrmIRequest<String> classifierRequest = new CrmIRequest<>(
				PythonUtil.getGpuURL( Constants.COMMITMENT_CLASSIFIER_END_POINT),
				null, (Map<String, String>) null, ContentType.APPLICATION_JSON, requestBody);

		CrmIResponse<String> classifierResponse = PythonUtil.sendPostRequest(1L, classifierRequest, false);
		if (classifierResponse.statusCode() == 200) {
			JsonObject jsonObject = GSON.fromJson(classifierResponse.data(), JsonObject.class);
			ArrayList<CommitmentSingleResponse> response = GSON.fromJson(jsonObject.get("response"),
					new TypeToken<ArrayList<CommitmentSingleResponse>>() {}.getType());
			for (CommitmentSingleResponse singleResponse : response) {
				commitmentOutput.put(singleResponse.getId(), singleResponse);
				LOGGER.info("CommitmentClassification:: ID :: " + singleResponse.getId() + "::isCommitmentPresent::" + singleResponse.getIsCommitmentPresent());

			}
			LOGGER.info("CommitmentClassification :: Time Taken For commitment Classifier Bulk ::: " + (System.currentTimeMillis() - start));
		} else {
			LOGGER.severe("CommitmentClassification :: Error when getting data from python server" + classifierResponse.data()); // No I18N
		}
		return new CommitmentClassificationResponse(commitmentOutput);
	}

}
