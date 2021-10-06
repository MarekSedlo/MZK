package cz.mzk;


import cz.mzk.scripts.*;
import cz.mzk.services.GetPropertyValues;

import java.io.IOException;
import java.util.Properties;

public class Main {
    private static final GetPropertyValues properties = new GetPropertyValues();
    private static Properties prop;

    public static void main(String[] args) throws IOException {
        try {
            prop = properties.getPropValues();
        } catch (IOException e){
            System.err.println("ERROR: cannot read config.properties");
            e.printStackTrace();
        }

        final String scriptName = "GetSDNNT";
        Script script = null;

        switch (scriptName){
            case "CheckMonographCoverAndContentPrivacy": {
                script = new CheckMonographCoverAndContentPrivacy();
                break;
            }
            case "CancelTheProcesses": {
                script = new CancelTheProcesses();
                break;
            }
            case "CheckImagesExistenceInRovnost": {
                script = new CheckImagesExistenceInRovnost();
                break;
            }
            case "GetSolrRootDocumentType": {
                script = new GetSolrRootDocumentType();
                break;
            }
            case "HowManyNDK": {
                script = new HowManyNDK();
                break;
            }
            case "GetSDNNT": {
                script = new GetSDNNT();
                break;
            }
            case "changePrefix": {
                script = new changePrefix();
                break;
            }
            default:{
                return;
            }
        }

        assert(script!=null);
        script.start(prop);

        return;
    }
}
