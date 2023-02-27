//$Id$
package com.zoho.zia.crm.activityextractor;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.zoho.zia.utils.modelloaders.StandfordModelLoader;
import com.zoho.zia.web.model.activity.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.TypesafeMap;
import org.apache.commons.text.StringEscapeUtils;

import com.zoho.crm.feature.entityparser.util.patterns.PhoneNoExtraction;
import com.zoho.crm.feature.summarizer.TextProcessing;
import com.zoho.crm.utils.DetectHTML;
import com.zoho.crm.utils.EmailThreads;
import com.zoho.zia.api.textservice.request.EmailExtractionRequest;
import com.zoho.zia.api.textservice.request.TextServiceInput;
import com.zoho.zia.crm.activityextractor.utils.ActionWordsExtractor;
import com.zoho.zia.crm.activityextractor.utils.TitleWordsExtractor;
import com.zoho.zia.utils.Constants;
import com.zoho.zia.utils.preprocessing.MailPreProcess;
import com.zoho.zia.utils.preprocessing.RegexBuilder;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.Triple;
import org.json.JSONObject;

import static com.zoho.zia.utils.modelloaders.StandfordModelLoader.getPipelineForPos;

public class ExtractorUtils {

    private static final Logger LOGGER = Logger.getLogger(ExtractorUtils.class.getName());
    private static final PhoneNumberUtil PHONE_NUMBER_INSTANCE = PhoneNumberUtil.getInstance();
    private static final PhoneNumberOfflineGeocoder GEOCODER_INSTANCE = PhoneNumberOfflineGeocoder.getInstance();
    private static final PhoneNumberToCarrierMapper CARRIER_INSTANCE = PhoneNumberToCarrierMapper.getInstance();

    public static String titleExtractor(String subject) {
        long start = System.currentTimeMillis();
        List<Triple<String, Integer, Integer>> titleWords = TitleWordsExtractor.parse(subject);
        int num = titleWords.size();
        int iterator = 0;
        String matchedWord;
        String titleHead = "";
        while (num != 0) {
            Integer startIndex = titleWords.get(iterator).second;
            Integer endIndex = titleWords.get(iterator).third;
            matchedWord = subject.substring(startIndex, endIndex);
            titleHead = titleHead.concat(" " + matchedWord);
            iterator++;
            num--;
        }
        titleHead = Arrays.stream(titleHead.split(" ")).distinct().collect(Collectors.joining(" "));
        LOGGER.info("ExtractorUtils :: Time Taken For Activity Title" + (System.currentTimeMillis() - start));
        return titleHead.trim();
    }

    public static String actionExtractor(String bodyContent) {
        long start = System.currentTimeMillis();
        List<Triple<String, Integer, Integer>> actionWords = ActionWordsExtractor.parse(bodyContent);
        int num = actionWords.size();
        int iterator = 0;
        String matchedWord = null;
        while (num != 0) {
            Integer startIndex = actionWords.get(iterator).second;
            Integer endIndex = actionWords.get(iterator).third;
            matchedWord = bodyContent.substring(startIndex, endIndex);
            iterator++;
            num--;
        }
        String actionHead = matchedWord;
        LOGGER.info("ExtractorUtils :: Time Taken For Activity Action " + (System.currentTimeMillis() - start));
        return actionHead;
    }

    public static List<ContactValidationResponse> validateContact(List<String> contactInfo) {
        List<ContactValidationResponse> validateResponse = new ArrayList<ContactValidationResponse>();
        for (String contact_number : contactInfo) {
            Phonenumber.PhoneNumber phoneNumber;
            if (PHONE_NUMBER_INSTANCE.isPossibleNumber(contact_number, "ZZ")) {
                try {
                    phoneNumber = PHONE_NUMBER_INSTANCE.parse(contact_number, "ZZ"); // no i18n
                } catch (NumberParseException ignored) {
                    validateResponse.add(new ContactValidationResponse("undefined", contact_number)); // no i18n
                    continue;
                }
                if (PHONE_NUMBER_INSTANCE.isValidNumber(phoneNumber)) {
                    validateResponse.add(new ContactValidationResponse(
                        "true", String.valueOf(phoneNumber.getNationalNumber()), String.valueOf(phoneNumber.getCountryCode()),
                         CARRIER_INSTANCE.getNameForNumber(phoneNumber, Locale.ENGLISH),
                         GEOCODER_INSTANCE.getDescriptionForNumber(phoneNumber, Locale.ENGLISH),
                        PHONE_NUMBER_INSTANCE.getNumberType(phoneNumber).toString()));
                } else {
                    validateResponse.add(new ContactValidationResponse("false", contact_number));
                }
            } else {
                validateResponse.add(new ContactValidationResponse("undefined", contact_number)); // no i18n
            }
        }
        return validateResponse;
    }

