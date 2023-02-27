package com.example.NERNLP;
import utility.*;


public class NewPipeLine {
    public static String getZohoPipeLine(String bodyContent)
    {
        if(bodyContent.length()>1)
        {
            bodyContent = bodyContent.replaceAll(Constants.NEW_LINE, Constants.ONE_SPACE);
        }
        TextPreprocessing processedMail = new TextPreprocessing(bodyContent);
        bodyContent = (String.join(" ", processedMail.filterSentence_())).replaceAll(Constants.NEW_LINE, Constants.ONE_SPACE);
        System.out.println(bodyContent);
        return bodyContent;
    }

}
