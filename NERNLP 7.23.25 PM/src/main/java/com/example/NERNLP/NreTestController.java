package com.example.demo;
//created without any pipeline. it has no pipeline.
//Properties props = new Properties();
//props.setProperty("annotators", "tokenize, ssplit, parse"); // text cleaning in original code
//StanfordCoreNLP stanfordCoreNlp = new StanfordCoreNLP(props);
import java.io.BufferedWriter;
import utility.*;

import java.io.FileWriter;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.util.*;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.CoreDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class NerEntities {
    private String word;
    private String label;
    public String getWord() {
        return word;
    }
    public void setWord(String word) {
        this.word = word;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }

    public NerEntities(String word, String label)
    {
        this.word = word;
        this.label = label;
    }
}
class InputData {
    private String id;
    private String text;

    public InputData() {
    }

    public InputData(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
class DataResponse {
    private String id;
    private List<NerEntities> nerEntities;
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public List<NerEntities> getNerEntities() {
        return nerEntities;
    }
    public void setNerEntities(List<NerEntities> nerEntities) {
        this.nerEntities = nerEntities;
    }
}


@RestController
public class NreTestController {



    @PostMapping("/test")
    public ResponseEntity<String> ner(@RequestBody Map<String, List<InputData>> inputs) throws IOException
    {
        String fileName = "ner_test"+ System.nanoTime() +".txt";
        BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
        long memoryTaken = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        System.out.println("memory status before starting the appication " + memoryTaken);
        bw.newLine();
        bw.write("memory status before starting the application: " + memoryTaken + "\n");
        long totalStartTime = System.currentTimeMillis();
        Properties props = new Properties();
        props.setProperty("ner.useSUTime", "false");
        StanfordCoreNLP stanfordCoreNlp = new StanfordCoreNLP(props);
        List<DataResponse> nerResults = new ArrayList<>();
        List<InputData> input = inputs.get("input");
        List<String> processedDataToThePipeLine = new ArrayList<>();
        for(int i = 0; i < input.size();i++)
        {
            String processed = NewPipeLine.getZohoPipeLine(input.get(i).getText());
            processedDataToThePipeLine.add(processed);
        }
        for(int i = 0; i < processedDataToThePipeLine.size(); i++)
        {
            long inputStartTime = System.currentTimeMillis();
            CoreDocument document = new CoreDocument(processedDataToThePipeLine.get(i));
            stanfordCoreNlp.annotate(document);
            List<NerEntities> nerEntities = new ArrayList<>();
            List<CoreLabel> coreLabels = document.tokens();
            for(CoreLabel coreLabel:coreLabels)
            {
                String ner = coreLabel.getString(CoreAnnotations.NamedEntityTagAnnotation.class);
                nerEntities.add(new NerEntities(coreLabel.originalText(), ner));
            }
            long middleMemory = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
            System.out.println(" memory status at the middle of the appication " + middleMemory);
            bw.write(" memory status for   " + i +  " is "  +  middleMemory + "\n");
            bw.newLine();
            DataResponse nerResult = new DataResponse();
            nerResult.setId(input.get(i).getId());
            nerResult.setNerEntities(nerEntities);
            nerResults.add(nerResult);
            long inputEndTime = System.currentTimeMillis();
            long inputTimeTaken = inputEndTime - inputStartTime;
            System.out.println("time taken for " + i + " single input is " + inputTimeTaken);
            bw.newLine();
            bw.write(" time taken for " + i + input.get(i).getText() + " input is " + inputTimeTaken + " ms , ");
            bw.newLine();
        }
        Map<String, List<DataResponse>> response = new HashMap<>();
        String HttpResponseStatus = "HTTP " + HttpStatus.OK.value() + " " + HttpStatus.OK.getReasonPhrase();
        response.put(HttpResponseStatus, nerResults);
        ObjectMapper objectMapper = new ObjectMapper();
        String responseJson = objectMapper.writeValueAsString(response);
        long totalEndTime = System.currentTimeMillis();
        long timeTaken = totalEndTime - totalStartTime;
        long memoryAtEnd = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        System.out.println(" memory status after ending the appication " + memoryAtEnd);
        bw.newLine();
        bw.write(" memory at the end " + memoryAtEnd + "\n");
        bw.newLine();
        bw.write(" total time taken " + timeTaken);
        System.out.println(" total Time taken = " + timeTaken + " ms ");
        bw.close();
        return ResponseEntity.ok(responseJson);

    }

}