    public static List<String> contactExtractor(String bodyContent) {
        long start = System.currentTimeMillis();
        List<String> contactInfo = new ArrayList<>();
        Matcher mail = RegexBuilder.emailPatternAdvance.matcher(bodyContent);
        while (mail.find()) {
            contactInfo.add(mail.group(0));
        }
        String[] phoneNumbers = PhoneNoExtraction.getPhoneNo(bodyContent);
        if (phoneNumbers.length > 0) {
            contactInfo.addAll(Arrays.asList(phoneNumbers));
        }
        LOGGER.info("ExtractorUtils :: Time Taken For Contacts" + (System.currentTimeMillis() - start));
        contactInfo = contactInfo.stream().distinct().collect(Collectors.toList());
        return contactInfo;
    }

    public static List<ContactValidationResponse> contactExtractorV2(String bodyContent) {
        long start = System.currentTimeMillis();
        List<String> contactInfo = new ArrayList<>();
        Matcher mail = RegexBuilder.emailPatternAdvance.matcher(bodyContent);
        while (mail.find()) {
            contactInfo.add(mail.group(0));
        }
        String[] phoneNumbers = PhoneNoExtraction.getPhoneNo(bodyContent);
        if (phoneNumbers.length > 0) {
            contactInfo.addAll(Arrays.asList(phoneNumbers));
        }
        LOGGER.info("ExtractorUtils :: Time Taken For Contacts" + (System.currentTimeMillis() - start));
        contactInfo = contactInfo.stream().distinct().collect(Collectors.toList());
        return validateContact(contactInfo);
    }

    public static NEREntities NEREntitiesExtractor(String inputSentence) {
        LOGGER.info("############NER Entities Extractor####################");
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        LOGGER.info("before starting the NEREntities Extractor Memory is " + memoryBefore);
        long start = System.currentTimeMillis();

        // create doc from inputSentence
        Document doc = new Document(inputSentence);
        Sentence sent = (Sentence) doc.sentences();
        LOGGER.info("s############sentence is  " + sent);
        long end = System.currentTimeMillis() - start;
        LOGGER.info("total time taken for sentence instance creation is     " + end + "  ms");
        LOGGER.info("  NER Entities extractor next as loction and participants");
        List<String> location = sent.mentions("LOCATION"); //No I18N
        List<String> participants = sent.mentions("PERSON"); //No I18N


        LOGGER.info("##################location and participants list complete##############");
        LOGGER.info("ExtractorUtils :: Time Taken For Simple NLP to find ner " + (System.currentTimeMillis() - start));
        location = location.stream().distinct().collect(Collectors.toList());
        participants = participants.stream().distinct().collect(Collectors.toList());
        return new NEREntities(location, participants);
    }
    public static NEREntities NEREntitiesExtractor1(String inputSentence) {
        LOGGER.info("############NER Entities Extractor####################");
        long start = System.currentTimeMillis();
        LOGGER.info("###################before if#################");
        if (inputSentence == null) {
            throw new IllegalArgumentException("Input sentence cannot be null");
        }
        LOGGER.info("###############after if####################");
        StanfordCoreNLP stanfordCoreNLP = getPipelineForPos();
        LOGGER.info("after instance stanford");
        CoreDocument document = new CoreDocument(inputSentence);
        LOGGER.info("after document");
        stanfordCoreNLP.annotate(document);
        LOGGER.info("after annotating");

        List<CoreLabel> coreLabels = document.tokens();
        List<String> location = new ArrayList<>();
        List<String> participants = new ArrayList<>();
            for (CoreLabel token :coreLabels) {
                LOGGER.info("token is printing from sentence " + token);
                String namedEntity = token.getString(CoreAnnotations.NamedEntityTagAnnotation.class);
                LOGGER.info("printing named entity "  + namedEntity);
                LOGGER.info("############after string named entity###############");
                if (namedEntity.equals("LOCATION")) {
                    location.add(token.word());
                } else if (namedEntity.equals("PERSON")) {
                    participants.add(token.word());
                }
            }


        LOGGER.info("##################location and participants list complete##############");
        LOGGER.info("ExtractorUtils :: Time Taken For Simple NLP to find ner " + (System.currentTimeMillis() - start));
        location = location.stream().distinct().collect(Collectors.toList());
        participants = participants.stream().distinct().collect(Collectors.toList());
        return new NEREntities(location, participants);
    }




