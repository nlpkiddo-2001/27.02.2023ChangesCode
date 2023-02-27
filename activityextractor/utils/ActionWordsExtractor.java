//$Id$
package com.zoho.zia.crm.activityextractor.utils;

import com.adventnet.crm.common.util.CommonUtil;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import edu.stanford.nlp.util.Triple;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActionWordsExtractor {
    private static final Logger LOGGER = Logger.getLogger(ActionWordsExtractor.class.getName());
    private static final String ROOT = CommonUtil.getProperty("server.dir");

    static CRFClassifier<CoreLabel> crf = getCRFInstance();

    public static List<Triple<String, Integer, Integer>> parse(String input) {
        return crf.classifyToCharacterOffsets(input);
    }

    private static CRFClassifier<CoreLabel> getCRFInstance() {
        Properties props = new Properties();
        InputStream file = null;
        try {
            file = new FileInputStream(ROOT + "/data/activity/action/action.config.props");
        } catch (FileNotFoundException e2) {
            LOGGER.log(Level.SEVERE, "ActionWordsExtractor :: Action Module : Error While Loading Props"); //No I18N
        }
        try {
            props.load(file);
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "ActionWordsExtractor :: Action Module : Error While Loading Props"); //No I18N
        }

        File dateGazetteFile;
        LOGGER.info("ActionWordsExtractor :: Action Module: Loading Gazette File");
        dateGazetteFile = new File(ROOT + "/data/activity/action/date-gazette.txt"); //No I18N
        LOGGER.info("ActionWordsExtractor :: Action Module: Loaded Gazette File");
        LOGGER.info("ActionWordsExtractor :: Action Module: Gazette File Path" +
                dateGazetteFile.getAbsolutePath());
        props.setProperty("distSimLexicon", dateGazetteFile.getAbsolutePath()); //No I18N
        String gazette = generateGazette(dateGazetteFile.getAbsolutePath());
        props.setProperty("gazette", gazette); //No I18N
        SeqClassifierFlags flags = new SeqClassifierFlags(props);
        CRFClassifier<CoreLabel> crf = new CRFClassifier<>(flags);

        InputStream parserModel;
        try {
            parserModel = IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(ROOT + "/data/activity/action/action.crf.ser.gz");  //No I18N
        } catch (NullPointerException e) {
            LOGGER.info("ActionWordsExtractor :: Action Module: Title Model is NULL " + e);
            parserModel = null;
        } catch (IOException e) {
            LOGGER.info("ActionWordsExtractor :: Action Module: Title Model can't be Found " + e);
            parserModel = null;
        }
        if (parserModel != null) {
            try {
                crf.loadClassifier(parserModel);
            } catch (ClassCastException | ClassNotFoundException | IOException e) {
                LOGGER.info("ActionWordsExtractor ::  Action Module: Error While Loading Model in CRF " + e);
            }
        } else {
            crf = null;
            LOGGER.info("ActionWordsExtractor :: Action Module: CRF is Null as Parser model is NULL ");
        }
        return crf;
    }

    private static String generateGazette(String dateGazettePath) {
        return dateGazettePath;
    }
}
