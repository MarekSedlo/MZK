package cz.mzk.scripts;

import cz.mzk.KrameriusVersion;
import cz.mzk.services.Connection;
import cz.mzk.services.SolrUtils;
import cz.mzk.services.XMLparser;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/*
    Issue 518, chyby ve zvukovych dokumentech

 */

public class soundDocsRelationsCheck implements Script{
    SolrUtils solrUtils = new SolrUtils(KrameriusVersion.K5);
    Connection con = new Connection();
    XMLparser parser = new XMLparser();
    private boolean DEBUG = false;
    private static final Logger logger = Logger.getLogger(updatePrivacyRegularly.class.getName());

    private List<String> soundrecordingsNotInFedora = new ArrayList<>();

    private List<String> checkUuidPrefix(List<String> pids){
        List<String> result = new ArrayList<>();
        if (!pids.isEmpty()){
            for (String pid:pids){
                if (!pid.startsWith("uuid:"))
                    result.add("uuid:" + pid);
                else
                    result.add(pid);
            }
        }
        return result;
    }

    private List<String> getChildren(String parentPID){
        List<String> children = new ArrayList<>();
        if (!parentPID.startsWith("uuid:"))
            parentPID = "uuid:" + parentPID;
        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/" + parentPID + "/datastreams/RELS-EXT/content", DEBUG);
        try {
            int response = connection.getResponseCode();
            if (response == 200){ //pokud byl parent dokument nalezen ve fedore
                StringBuilder FOXML = con.read(connection);
                children = parser.getFOXMLChildrenUuids(FOXML);
            }
            else if (response == 404){
                logger.warning("Pid not found in fedora " + parentPID);
                soundrecordingsNotInFedora.add(parentPID);
            }

            else
                logger.warning("Unexpected error " + response + ", for pid: " + parentPID);
        } catch (Exception e){
            e.printStackTrace();
        }
        children = checkUuidPrefix(children);
        return children;
    }

    private boolean existsInFedora(String inputPid){
        String pid = inputPid;
        if (!pid.startsWith("uuid:"))
            pid = "uuid:" + pid;
        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/" + pid + "/datastreams/RELS-EXT/content", DEBUG);
        try {
            int response = connection.getResponseCode();
            if (response == 200){ //pokud byl parent dokument nalezen ve fedore
                return true;
            }
            else if (response == 404)
                logger.warning("Pid not found in fedora " + pid);
            else
                logger.warning("Unexpected error " + response + ", for pid: " + pid);
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private void printLineByLine(List<String> input){
        for (String s:input)
            System.out.println(s);
    }

    public void start(Properties prop) {
        String soundRecordingQuery = "fedora.model:soundrecording";
        String soundUnitQuery = "fedora.model:soundunit";
        List<String> recordings = solrUtils.getPids(soundRecordingQuery, 1000000, false);
        int childrenOfAllRecordings = 0;
        List<String> childrenWhichAreSoundunits = new ArrayList<>();
        List<String> fedoraNotExistingSoundunitsFromRecordings = new ArrayList<>();
        List<String> fedoraNotExistingSoundunitsFromSolr = new ArrayList<>();
        List<String> fedoraNotExistingRootGotFromSoundunit = new ArrayList<>();
        for (String r:recordings){
            List<String> children = getChildren(r);
            for (String ch:children){
                String childDocType = solrUtils.getSolrParameterByPid(ch, "fedora.model", false);
                childDocType = childDocType.toLowerCase();
                if (childDocType.equals("soundunit")){
                    childrenWhichAreSoundunits.add(ch);
                    if(!existsInFedora(ch))
                        fedoraNotExistingSoundunitsFromRecordings.add(ch);
                }
            }
            childrenOfAllRecordings += children.size();
        }

        List<String> units = solrUtils.getPids(soundUnitQuery, 1000000, false);
        List<String> unitsNotInChildren = new ArrayList<>();
        List<String> soundunitsWithNotExistingParent = new ArrayList<>();
        for (String u:units){
            String rootPid = solrUtils.getSolrParameterByPid(u, "root_pid", false);
            if (!childrenWhichAreSoundunits.contains(u))
                unitsNotInChildren.add(u);
            if (!existsInFedora(u))
                fedoraNotExistingSoundunitsFromSolr.add(u);

            if (!existsInFedora(rootPid)){
                soundunitsWithNotExistingParent.add(u);
                if (!fedoraNotExistingRootGotFromSoundunit.contains(rootPid))
                    fedoraNotExistingRootGotFromSoundunit.add(rootPid);
            }
        }





        System.out.println("Soundrecordings not found in fedora: " + soundrecordingsNotInFedora.size());
        printLineByLine(soundrecordingsNotInFedora);
        System.out.println("Children got from recordings: " + childrenOfAllRecordings);
        System.out.println("Children got from recordings, which are soundunits: " + childrenWhichAreSoundunits.size());
        System.out.println("Children got from recordings, which are soundunits, which does not exists in fedora: " + fedoraNotExistingSoundunitsFromRecordings.size());
        printLineByLine(fedoraNotExistingSoundunitsFromRecordings);

        System.out.println("Num of soundunits from solr query: " + units.size());
        System.out.println("Num of soundunits, which are in solr, but are not children from relations: " + unitsNotInChildren.size());
        printLineByLine(unitsNotInChildren);
        System.out.println("Num of soundunits got directly from solr, which does not exists in fedora: " + fedoraNotExistingSoundunitsFromSolr.size());
        printLineByLine(fedoraNotExistingSoundunitsFromSolr);
        System.out.println("Parents got from solr root_pid field, which does not exists: " + fedoraNotExistingRootGotFromSoundunit.size());
        printLineByLine(fedoraNotExistingRootGotFromSoundunit);
        System.out.println("Children with not existing parent: " + soundunitsWithNotExistingParent.size());
        printLineByLine(soundunitsWithNotExistingParent);
    }
}



