    public static ActivityResponse getActivityResponse(String activityType, String bodyContent, ActivityEntities activityEntities, String subject) {
        String activityTitle = ExtractorUtils.titleExtractor(subject);
        String activityAction = ExtractorUtils.actionExtractor(bodyContent);
        ActivityResponse activity = new ActivityResponse(activityType, activityTitle, activityAction, activityEntities.getActivityParticipants(),
                activityEntities.getContactInfo(), activityType.equals("Event") ? activityEntities.getLocation() : Collections.emptyList(),
                activityEntities.getDescription(), activityEntities.getDates(), activityEntities.getDatesPresent());
        return activity;
    }

    public static ActivitySingleOutput getActivityResponseV2(List<ActivityAttributes> activityAttributes, ExtractorInputBody extractorInputBody, ActivityV2Entities activityEntities, String subject) {
        HashMap<String, List<String>> activity_date_lookup = new HashMap<String, List<String>>();
        HashMap<String, List<Integer>> activity_date_id_lookup = new HashMap<String, List<Integer>>();
        Integer index = 0;

        String activityTitle = ExtractorUtils.titleExtractor(subject);
        List<String> location;

        List<ActivityOutputAttributes> activityOutput = new ArrayList<ActivityOutputAttributes>();
        for (ActivityAttributes singleActivity : activityAttributes) {
            String sentence = singleActivity.getSentence().replaceAll(Constants.MULTIPLE_SPACE, Constants.ONE_SPACE).trim();
            JSONObject dates = EntityUtils.extractDatesFromText(sentence, extractorInputBody.getDateString(), extractorInputBody.getTimeZone());

            if (dates != null) {
                String activityAction = ExtractorUtils.actionExtractor(sentence);
                if (singleActivity.getType().equals("Event")) {
                    location = new Sentence(sentence).mentions("LOCATION");//No I18N
                    location = location.stream().distinct().collect(Collectors.toList());
                } else {
                    location = Collections.emptyList();
                }

                List<String> date_as_string = activity_date_lookup.getOrDefault(dates.toString(), null);
                List<Integer> date_event_index = activity_date_id_lookup.getOrDefault(dates.toString(), null);
                if (date_as_string != null) {
                    if (!date_as_string.contains(singleActivity.getType())) {
                        date_as_string.add(singleActivity.getType());
                        date_event_index.add(index);
                        activityOutput.add(new ActivityOutputAttributes(new ArrayList<String>(Collections.singletonList(singleActivity.getSentence())),
                            singleActivity.getType(), activityAction, dates, !dates.isEmpty(), location));
                        index++;

                    } else {
                        Integer eventIndex = date_event_index.get(date_as_string.indexOf(singleActivity.getType()));
                        ActivityOutputAttributes previousActivitySentence = activityOutput.get(eventIndex);
                        previousActivitySentence.getSentence().add(singleActivity.getSentence());
                    }
                } else {
                    if (!dates.isEmpty()) {
                        activity_date_lookup.putIfAbsent(dates.toString(), new ArrayList<String>(Collections.singletonList(singleActivity.getType())));
                        activity_date_id_lookup.putIfAbsent(dates.toString(), new ArrayList<Integer>(Collections.singletonList(index)));
                    }
                    activityOutput.add(new ActivityOutputAttributes(new ArrayList<String>(Collections.singletonList(singleActivity.getSentence())),
                        singleActivity.getType(), activityAction, dates, !dates.isEmpty(), location));
                    index++;
                }
            }
        }

        boolean globalDateCheck = activityOutput.stream().anyMatch(ActivityOutputAttributes::getIsDatePresent);
        return new ActivitySingleOutput(activityTitle, activityEntities.getActivityParticipants(), activityEntities.getContactInfo(), activityOutput, activityEntities.getDescription(),
            globalDateCheck ? new JSONObject(JSONObject.NULL) : activityEntities.getDates(), !globalDateCheck && activityEntities.getDatesPresent());
    }

