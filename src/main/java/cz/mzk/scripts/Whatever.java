package cz.mzk.scripts;

import cz.mzk.services.Connection;
import cz.mzk.services.FileIO;
import cz.mzk.services.SolrUtils;
import cz.mzk.services.XMLparser;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Whatever implements Script {
    FileIO fileIO = new FileIO();
    SolrUtils solrUtils = new SolrUtils();
    private final boolean DEBUG = true;
    /*Connection con = new Connection();
    XMLparser parser = new XMLparser();

    private StringBuilder debugOutput = new StringBuilder();
    private int list4Pages = 0;
    private int list5Pages = 0;*/

    //issue Erika
    /*private void getDeeper(String parentUuid, boolean isList4){
        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/uuid:" + parentUuid + "/datastreams/RELS-EXT/content", DEBUG);
        StringBuilder FOXML = con.read(connection);
        List<String> childrenUuids = parser.getFOXMLChildrenUuids(FOXML);

        if (childrenUuids.size() > 0){
            for (String childUuid : childrenUuids){
                getDeeper(childUuid, isList4);
            }
        } else { //tady je child
            if (isList4)
                list4Pages++;
            else
                list5Pages++;
        }
    }*/

    public void start(Properties prop) {
        //vytahovani dat pro issue#456 - zrusit covid label
        List<String> covidPids = solrUtils.getPids("dnnt-labels:covid", 1600000, DEBUG);
        fileIO.toOutputFile(covidPids, "IO/456/covidPids.txt");


        //issue Erika

        /*List<String> list4 = fileIO.readFileLineByLine("IO/Erika/list4.txt");
        List<String> list5 = fileIO.readFileLineByLine("IO/Erika/list5.txt");

        String uuid = "";

        for (String item : list4){
            String[] parts = item.split("uuid:");
            uuid = parts[1];
            uuid = uuid.replaceAll("\\s+$", "");
            getDeeper(uuid, true);
        }

        for (String item : list5){
            String[] parts = item.split("uuid:");
            uuid = parts[1];
            uuid = uuid.replaceAll("\\s+$", "");
            getDeeper(uuid, false);
        }

        debugOutput.append("Pocet stranek v listu 4: " + list4Pages + "\n");
        debugOutput.append("Pocet stranek v listu 5: " + list5Pages);
        System.out.println(debugOutput);*/



        // Vytahovani dat ze souboru od Kuby k issue 437
        /*String file = fileIO.readFileToStr("IO/437/SOLRPUBLIC.txt");
        List<String> uuids = new ArrayList<>();
        //parsing
        String[] parts = file.split(" ");
        int sizeShouldBe = Integer.parseInt(parts[0]);

        assert (parts.length == sizeShouldBe);

        for (String part : parts){
            String uuid = "NOTHING";
            if (part.length() > 2){
                //if ((part.charAt(part.length()-1) == '[') && (part.charAt(part.length()-2) == ','))
                //    uuid = part.substring(0, part.length()-2);
                if ((part.charAt(part.length()-1) == ',') || (part.charAt(part.length()-1) == '[')|| (part.charAt(part.length()-1) == ']'))
                    uuid = part.substring(0, part.length()-1);
                if (uuid.charAt(0) == 'u')
                    uuids.add(uuid);
                else if ((uuid.charAt(0) == '[') && (uuid.charAt(1) == 'u'))
                    uuids.add(uuid.substring(1));
                else
                    System.out.println(part);
            }
        }

        assert (uuids.size() == sizeShouldBe);

        fileIO.toOutputFile(uuids, "IO/437/output.txt");*/
    }
}
