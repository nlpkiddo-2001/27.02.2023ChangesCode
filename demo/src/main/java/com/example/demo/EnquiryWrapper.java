package com.example.demo;

import org.apache.coyote.Response;
import org.apache.juli.logging.Log;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.EnquiryExtraction.*;

import javax.security.auth.callback.ConfirmationCallback;
import javax.xml.crypto.Data;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
@RestController
public class EnquiryWrapper {
    @PostMapping("/enquiry1")
    public   ResponseEntity<Map<String,DataResponse>> enquiryResponse(@RequestBody Map<String, InputData> emailBody) throws NullPointerException
    {
       InputData inputData = new InputData();
       inputData = emailBody.get("input1");
       System.out.println(inputData);
       String responseEntitiy = inputData.getEnquiry();
       responseEntitiy = new EnquiryExtraction().enquiryExtract(responseEntitiy);
       Map<String, DataResponse> responseMap =  new HashMap<>();
       DataResponse response = new DataResponse();
       response.setResponse(responseEntitiy);
       response.setId(inputData.getId());
       String statusResponse = " HTTP " + HttpStatus.OK.value() + " " + HttpStatus.OK.getReasonPhrase();
        responseMap.put(statusResponse, response);
        return ResponseEntity.ok(responseMap);


    }

}


class InputData
{
    private String id;
    private String enquiry;


    public InputData() {
    }

    public InputData(String id, String enquiry)
    {
        this.id = id;
        this.enquiry = enquiry;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEnquiry() {
        return enquiry;
    }

    public void setEnquiry(String enquiry) {
        this.enquiry = enquiry;
    }
}

class DataResponse
{
    private String id;
    private String response;

    public DataResponse(){

    }
    public DataResponse(String id, String response)
    {
        this.id = id;
        this.response = response;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