    public static String getProcessedMailBody(String bodyContent, String emailContentAsText){
        bodyContent = (bodyContent.length() > 1) ? bodyContent.replaceAll(Constants.NEW_LINE, Constants.ONE_SPACE)
            : emailContentAsText.replaceAll(Constants.NEW_LINE, Constants.ONE_SPACE);
        TextProcessing processedMail = new TextProcessing(bodyContent);
        bodyContent = (String.join((CharSequence) " ",  processedMail.filteredSentence())).replaceAll(Constants.NEW_LINE, Constants.ONE_SPACE);
        return bodyContent;
    }

    public static String getProcessedMail(String mailContent){
        if (DetectHTML.isHtml(StringEscapeUtils.unescapeHtml4(mailContent))) {
            mailContent = MailPreProcess.removeHtml(mailContent);
        }
        mailContent = EmailThreads.getTopThreadAsString(mailContent);
        return mailContent;
    }

    public static ExtractorBulkInput getExtractorBulkInput(TextServiceInput textServiceRequest){
        EmailExtractionRequest emailServiceRequest = (EmailExtractionRequest) textServiceRequest;
        String id = textServiceRequest.getId();
        String bodyContent = emailServiceRequest.getProcessedInputs().getEmailContentsBody();
        bodyContent = getProcessedMailBody(bodyContent, emailServiceRequest.getProcessedInputs().getEmailContentAsText());
        ExtractorBulkInput extractorInput = new ExtractorBulkInput(id, emailServiceRequest.getEmailSubject(),
            emailServiceRequest.getProcessedInputs().getEmailContentAsText().replaceAll(Constants.NEW_LINE, Constants.EMPTY_STRING),
            emailServiceRequest.getEmailReceivedDate(),
            ((HashMap<String, Object>) emailServiceRequest.getFeatureParams().get(Constants.ACTIVITY_EXTRACTION)).get(Constants.TIMEZONE).toString(),
            emailServiceRequest.getLanguage(), bodyContent, null, null,  null);
        return extractorInput;
    }
    
    public static ExtractorBulkInputV2 getExtractorBulkInputV2(TextServiceInput textServiceRequest){
        EmailExtractionRequest emailServiceRequest = (EmailExtractionRequest) textServiceRequest;
        String id = textServiceRequest.getId();
        String bodyContent = emailServiceRequest.getProcessedInputs().getEmailContentsBody();
        bodyContent = getProcessedMailBody(bodyContent, emailServiceRequest.getProcessedInputs().getEmailContentAsText());
        HashMap<String, Object> feature_params = ((HashMap<String, Object>) emailServiceRequest.getFeatureParams().get(Constants.ACTIVITY_EXTRACTION_V2));
        return new ExtractorBulkInputV2(id, emailServiceRequest.getEmailSubject(),
                emailServiceRequest.getProcessedInputs().getEmailContentAsText(),
                emailServiceRequest.getEmailReceivedDate(), feature_params.get(Constants.TIMEZONE).toString(), emailServiceRequest.getLanguage(), bodyContent,
                (Boolean) feature_params.get(Constants.ISCOMMITMENTREQUIRED)
                , null, null);
    }

}
