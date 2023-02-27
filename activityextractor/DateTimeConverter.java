//$Id$
package com.zoho.zia.crm.activityextractor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.language.english.model.DateGroup;
import com.zoho.hawking.language.english.model.DateRange;
import com.zoho.hawking.language.english.model.DatesFound;
import com.zoho.hawking.language.english.model.ParserOutput;
import com.zoho.hawking.language.english.model.RecognizerOutput;
import com.zoho.zia.utils.modelloaders.StandfordModelLoader;

public class DateTimeConverter {

    private static final Logger LOGGER = Logger.getLogger(DateTimeConverter.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //No I18N
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss"); //No I18N
    private static final SimpleDateFormat DATE_FORMAT_ZONE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"); //No I18N
    private static final DateTimeFormatter ZONE_FORMATTER = DateTimeFormat.forPattern("ZZ"); //No I18N

    public static DatesFound dateTimeEntity(DatesFound dateEntity) {

        List<DateGroup> datesGroup = dateEntity.getDateGroups();
        List<ParserOutput> datesFound = dateEntity.getParserOutputs();
        if (datesGroup.size() == 2) {
            int ret = exactTimeFinder(dateEntity);
            if (ret == 1 || ret == 2) {
                try {
                    int temp = (ret == 1) ? 1 : 0;
                    int flag = (temp == 1) ? 0 : 1;
                    Long startDateInBody = getStartTimeInLong(datesFound, temp);
                    Long endDateInBody = getEndTimeInLong(datesFound, temp);
                    Date referenceDate;
                    if ((startDateInBody != null) && (endDateInBody != null) &&
                            (getDateDifference(datesFound, temp) == 1) &&
                            (endDateInBody - startDateInBody == 86399000) &&
                            (getTZDifference(datesFound) == 1)) {
                        referenceDate = new Date(endDateInBody);
                    } else {
                        referenceDate = startDateInBody != null ? new Date(startDateInBody) : new Date(endDateInBody);
                    }
                    return StandfordModelLoader.getParser().parse(datesFound.get(flag).getText(), referenceDate, new HawkingConfiguration(),"eng");//No I18N
                } catch (Exception e) {
                    LOGGER.info("DateTimeConverter :: Error occurred while merging dates found" + datesFound.get(1).getText());
                    return dateEntity;
                }
            } else {
                return dateEntity;
            }
        } else if (datesGroup.size() == 3) {
            int ret = exactTripleTimeFinder(dateEntity);
            if (ret == 1 || ret == 2) {
                int flagOne = (ret == 1) ? 0 : 2;
                int flagTwo = (ret == 1) ? 1 : 0;
                int flagThree = (ret == 1) ? 2 : 1;
                Date dateOne;
                Date dateOneStart;
                Date dateOneEnd;
                try {
                    dateOne = dateFormatterReturn(dateEntity, DATE_FORMAT, flagOne);
                    dateOneStart = dateFormatterReturn(dateEntity, TIME_FORMAT, flagTwo);
                    dateOneEnd = dateFormatterReturn(dateEntity, TIME_FORMAT, flagThree);
                    String[] d1 = DATE_FORMAT.format(dateOne).split(" ");
                    String[] d2 = DATE_FORMAT.format(dateOneStart).split(" ");
                    String[] d3 = DATE_FORMAT.format(dateOneEnd).split(" ");
                    String dateReturnStart = d1[0] + " " + d2[1];
                    String dateReturnEnd = d1[0] + " " + d3[1];
                    Date dateOneFinalStart = DATE_FORMAT.parse(dateReturnStart);
                    Date dateOneFinalEnd = DATE_FORMAT.parse(dateReturnEnd);

                    DateTime dateTimeOne = new DateTime(dateOneFinalStart);
                    DateTime dateTimeTwo = new DateTime(dateOneFinalEnd);

                    List<DateGroup> datesGroupFinal = new ArrayList<>();
                    List<ParserOutput> datesFoundFinal = new ArrayList<>();
                    List<RecognizerOutput> recognizerOutputs = new ArrayList<>();
                    recognizerOutputs.addAll(datesFound.get(flagOne).getRecognizerOutputs());
                    recognizerOutputs.addAll(datesFound.get(flagTwo).getRecognizerOutputs());
                    recognizerOutputs.addAll(datesFound.get(flagThree).getRecognizerOutputs());
                    DateRange dateRange = new DateRange(datesFound.get(flagOne).getDateRange().getMatchType(),
                            dateTimeOne, dateTimeTwo, datesFound.get(flagOne).getDateRange().getStartDateFormat(),
                            datesFound.get(flagOne).getDateRange().getEndDateFormat());
                    ParserOutput parserOut = new ParserOutput(datesFound.get(flagOne).getId(),
                            dateRange,
                            datesFound.get(flagOne).getParserLabel(),
                            datesFound.get(flagOne).getParserStartIndex(),
                            (datesFound.get(flagOne).getText() + " " + datesFound.get(flagTwo).getText() //No I18N
                                    + " to " + datesFound.get(flagOne).getText() + " " + datesFound.get(flagThree).getText()), //No I18N
                            datesFound.get(flagOne).getIsTimeZonePresent(),
                            datesFound.get(flagOne).getIsExactTimePresent(),
                            datesFound.get(flagOne).getTimezoneOffset(),
                            datesFound.get(flagOne).getParserEndIndex(),
                            recognizerOutputs);
                    datesFoundFinal.add(0, parserOut);
                    datesGroupFinal.add(0, datesGroup.get(flagOne));
                    DatesFound dateReturn = new DatesFound();
                    dateReturn.setDateGroups(datesGroupFinal);
                    dateReturn.setParserOutputs(datesFoundFinal);
                    return dateReturn;
                } catch (ParseException e) {
                    LOGGER.info("DateTimeConverter :: Error occurred while parsing dates " + datesFound.get(1).getText());
                    return dateEntity;
                }
            } else {
                return dateEntity;
            }
        } else {
            return dateEntity;
        }
    }

    private static int exactTimeFinder(DatesFound dateEntity) {

        int ret;
        List<RecognizerOutput> dateListOne = dateEntity
                .getParserOutputs()
                .get(0)
                .getRecognizerOutputs();
        List<RecognizerOutput> dateListTwo = dateEntity
                .getParserOutputs()
                .get(1)
                .getRecognizerOutputs();

        List<String> labelListOne = dateListOne.stream().map(RecognizerOutput::getRecognizerLabel).collect(Collectors.toList());
        List<String> labelListTwo = dateListTwo.stream().map(RecognizerOutput::getRecognizerLabel).collect(Collectors.toList());
        if ((labelListOne.contains("exact_time")) && (!(labelListTwo.contains("exact_time")))) {
            ret = 1;
        } else if ((labelListTwo.contains("exact_time")) && (!(labelListOne.contains("exact_time")))) {
            ret = 2;
        } else {
            ret = 0;
        }
        return ret;
    }

    private static int exactTripleTimeFinder(DatesFound dateEntity) {

        int ret;
        List<RecognizerOutput> dateListOne = dateEntity
                .getParserOutputs()
                .get(0)
                .getRecognizerOutputs();
        List<RecognizerOutput> dateListTwo = dateEntity
                .getParserOutputs()
                .get(1)
                .getRecognizerOutputs();
        List<RecognizerOutput> dateListThree = dateEntity
                .getParserOutputs()
                .get(2)
                .getRecognizerOutputs();

        List<String> labelListOne = dateListOne.stream().map(RecognizerOutput::getRecognizerLabel).collect(Collectors.toList());
        List<String> labelListTwo = dateListTwo.stream().map(RecognizerOutput::getRecognizerLabel).collect(Collectors.toList());
        List<String> labelListThree = dateListThree.stream().map(RecognizerOutput::getRecognizerLabel).collect(Collectors.toList());
        if ((!(labelListOne.contains("exact_time"))) && (labelListTwo.contains("exact_time"))
                && (labelListThree.contains("exact_time"))) { //No I18N
            ret = 1;
        } else if ((labelListOne.contains("exact_time")) && (labelListTwo.contains("exact_time"))
                && (!(labelListThree.contains("exact_time")))) { //No I18N
            ret = 2;
        } else {
            ret = 0;
        }
        return ret;
    }


    public static Date dateFormatterReturn(DatesFound dateEntity, SimpleDateFormat formatDate, int flag) throws ParseException {
        List<ParserOutput> datesFound = dateEntity.getParserOutputs();
        Date currentDateStart = getStartTime(datesFound, flag);
        Date currentDateEnd = getEndTime(datesFound, flag);
        String startTimeFormat = null;
        if (currentDateStart != null) {
            startTimeFormat = formatDate.format(currentDateStart);
        } else if (currentDateEnd != null) {
            startTimeFormat = formatDate.format(currentDateEnd);
        }
        return formatDate.parse(startTimeFormat);
    }

    public static Date getStartTime(List<ParserOutput> datesFound, int index) {
        DateTime startDate = datesFound.get(index).getDateRange().getStart();
        return (startDate != null) ? new Date(startDate.getMillis()) : null;
    }

    public static Date getEndTime(List<ParserOutput> datesFound, int index) {
        DateTime endDate = datesFound.get(index).getDateRange().getEnd();
        return (endDate != null) ? new Date(endDate.getMillis()) : null;
    }

    public static Long getStartTimeInLong(List<ParserOutput> datesFound, int index) {
        DateTime startDate = datesFound.get(index).getDateRange().getStart();
        return (startDate != null) ? startDate.getMillis() : null;
    }

    public static Long getEndTimeInLong(List<ParserOutput> datesFound, int index) {
        DateTime endDate = datesFound.get(index).getDateRange().getEnd();
        return (endDate != null) ? endDate.getMillis() : null;
    }

    public static String getStartTimeFormat(List<ParserOutput> datesFound, int index) {
        return datesFound.get(index).getDateRange().getStartDateFormat();
    }

    public static String getEndTimeFormat(List<ParserOutput> datesFound, int index) {
        return datesFound.get(index).getDateRange().getEndDateFormat();
    }

    private static int getDateDifference(List<ParserOutput> datesFound, int index) {
        int startDate = datesFound.get(index).getDateRange().getStart().getDayOfMonth();
        int endDate = datesFound.get(index).getDateRange().getEnd().getDayOfMonth();
        return (startDate - endDate) == 0 ? 0 : 1;
    }

    private static int getTZDifference(List<ParserOutput> datesFound) throws ParseException {
        String userTZ = datesFound.get(0).getTimezoneOffset();
        String serverTZZoneID = datesFound.get(0).getDateRange().getStart().getZone().toString();
        DateTimeZone zone = DateTimeZone.forID(serverTZZoneID);
        String serverTZ = ZONE_FORMATTER.withZone(zone).print(0);
        String referenceDateStringUTZ = "2020-06-01T00:00:00.000" + userTZ;  //No I18N
        String referenceDateStringSTZ = "2020-06-01T00:00:00.000" + serverTZ;  //No I18N
        Date dateUTZ = DATE_FORMAT_ZONE.parse(referenceDateStringUTZ);
        Date dateSTZ = DATE_FORMAT_ZONE.parse(referenceDateStringSTZ);
        DateTime userTZDate = new DateTime(dateUTZ);
        DateTime serverTZDate = new DateTime(dateSTZ);
        if (serverTZDate.isAfter(userTZDate.getMillis())) {
            return 1;
        } else {
            return 0;
        }
    }
}
