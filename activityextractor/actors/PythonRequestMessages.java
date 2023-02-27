//$Id$
package com.zoho.zia.crm.activityextractor.actors;

import akka.actor.NoSerializationVerificationNeeded;
import com.zoho.zia.web.model.activity.ActivityPythonSingleResponse;
import com.zoho.zia.web.model.activity.CommitmentSingleResponse;
import com.zoho.zia.web.model.python.PythonRequest;

import java.util.List;
import java.util.Map;

public interface PythonRequestMessages {

    class PythonRequestActorInput implements NoSerializationVerificationNeeded {

        private ClassificationRequestInput activityActorRequestMessages;
        private ClassificationRequestInput commitmentRequestMessages;

        public PythonRequestActorInput() {
        }

        public PythonRequestActorInput(ClassificationRequestInput activityActorRequestMessages, ClassificationRequestInput commitmentRequestMessages) {
            this.activityActorRequestMessages = activityActorRequestMessages;
            this.commitmentRequestMessages = commitmentRequestMessages;
        }


        public ClassificationRequestInput getActivityActorRequestMessages() {
            return activityActorRequestMessages;
        }

        public void setActivityActorRequestMessages(ClassificationRequestInput activityActorRequestMessages) {
            this.activityActorRequestMessages = activityActorRequestMessages;
        }

        public ClassificationRequestInput getCommitmentRequestMessages() {
            return commitmentRequestMessages;
        }

        public void setCommitmentRequestMessages(ClassificationRequestInput commitmentRequestMessages) {
            this.commitmentRequestMessages = commitmentRequestMessages;
        }
    }


    class ClassificationRequestInput implements NoSerializationVerificationNeeded{
        private List<PythonRequest> requestMessages;
        public ClassificationRequestInput() {
        }

        public ClassificationRequestInput(List<PythonRequest> requestMessages) {
            this.requestMessages = requestMessages;
        }

        public List<PythonRequest> getRequestMessages() {
            return requestMessages;
        }

        public void setRequestMessages(List<PythonRequest> requestMessages) {
            this.requestMessages = requestMessages;
        }
    }

    class ActivityClassificationResponse implements NoSerializationVerificationNeeded{
        private Map<String, ActivityPythonSingleResponse> response;

        public ActivityClassificationResponse(Map<String, ActivityPythonSingleResponse> response){
            this.response = response;
        }

        public Map<String, ActivityPythonSingleResponse> getResponse() {
            return response;
        }

        public void setResponse(Map<String, ActivityPythonSingleResponse> response) {
            this.response = response;
        }
    }

    class CommitmentClassificationResponse implements NoSerializationVerificationNeeded{
        private Map<String, CommitmentSingleResponse> response;

        public CommitmentClassificationResponse(Map<String, CommitmentSingleResponse> response){
            this.response = response;
        }

        public Map<String, CommitmentSingleResponse> getResponse() {
            return response;
        }

        public void setResponse(Map<String, CommitmentSingleResponse> response) {
            this.response = response;
        }
    }

    class PythonRequestActorResponse implements NoSerializationVerificationNeeded {

        private ActivityClassificationResponse activityResponse;
        private CommitmentClassificationResponse commitmentResponse;

        public PythonRequestActorResponse() {
        }

        public PythonRequestActorResponse(ActivityClassificationResponse activityResponse, CommitmentClassificationResponse commitmentResponse) {
            this.activityResponse = activityResponse;
            this.commitmentResponse = commitmentResponse;
        }

        public ActivityClassificationResponse getActivityResponse() {
            return activityResponse;
        }

        public void setActivityResponse(ActivityClassificationResponse activityResponse) {
            this.activityResponse = activityResponse;
        }

        public CommitmentClassificationResponse getCommitmentResponse() {
            return commitmentResponse;
        }

        public void setCommitmentResponse(CommitmentClassificationResponse commitmentResponse) {
            this.commitmentResponse = commitmentResponse;
        }
    }

}
