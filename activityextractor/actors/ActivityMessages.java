package com.zoho.zia.crm.activityextractor.actors;

import akka.actor.NoSerializationVerificationNeeded;
import com.zoho.hawking.language.english.model.DatesFound;
import com.zoho.zia.crm.hawking.HawkingMessages.HawkingInputSingle;
import com.zoho.zia.web.model.activity.NEREntities;

public interface ActivityMessages {

    class ActivityActorRequest implements NoSerializationVerificationNeeded {

        private HawkingInputSingle hawkingInputSingle;
        private String mailBody;

        public ActivityActorRequest() {
        }

        public ActivityActorRequest(HawkingInputSingle hawkingInputSingle, String mailBody) {
            this.hawkingInputSingle = hawkingInputSingle;
            this.mailBody = mailBody;
        }

        public HawkingInputSingle getHawkingInputSingle() {
            return hawkingInputSingle;
        }

        public void setHawkingInputSingle(HawkingInputSingle hawkingInputSingle) {
            this.hawkingInputSingle = hawkingInputSingle;
        }

        public String getMailBody() {
            return mailBody;
        }

        public void setMailBody(String mailBody) {
            this.mailBody = mailBody;
        }
    }

    class ActivityActorResponse implements NoSerializationVerificationNeeded {

        private DatesFound dateTimeEntity;
        private NEREntities nerEntities;

        public ActivityActorResponse() {
        }

        public ActivityActorResponse(DatesFound dateTimeEntity, NEREntities nerEntities) {
            this.dateTimeEntity = dateTimeEntity;
            this.nerEntities = nerEntities;
        }

        public DatesFound getDateTimeEntity() {
            return dateTimeEntity;
        }

        public void setDateTimeEntity(DatesFound dateTimeEntity) {
            this.dateTimeEntity = dateTimeEntity;
        }

        public NEREntities getNerEntities() {
            return nerEntities;
        }

        public void setNerEntities(NEREntities nerEntities) {
            this.nerEntities = nerEntities;
        }
    }

}
