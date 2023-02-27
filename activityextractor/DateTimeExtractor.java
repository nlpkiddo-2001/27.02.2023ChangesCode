//$Id$
package com.zoho.zia.crm.activityextractor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.zoho.hawking.language.english.model.DateGroup;
import com.zoho.hawking.language.english.model.DatesFound;
import com.zoho.hawking.language.english.model.ParserOutput;
import com.zoho.zia.web.model.activity.DateTimeEntity;
import com.zoho.zia.web.model.activity.ExtractorInputBody;

public class DateTimeExtractor {

    private static final Logger LOGGER = Logger.getLogger(DateTimeExtractor.class.getName());
    static SimpleDateFormat ZONED_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss z");  //No I18N
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //No I18N

    public static DateTimeEntity dateTimeEntity(ExtractorInputBody entityText, DatesFound dates) {
        long start = System.currentTimeMillis();
        LOGGER.info( "DateTimeExtractor :: Input Request Timestamp :: "+entityText.getDateString());
        LOGGER.info( "DateTimeExtractor :: Input Request TimeZone :: "+entityText.getTimeZone());
        LOGGER.info( "DateTimeExtractor :: Machine TimeZone :: "+TimeZone.getDefault().getDisplayName());

        //convert date to user time zone
        TimeZone userTimeZone = TimeZone.getTimeZone(entityText.getTimeZone());
        DateTimeEntity datetime = new DateTimeEntity();
        List<String> dateText = new ArrayList<>();
        DatesFound datesFound;
        try {
            //removing data component If it has no start and end date.
            datesFound = dates != null ? emptyDatesRemover(dates) : new DatesFound();
            // check if input has at least one valid date time component.
            if (datesFound.getDateGroups() != null && datesFound.getDateGroups().size() > 0) {
                datesFound = DateTimeConverter.dateTimeEntity(datesFound);
                datesFound = removePastDatetime(datesFound, userTimeZone);
                List<DateGroup> dateGroups = datesFound != null ? datesFound.getDateGroups() : new ArrayList<>();
                if (dateGroups.size() > 0) {
                    LOGGER.info("DateTimeExtractor :: Future date  is Present");
                    datesFound = closestDatetime(datesFound);
                    List<ParserOutput> parserOut = datesFound.getParserOutputs();
                    for (ParserOutput parserOutput : parserOut) {
                        dateText.add(parserOutput.getText());
                    }
                    datetime.setDatetext(dateText);
                    datetime.setDates(datesFound);
                } else {
                    LOGGER.info("DateTimeExtractor :: No Future date Present");
                    datetime.setDates(null);
                    datetime.setOnlyPastEvent(true);
                }
                datetime.setDatePresent(true);
            } else {
                LOGGER.info("DateTimeExtractor :: No dates are Present");
                datetime.setDates(null);
                datetime.setOnlyPastEvent(false);
                datetime.setDatePresent(false);
            }
        } catch (Exception e) {
            datetime.setDates(null);
            datetime.setDatePresent(true);
            datetime.setOnlyPastEvent(false);
            LOGGER.log(Level.SEVERE, "DateTimeExtractor :: Exception in getting date time", e.getMessage());
            LOGGER.log(Level.SEVERE, ExceptionUtils.getStackTrace(e));
        }
        LOGGER.info("DateTimeExtractor :: Is Date Present" + datetime.getDatePresent());
        LOGGER.info("DateTimeExtractor :: Time Taken for Date and Time Total " + (System.currentTimeMillis() - start));
        return datetime;

    }

    private static DatesFound emptyDatesRemover(DatesFound dates) {
        List<DateGroup> dateGroups = dates.getDateGroups();
        List<ParserOutput> parserOutputs = dates.getParserOutputs();
        List<DateGroup> dateGroup = new ArrayList<>();
        List<ParserOutput> parserOutput = new ArrayList<>();
        DatesFound date = new DatesFound();
        for (int i = 0; i < dateGroups.size(); i++) {
            Long startDateInBody = DateTimeConverter.getStartTimeInLong(parserOutputs, i);
            Long endDateInBody = DateTimeConverter.getEndTimeInLong(parserOutputs, i);
            if (startDateInBody != null || endDateInBody != null) {
                dateGroup.add(dates.getDateGroups().get(i));
                parserOutput.add(parserOutputs.get(i));
            } else {
                LOGGER.info("DateTimeExtractor :: Unparsed date is present");
            }
        }
        date.setDateGroups(dateGroup);
        date.setParserOutputs(parserOutput);
        return date;
    }

    private static DatesFound closestDatetime(DatesFound dates) throws ParseException {

        TreeMap<Date, Integer> dateMap = new TreeMap<>();
        List<DateGroup> dateGroups = dates.getDateGroups();
        List<ParserOutput> parserOutputs = dates.getParserOutputs();
        List<DateGroup> dateGroup = new ArrayList<>();
        List<ParserOutput> parserOutput = new ArrayList<>();
        DatesFound date = new DatesFound();

        for (int i = 0; i < dateGroups.size(); i++) {
            Long startDateInBody = DateTimeConverter.getStartTimeInLong(parserOutputs, i);
            Long endDateInBody = DateTimeConverter.getEndTimeInLong(parserOutputs, i);
            Date checkDate;
            if (startDateInBody != null) {
                checkDate = DATE_FORMAT.parse(DATE_FORMAT.format(new Date(startDateInBody)));
            } else if (endDateInBody != null) {
                checkDate = DATE_FORMAT.parse(DATE_FORMAT.format(new Date(endDateInBody)));
            } else {
                checkDate = null;
            }
            dateMap.put(checkDate, i);
        }
        int key = dateMap.firstEntry().getValue();
        dateGroup.add(dateGroups.get(key));
        parserOutput.add(parserOutputs.get(key));
        date.setDateGroups(dateGroup);
        date.setParserOutputs(parserOutput);
        LOGGER.info("DateTimeExtractor :: closest date time over");
        return date;
    }

    private static DatesFound removePastDatetime(DatesFound dates, TimeZone userTimeZone) throws ParseException {
        List<ParserOutput> parserOutputs = dates.getParserOutputs();
        List<DateGroup> dateGroup = new ArrayList<>();
        List<ParserOutput> parserOutput = new ArrayList<>();
        DatesFound date = new DatesFound();
        int dateSize = dates.getDateGroups().size();
        ZONED_DATE_FORMAT.setTimeZone(userTimeZone);
        if (dateSize == 0) {
            return null;
        }

        Date currentTime = DATE_FORMAT.parse(ZONED_DATE_FORMAT.format(new Date()));
        for (int i = 0; i < dateSize; i++) {
            Long startDateInBody = DateTimeConverter.getStartTimeInLong(parserOutputs, i);
            Long endDateInBody = DateTimeConverter.getEndTimeInLong(parserOutputs, i);
            Date checkDate = null;
            if (startDateInBody != null && !(parserOutputs.get(i).getText().toLowerCase().equals("today"))) {
                checkDate = DATE_FORMAT.parse(DATE_FORMAT.format(new Date(startDateInBody)));
            } else if (endDateInBody != null) {
                checkDate = DATE_FORMAT.parse(DATE_FORMAT.format(new Date(endDateInBody)));
            }
            if ((checkDate != null) && (checkDate.after(currentTime))) {
                dateGroup.add(dates.getDateGroups().get(i));
                parserOutput.add(parserOutputs.get(i));
            }
        }
        date.setDateGroups(dateGroup);
        date.setParserOutputs(parserOutput);
        LOGGER.info("DateTimeExtractor :: Past date time over");
        return date;
    }

}
