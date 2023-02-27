package com.example.NERNLP;
import java.io.BufferedWriter;
//import utility.*;

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

import java.util.concurrent.TimeUnit;

@RestController

public class NerWithPipeLine {

    @PostMapping("/test2")

    public ResponseEntity<Map<String, List<com.example.demo.DataResponse>>> ner2(@RequestBody Map<String, List<com.example.demo.InputData>> inputs) throws IOException

    {

        String fileName = "ner_test_with_pipeline"+ System.nanoTime() +".txt";

        BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));

        long memoryTaken = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();

        System.out.println("memory status before starting the appication with pipeline " + memoryTaken);

        bw.newLine();

        bw.write("memory status before starting the application with pipeline : " + memoryTaken + "\n");

        long totalStartTime = System.currentTimeMillis();

        StanfordCoreNLP stanfordCoreNlp = PipeLine.getPipelineForPos();

        List<com.example.demo.DataResponse> nerResults = new ArrayList<>();

        List<InputData> input = inputs.get("input");

        for(int i  = 0; i < input.size(); i++)

        {

            long inputStartTime = System.currentTimeMillis();

            CoreDocument document = new CoreDocument(input.get(i).getText());

            stanfordCoreNlp.annotate(document);

            List<NerEntities> nerEntities = new ArrayList<>();

            List<CoreLabel> coreLabels = document.tokens();

            for(CoreLabel coreLabel:coreLabels)

            {

                String ner = coreLabel.getString(CoreAnnotations.NamedEntityTagAnnotation.class);

                nerEntities.add(new NerEntities(coreLabel.originalText(), ner));

            }

            long middleMemory = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();

            System.out.println(" memory status at the middle of the appication with pipeline " + middleMemory);

            bw.write(" memory status for   " + i +  " is "  +  middleMemory + " with pipeline \n");

            bw.newLine();

            DataResponse nerResult = new DataResponse();

            nerResult.setId(input.get(i).getId());



            nerResult.setNerEntities(nerEntities);

            nerResults.add(nerResult);

            long inputEndTime = System.currentTimeMillis();

            long inputTimeTaken = inputEndTime - inputStartTime;

            System.out.println("time taken for " + i + " single input is with pipeline is " + inputTimeTaken);

            bw.newLine();

            bw.write(" time taken for  " + i + "  " + input.get(i).getText() + " input is with pipeline is " + inputTimeTaken + " ms , ");

            bw.newLine();

        }

        Map<String,List<DataResponse>> response = new HashMap<>();

        String HttpResponseStatus = "HTTP " + HttpStatus.OK.value() + " " + HttpStatus.OK.getReasonPhrase();

        response.put(HttpResponseStatus, nerResults);

        long totalEndTime = System.currentTimeMillis();

        long timeTaken = totalEndTime - totalStartTime;

        long memoryAtEnd = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();

        System.out.println(" memory status after ending the appication with pipeline " + memoryAtEnd);

        bw.newLine();

        bw.write(" memory at the end with pipeline " + memoryAtEnd + "\n");

        bw.newLine();

        bw.write(" total time taken with pipeline " + timeTaken);

        System.out.println(" total Time taken = " + timeTaken + " ms   with pipeline  ");

        bw.close();

        return ResponseEntity.ok(response);

    }
}

