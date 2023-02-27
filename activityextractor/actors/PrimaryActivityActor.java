//$Id$
package com.zoho.zia.crm.activityextractor.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.zoho.hawking.language.english.model.DatesFound;
import com.zoho.zia.crm.activityextractor.actors.ActivityMessages.ActivityActorRequest;
import com.zoho.zia.crm.activityextractor.actors.ActivityMessages.ActivityActorResponse;
import com.zoho.zia.web.model.activity.NEREntities;

import java.util.logging.Logger;

public class PrimaryActivityActor extends AbstractActor {

    private static final Logger LOGGER = Logger.getLogger(PrimaryActivityActor.class.getName());
    private DatesFound dateTimeEntity;
    private NEREntities nerEntities;
    private int counter = 0;
    private ActorRef mySender;

    @Override
    public Receive createReceive() {


        LOGGER.info("########################Primary actor#########################");
        return receiveBuilder()
                .match(ActivityActorRequest.class, activityActorRequest -> {
                    mySender = getSender();

                    ActorRef dateEntityActor = getContext().actorOf(Props.create(ActivityActor.class));
                    LOGGER.info("########################Location entity actor#########################");
                    ActorRef locationEntityActor = getContext().actorOf(Props.create(ActivityActor.class));

                    LOGGER.info("MainActivityActors :: Calling location entity actor");
                    dateEntityActor.tell(activityActorRequest.getHawkingInputSingle(), getSelf());
                    locationEntityActor.tell(activityActorRequest.getMailBody(), getSelf());
                })

                .match(DatesFound.class, dateTimeEntity -> {
                    this.dateTimeEntity = dateTimeEntity;
                    counter += 1;
                    if (counter == 2) {
                        mySender.tell(new ActivityActorResponse(dateTimeEntity, nerEntities), getSelf());
                        getContext().stop(getSelf());
                    }
                })
                .match(NEREntities.class, nerEntities -> {
                    this.nerEntities = nerEntities;
                    counter += 1;
                    if (counter == 2) {
                        mySender.tell(new ActivityActorResponse(dateTimeEntity, nerEntities), getSelf());
                        getContext().stop(getSelf());
                    }
                })
                .build();
    }

}
