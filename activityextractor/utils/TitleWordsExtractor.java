//$Id$
package com.zoho.zia.crm.activityextractor.utils;

import com.adventnet.crm.common.util.CommonUtil;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import edu.stanford.nlp.util.Triple;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TitleWordsExtractor {

    private static final Logger LOGGER = Logger.getLogger(TitleWordsExtractor.class.getName());
    private static final String ROOT = CommonUtil.getProperty("server.dir");
    static CRFClassifier<CoreLabel> crf = getCRFInstance();

    public static List<Triple<String, Integer, Integer>> parse(String input) {
        LOGGER.info("TitleWordsExtractor :: Activity Title Parser");
        return crf.classifyToCharacterOffsets(input);
    }

    private static CRFClassifier<CoreLabel> getCRFInstance() {

        Properties props = new Properties();
        InputStream file = null;
        try {
            file = new FileInputStream(ROOT + "/data/activity/title/title.config.props");
        } catch (FileNotFoundException e2) {
            LOGGER.log(Level.SEVERE, "TitleWordsExtractor :: Title Module : Error While Loading Props :: FileNotFoundException");
        }
        try {
            props.load(file);
            LOGGER.info("TitleWordsExtractor ::  Title Module: Props Loaded");
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "TitleWordsExtractor :: Title Module : Error While Loading Props :: IOException");
        }

        File dateGazetteFile;
        dateGazetteFile = new File(ROOT + "/data/activity/title/titlesGazette.txt");
        LOGGER.info("TitleWordsExtractor ::  Title Module: Loaded Gazzete File");
        LOGGER.info("TitleWordsExtractor :: Title Module: Gazzete File Path" + dateGazetteFile.getAbsolutePath());
        props.setProperty("distSimLexicon", dateGazetteFile.getAbsolutePath()); //No I18Nca
        String gazette = generateGazette(dateGazetteFile.getAbsolutePath());
        props.setProperty("gazette", gazette); //No I18N
        SeqClassifierFlags flags = new SeqClassifierFlags(props);
        CRFClassifier<CoreLabel> crf = new CRFClassifier<>(flags);

        LOGGER.info("TitleWordsExtractor :: Title Module: Loading Title Model");
        InputStream parserModel;
        try {
            parserModel = IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(ROOT + "/data/activity/title/title.crf.ser.gz");   //No I18N
        } catch (NullPointerException e) {
            LOGGER.info("TitleWordsExtractor :: Title Module: Title Model is NULL" + e);
            parserModel = null;
        } catch (IOException e) {
            LOGGER.info("TitleWordsExtractor :: Title Module: Title Model can't be Found" + e);
            parserModel = null;
        }
        if (parserModel != null) {
            try {
                crf.loadClassifier(parserModel);
            } catch (ClassCastException | ClassNotFoundException | IOException e) {
                LOGGER.log(Level.SEVERE, "TitleWordsExtractor :: Title Module: Error While Loading Model in CRF");
                LOGGER.log(Level.SEVERE, ExceptionUtils.getStackTrace(e));
            }
        } else {
            crf = null;
            LOGGER.info("TitleWordsExtractor :: Title Module: CRF is Null as Parser model is NULL");
        }
        return crf;
    }

    private static String generateGazette(String dateGazettePath) {
        return dateGazettePath;
    }
}
