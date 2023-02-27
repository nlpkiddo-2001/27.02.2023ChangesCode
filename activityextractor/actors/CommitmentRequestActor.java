//$Id$
package com.zoho.zia.crm.activityextractor.actors;

import akka.actor.AbstractActor;
import com.zoho.zia.crm.activityextractor.classify.ActivityClassifier;
import com.zoho.zia.crm.activityextractor.actors.PythonRequestMessages.*;
import com.zoho.zia.web.model.python.PythonRequest;

import java.util.List;

public class CommitmentRequestActor extends AbstractActor {

  @Override
  public AbstractActor.Receive createReceive() {
    return receiveBuilder()
        .match(ClassificationRequestInput.class, pythonRequest -> {
          List<PythonRequest> messages = pythonRequest.getRequestMessages();
          CommitmentClassificationResponse commitmentOutput = ActivityClassifier.predictCommitment(messages);
          getSender().tell(commitmentOutput, getSelf());
          getContext().stop(getSelf());
        })
        .build();
  }
}
