import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;


public class Main {
    public static int minimumPriceIndex = -1;
    public static int min = Integer.MAX_VALUE;
    public static int max = 0;
    static NEREntities nerEntities = new NEREntities();
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static void main(String[] args) {
        int[] prices = {7,6,4,3,1};
        int ans = maxProfit(prices);
        System.out.println(ans);
    }
    public static NEREntities NEREntitiesExtractor(String inputSentence) {
        LOGGER.info("############NER Entities Extractor####################");
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        LOGGER.info("before starting the NEREntities Extractor Memory is " + memoryBefore);
        long start = System.currentTimeMillis();

        // create doc from inputSentence
        Document doc = new Document(inputSentence);
        System.out.println(doc);
        Sentence sent = doc.sentences().get(0);

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
    public static int maxProfit(int[] prices)
    {
        for(int i = 0; i < prices.length;i++)
        {
            if(min>prices[i])
            {
                min=prices[i];
                minimumPriceIndex = i;
            }
        }
        if(minimumPriceIndex==prices.length-1)
        {
            return 0;
        }
        else
        {
            for(int i = minimumPriceIndex; i < prices.length;i++)
            {
                if(max<prices[i])
                {
                    max=prices[i];
                }
            }
        }
        return max-min;

    }


    @Override
    public String toString(){
        return nerEntities.getParticipants() + "  " +  nerEntities.getLocation();
    }
}
