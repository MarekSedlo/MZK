package cz.mzk.scripts;

import cz.mzk.KrameriusVersion;
import cz.mzk.services.FileIO;
import cz.mzk.services.SolrProcessing;
import cz.mzk.services.SolrUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Whatever implements Script {
    FileIO fileIO = new FileIO();
    SolrUtils solrUtils = new SolrUtils(KrameriusVersion.K5);
    private final boolean DEBUG = true;

    public void start(Properties prop) {
        /*SolrUtils sUtilsTest = new SolrUtils(KrameriusVersion.K7);
        String result = sUtilsTest.getSolrParameterByPid("uuid:b4944130-5f1c-11eb-9496-005056827e52", "date_range_start.year", false);
        if (result.isEmpty())
            System.out.println("empty");
        else
            System.out.println(result);*/



        //vytahovani dat pro issue#456 - zrusit covid label
        List<String> covidPids = solrUtils.getPids("dnnt-labels:covid", 200000, DEBUG);
        fileIO.toOutputFile(covidPids, "IO/456/covidPids.txt");


        /*boolean allAreRoots = true;
        List<String> inputPids = fileIO.readFileLineByLine("IO/527/input.txt");
        for (String pid:inputPids){
            String rootPid = solrUtils.getSolrParameterByPid(pid, "root_pid", false);
            if (!pid.equals(rootPid))
                allAreRoots = false;
        }
        System.out.println("all are roots: " + allAreRoots);*/
        //BYLO TRUE :-)







        //Kontrola co je issn a co isbn v poli solru "issn:"

        /*List<String> inputPids = new ArrayList<>();
        //inputPids = fileIO.readFileLineByLine("IO/issnCheck/input");
        inputPids = solrUtils.getPids("fedora.model:monograph", 1000, true);
        List<String> issns = new ArrayList<>();
        int issnCount = 0;
        int isbnCount = 0;
        int emptyFieldCount = 0;

        for (int i=0;i<inputPids.size();i++){
            String issnSolrField = solrUtils.getSolrParameterByPid(inputPids.get(i), "issn", false);
            issns.add(inputPids.get(i) + " issn: " +issnSolrField);
            if (issnSolrField.length() > 9)
                isbnCount++;
            else if (issnSolrField.length() < 2)
                emptyFieldCount++;
            else
                issnCount++;
        }
        fileIO.toOutputFile(issns, "IO/issnCheck/output.txt");
        System.out.println("isbn count:" + isbnCount + " issn count:" + issnCount + " empty fields count:" + emptyFieldCount);*/












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
