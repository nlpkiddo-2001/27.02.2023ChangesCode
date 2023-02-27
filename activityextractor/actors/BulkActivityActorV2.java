//$Id$
package com.zoho.zia.crm.activityextractor.actors;

import java.util.ArrayList;
import java.util.List;

import com.zoho.zia.api.textservice.response.TextServiceResponse;
import com.zoho.zia.api.textservice.response.v1.Features;
import com.zoho.zia.web.model.activity.ActivityBulkInputV2;
import com.zoho.zia.web.model.activity.ExtractorBulkInputV2;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class BulkActivityActorV2 extends AbstractActor {

  private final List<TextServiceResponse> textServiceResponses = new ArrayList<>();
  private ActorRef mySender;
  private int counter = 0;
  private int inputSize = 0;
  private final Features features = new Features();

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ActivityBulkInputV2.class, activityBulkInput -> {
          mySender = getSender();
          inputSize = activityBulkInput.getExtractorBulkInput().size();
          for (ExtractorBulkInputV2 extractorInput : activityBulkInput.getExtractorBulkInput()) {
            ActorRef activityActorSingle = getContext().actorOf(Props.create(ParserActorV2.class));
            activityActorSingle.tell(extractorInput, getSelf());
          }
        })

        .match(TextServiceResponse.class, textServiceResponse -> {
          textServiceResponses.add(textServiceResponse);
          counter += 1;
          if (counter == inputSize) {
            features.setFeatures(textServiceResponses);
            mySender.tell(features, getSelf());
            getContext().stop(getSelf());
          }
        })
        .build();
  }

}
