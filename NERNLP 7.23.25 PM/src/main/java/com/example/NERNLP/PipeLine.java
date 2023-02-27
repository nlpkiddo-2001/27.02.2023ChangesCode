package com.example.NERNLP;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import java.util.Properties;


public class PipeLine {
    public static synchronized StanfordCoreNLP getPipelineForPos() {

        StanfordCoreNLP pipelineForPos = null;

        if (pipelineForPos == null) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit, pos");
            props.setProperty("tokenize.whitespace", "true");
            pipelineForPos = new StanfordCoreNLP(props);
            return pipelineForPos;
        }
        return pipelineForPos;
    }

}
