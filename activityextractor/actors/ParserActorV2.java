//$Id$
package com.zoho.zia.crm.activityextractor.actors;

import com.zoho.zia.api.textservice.response.TextServiceResponse;
import com.zoho.zia.crm.activityextractor.ActivityParserV2;
import com.zoho.zia.web.model.activity.ExtractorBulkInputV2;

import akka.actor.AbstractActor;

public class ParserActorV2 extends AbstractActor {

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ExtractorBulkInputV2.class, extractorBulkInput -> {
          TextServiceResponse textServiceResponse = ActivityParserV2.parseBulkActivityV2(extractorBulkInput);
          getSender().tell(textServiceResponse, getSelf());
          getContext().stop(getSelf());
        })

        .build();
  }

}
