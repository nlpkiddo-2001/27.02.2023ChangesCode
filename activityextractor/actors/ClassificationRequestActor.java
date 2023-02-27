//$Id$
package com.zoho.zia.crm.activityextractor.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.logging.Logger;
import com.zoho.zia.crm.activityextractor.actors.PythonRequestMessages.*;

public class ClassificationRequestActor extends AbstractActor {

  private static final Logger LOGGER = Logger.getLogger(ClassificationRequestActor.class.getName());
  private ActorRef mySender;
  private CommitmentClassificationResponse commitmentResponse;
  private ActivityClassificationResponse activityResponse;
  private int counter = 0;

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(PythonRequestActorInput.class, pythonActorRequest -> {
          mySender = getSender();
          ActorRef ActivityActor = getContext().actorOf(Props.create(ActivityV2RequestActor.class));
          ActorRef CommitmentActor = getContext().actorOf(Props.create(CommitmentRequestActor.class));

          LOGGER.info("MainActivityActors :: Calling commitment and activity prediction Actors");
          ActivityActor.tell(pythonActorRequest.getActivityActorRequestMessages(), getSelf());
          CommitmentActor.tell(pythonActorRequest.getCommitmentRequestMessages(), getSelf());
        })

        .match(CommitmentClassificationResponse.class, commitmentResponse -> {
          this.commitmentResponse = commitmentResponse;
          counter += 1;
          if (counter == 2) {
            mySender.tell(new PythonRequestActorResponse(activityResponse, commitmentResponse),
                getSelf());
            getContext().stop(getSelf());
          }
        })

        .match(ActivityClassificationResponse.class, activityResponse -> {
          this.activityResponse = activityResponse;
          counter += 1;
          if (counter == 2) {
            mySender.tell(new PythonRequestActorResponse(activityResponse, commitmentResponse),
                getSelf());
            getContext().stop(getSelf());
          }
        })
        .build();
  }
}