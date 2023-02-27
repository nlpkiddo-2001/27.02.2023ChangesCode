//$Id$
package com.zoho.zia.crm.activityextractor.actors;

import akka.actor.AbstractActor;
import com.zoho.zia.crm.activityextractor.classify.ActivityClassifier;
import com.zoho.zia.crm.activityextractor.actors.PythonRequestMessages.*;
import com.zoho.zia.web.model.python.PythonRequest;

import java.util.List;

public class ActivityV2RequestActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ClassificationRequestInput.class, pythonRequest -> {
                    List<PythonRequest> messages = pythonRequest.getRequestMessages();
                    ActivityClassificationResponse activityOutput = ActivityClassifier.predictActivityV2(messages);
                    getSender().tell(activityOutput, getSelf());
                    getContext().stop(getSelf());
                })
                .build();
    }
}
