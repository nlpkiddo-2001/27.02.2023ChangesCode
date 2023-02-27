package com.zoho.zia.crm.activityextractor.actors;

import akka.actor.AbstractActor;
import com.zoho.zia.api.textservice.response.TextServiceResponse;
import com.zoho.zia.crm.activityextractor.ActivityParser;
import com.zoho.zia.web.model.activity.ExtractorBulkInput;

public class ParserActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ExtractorBulkInput.class, extractorBulkInput -> {
                    TextServiceResponse textServiceResponse = ActivityParser.parseBulkActivity(extractorBulkInput);
                    getSender().tell(textServiceResponse, getSelf());
                    getContext().stop(getSelf());
                })

                .build();
    }
}
