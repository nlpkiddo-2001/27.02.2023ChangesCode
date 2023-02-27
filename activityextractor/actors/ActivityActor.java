//$Id$
package com.zoho.zia.crm.activityextractor.actors;

import akka.actor.AbstractActor;
import com.zoho.hawking.language.english.model.DatesFound;
import com.zoho.zia.crm.activityextractor.ExtractorUtils;
import com.zoho.zia.crm.hawking.HawkingMessages.HawkingInputSingle;
import com.zoho.zia.web.model.activity.NEREntities;
import com.zoho.zia.web.service.HawkingService;
import org.spark_project.jetty.util.log.LoggerLog;
import java.util.logging.Logger;


public class ActivityActor extends AbstractActor {

    private static final Logger LOGGER = Logger.getLogger(PrimaryActivityActor.class.getName());
    @Override
    public Receive createReceive() {
        LOGGER.info("######################Activity Actor######################");
        return receiveBuilder()
//                .match(HawkingInputSingle.class, hawkingText -> {
//                    DatesFound datesFound = HawkingService.getDates(hawkingText);
//                    getSender().tell(datesFound, getSelf());
//                    getContext().stop(getSelf());
//                })
                .match(String.class, mailBody -> {
                    NEREntities nerEntities = ExtractorUtils.NEREntitiesExtractor(mailBody);
                    getSender().tell(nerEntities, getSelf());
                    getContext().stop(getSelf());
                })
                .build();
    }
}
