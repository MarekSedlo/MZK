package cz.mzk.scripts;

import cz.mzk.KrameriusVersion;
import cz.mzk.services.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.util.logging.*;
import java.util.logging.FileHandler;

/*
    Skript pro kontrolu dostupnosti vsech periodik s rokem vydani do 1911 (toYearPeriodicals) a vsech ostatnich dokumentu s rokem vydani do 1900 (toYearAllDocs).
    Tyto dokumenty maji byt public. V pripade, ze nejsou, tak bude provedena jejich zmena a vytvoren log o teto zmene.
    Skript ma byt spousteny pravidelne.
    Skript ma podporovat K5 i K7
    (issue #438)
 */


public class updatePrivacyRegularly implements Script{
    private static final Logger logger = Logger.getLogger(updatePrivacyRegularly.class.getName());
    private FileHandler fileHandler = null;
    private final boolean DEBUG = true;
    private final int maxPidsToRead = 1000000;
    private final boolean K7 = false;
    private List<String> notFoundInFedora = new ArrayList<>(); // TODO zjistit, jak se bude delat kontrola na akubre
    private final int fromYearAllDocs = 3;
    private final int toYearAllDocs = 1900;
    private final int fromYearPeriodicals = toYearAllDocs+1;
    private final int toYearPeriodicals = 1911;


    SolrUtils solrUtils;
    SolrProcessing solrProcessing;
    FileIO fileIO = new FileIO();
    Connection con = new Connection();
    XMLparser parser = new XMLparser();

    private String publicationYearSolrQ;
    private String privacySolrQ;
    private String documentTypeSolrQ;
    private String rootPidSolrQ;
    private String dnntLabelSolrQ; //TODO POZOR ZJISTIT JESTE JAK JE NAPSANE DNNT V K7!


    private void loggerInit(){
        try {
            fileHandler = new FileHandler("logs/438/log.log");
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //hleda potomky vstupniho pidu, vraci pouze boolean, ze nejaci potomci existuji, jedno zanoreni
    private boolean haveChildrenInFedora(String pid){
        boolean haveChildren = false;
        if (!pid.startsWith("uuid:"))
            pid = "uuid:" + pid;
        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/" + pid + "/datastreams/RELS-EXT/content", DEBUG);
        try {
            int response = connection.getResponseCode();
            if (response == 200){
                List<String> childrenUuids = new ArrayList<>();
                if (K7){ //verze pro K7, bere si potomky z processing solru //TODO zjistit, jestli se v K7 bude delat kontrola, zda se dokument nachazi na fedore, respektive jak se bude delat kontrola, jestli dokument existuje
                    childrenUuids = solrProcessing.getChildrenOfCurrentPid(pid);
                }
                else {
                    StringBuilder FOXML = con.read(connection);
                    childrenUuids = parser.getFOXMLChildrenUuids(FOXML);
                }
                if (childrenUuids.size() > 0)
                    haveChildren = true;
            }
            else if (response == 404){
                logger.warning("PID NOT FOUND IN FEDORA: " + pid + " SKIPPING\n");
                notFoundInFedora.add(pid);
            }

            else
                logger.severe("Unexpected error " + response + ", when fedora connecting to pid: " + pid + " SKIPPING\n");
        } catch (Exception e){
            e.printStackTrace();
        }
        return haveChildren;
    }

    //prida vsem pidum ve vstupnim seznam prefix uuid:, v pripade, ze ho nemaji
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

    //vraci potomky vstupniho pidu, pouze jedno zanoreni
    List<String> getChildren(String parentPID){
        List<String> children = new ArrayList<>();
        if (!parentPID.startsWith("uuid:"))
            parentPID = "uuid:" + parentPID;
        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/" + parentPID + "/datastreams/RELS-EXT/content", DEBUG);
        try {
            int response = connection.getResponseCode();
            if (response == 200){ //pokud byl parent dokument nalezen ve fedore
                if (K7){ //verze pro K7, bere si potomky z processing solru //TODO zjistit, jestli se v K7 bude delat kontrola, zda se dokument nachazi na fedore, respektive jak se bude delat kontrola, jestli dokument existuje
                    children = solrProcessing.getChildrenOfCurrentPid(parentPID);
                }
                else {
                    StringBuilder FOXML = con.read(connection);
                    children = parser.getFOXMLChildrenUuids(FOXML);
                }
            }
            else if (response == 404)
                logger.severe("Pid, which was previously found in fedora, cannot be found now!!!: " + parentPID);
            else
                logger.severe("Unexpected error " + response + ", for document, which was previously found!!! pid: " + parentPID);
        } catch (Exception e){
            e.printStackTrace();
        }

        children = checkUuidPrefix(children);
        return children;
    }


    //Najde potomky vstupniho root pidu
    //Pokud tito potomci maji sve vlastni potomky, tak je prida do seznamu
    //Vraci seznam potomku, ktere je treba zkontrolovat
    List<String> getChildrenForCheck(String rootPid){
        List<String> result = new ArrayList<>();
        List<String> children = getChildren(rootPid);

        if (children.size() > 0){
            for (String child:children){
                if (haveChildrenInFedora(child)){ //pokud existuji vnoucata
                    result.add(child);
                }
            }
        }
        else
            logger.info("ROOT without children, rootPID: " + rootPid);
        return result;
    }

    //Tato funkce hleda pid ve fedore a vraci boolean, jestli neco nasla
    //TODO zjistit, jestli se v K7 bude delat kontrola, zda se dokument nachazi na fedore, respektive jak se bude delat kontrola, jestli dokument existuje
    private boolean pidExistsInFedora(String pid){
        boolean pidExists = false;
        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/" + pid + "/datastreams/RELS-EXT/content", DEBUG);
        try {
            int response = connection.getResponseCode();
            if (response == 200){
                pidExists = true;
            }
            else if (response == 404){
                pidExists = false;
                logger.warning("PID NOT FOUND IN FEDORA: " + pid + " SKIPPING\n");
                notFoundInFedora.add(pid);
            }
            else {
                pidExists = false;
                logger.severe("Unexpected error " + response + ", when fedora connecting to pid: " + pid + " SKIPPING\n");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return pidExists;
    }

    //Tato funkce vytvori solr dotaz, kde hleda rok vydani podle zadaneho pidu
    //Pote vrati boolean, jestli je rok vydani v zadanem intervalu
    private boolean isPidInInterval(String pid, int fromYear, int toYear){
        boolean pidInInterval = false;
        if (!pid.startsWith("uuid:"))
            pid = "uuid:" + pid;
        String yearOfPublicationStr = solrUtils.getSolrParameterByPid(pid, publicationYearSolrQ, false);
        if (yearOfPublicationStr.equals("null")){
            logger.warning("pid exists in solr, but " + publicationYearSolrQ + " field does not, pid: " + pid);
            return false;
        }

        int yearOfPublication = Integer.parseInt(yearOfPublicationStr);
        if ((fromYear <= yearOfPublication) && (yearOfPublication <= toYear)) //Pozor tady by to mohlo spadnout, kdyz tam nebude validni hodnota
            pidInInterval = true;
        else
            pidInInterval = false;
        return pidInInterval;
    }

    private boolean isPidPublic(String pid){
        String privacy = solrUtils.getSolrParameterByPid(pid, privacySolrQ, false);
        return privacy.equals("public");
    }

    //Tato funkce vraci seznam pidu, ktere se maji zmenit na verejne (predpoklada se vstup root pidů)
    //Pokud ma root pid vnoucata, tak zkontroluje i deti a da je spolecne i s rootem do seznamu, ktery vraci
    private List<String> getPidsForPrivacyChange(List<String> inputRootPids, boolean publicInput, int fromYear, int toYear){
        List<String> result = new ArrayList<>();
        for (String rootPid:inputRootPids){
            logger.info("CHECK ROOT: " + rootPid);
            if (!pidExistsInFedora(rootPid)) //TODO zjistit, jestli se v K7 bude delat kontrola, zda se dokument nachazi na fedore, respektive jak se bude delat kontrola, jestli dokument existuje
                continue;
            if (publicInput){
                //zkontrolovat deti, jestli maji byt public
                List<String> childrenToCheck = getChildrenForCheck(rootPid); //tato funkce vraci deti na kontrolu dostupnosti (pouze deti, ktere maji sve vlastni deti)
                for (String child:childrenToCheck){
                    logger.info("CHECK CHILD: " + child);
                    if (!solrUtils.pidExistsInSolr(child)){
                        logger.warning("pid does not exist in solr: " + child + " SKIPPING\n");
                        continue;
                    }
                    if (isPidPublic(child))
                        logger.info("Child pid is already public");
                    else {
                        boolean childShouldBePublic = isPidInInterval(child, fromYear, toYear);
                        if (childShouldBePublic){
                            logger.info("Child pid should be changed to public: " + child);
                            result.add(child);
                        }
                        else
                            logger.info("Child pid should remain private: " + child);
                    }
                }
                logger.info("Root pid is already public: " + rootPid + "\n");
            }
            else {
                boolean rootPidShouldBePublic = isPidInInterval(rootPid, fromYear, toYear);
                boolean changeRootToPublic = rootPidShouldBePublic;
                if (rootPidShouldBePublic){
                    //zkontrolovat deti, jestli maji byt public
                    List<String> childrenForCheck = getChildrenForCheck(rootPid); //tato funkce vraci deti na kontrolu dostupnosti (pouze deti, ktere maji sve vlastni deti)
                    for (String childPid:childrenForCheck){
                        logger.info("CHECK CHILD: " + childPid);
                        if (!solrUtils.pidExistsInSolr(childPid)){
                            logger.warning("pid does not exist in solr: " + childPid + " SKIPPING\n");
                            changeRootToPublic = false;
                            continue;
                        }
                        if (isPidPublic(childPid))
                            logger.info("Child pid is already public");
                        else {
                            boolean childShouldBePublic = isPidInInterval(childPid, fromYear, toYear);
                            if (childShouldBePublic){
                                logger.info("Child pid should be changed to public: " + childPid);
                                result.add(childPid);
                            }
                            else{
                                logger.info("Child pid should remain private: " + childPid);
                                changeRootToPublic = false;
                            }
                        }
                    }
                }
                if (changeRootToPublic){
                    logger.info("Root pid should be changed to public: " + rootPid + "\n");
                    result.add(rootPid);
                }
                else
                    logger.info("Root pid should remain private: " + rootPid + "\n");
            }
        }
        return result;
    }


    /*  This function reads input list of pids and returns pids, which are ALREADY roots.
        (It does not find root pids of given pids, it returns roots of given list.)
    */
    private List<String> extractRoot(List<String> inputPids){
        List <String> rootPids = new ArrayList<>();
        String rootPid = "";
        for (String pid : inputPids){
            rootPid = solrUtils.getSolrParameterByPid(pid, rootPidSolrQ, false);
            if (pid.equals(rootPid))
                rootPids.add(rootPid);
        }
        return rootPids;
    }

    public void start(Properties prop) {
        loggerInit();
        if (K7){
            solrUtils = new SolrUtils(KrameriusVersion.K7);
            solrProcessing = new SolrProcessing(KrameriusVersion.K7);
            publicationYearSolrQ = "date_range_start.year";
            privacySolrQ = "accessibility";
            documentTypeSolrQ = "model";
            rootPidSolrQ = "root.pid";
            dnntLabelSolrQ = "TODO!!!"; //TODO!!!
        }
        else { //K5
            solrUtils = new SolrUtils(KrameriusVersion.K5);
            publicationYearSolrQ = "datum_begin";
            privacySolrQ = "dostupnost";
            documentTypeSolrQ = "fedora.model";
            rootPidSolrQ = "root_pid";
            dnntLabelSolrQ = "dnnt-labels";
        }

        /* interval for checking all types
            3-18
            21-189
            210-1900
         */

        String query = publicationYearSolrQ + ":["+fromYearAllDocs+" TO 18] AND " + privacySolrQ + ":private AND NOT " + dnntLabelSolrQ + ":license";
        List<String> pidsAllTypes = solrUtils.getPids(query, maxPidsToRead, true);

        query = publicationYearSolrQ + ":[21 TO 189] AND " + privacySolrQ + ":private AND NOT " + dnntLabelSolrQ + ":license";
        pidsAllTypes.addAll(solrUtils.getPids(query, maxPidsToRead, true));

        query = publicationYearSolrQ + ":[210 TO "+toYearAllDocs+"] AND " + privacySolrQ + ":private AND NOT " + dnntLabelSolrQ + ":license";
        pidsAllTypes.addAll(solrUtils.getPids(query, maxPidsToRead, true));

        List<String> rootPids = extractRoot(pidsAllTypes); //realna data, zakomentovano, protoze to trva

        //all types from fromYearAllDocs to toYearAllDocs
        List<String> pidsForPrivacyChange = getPidsForPrivacyChange(rootPids, false, fromYearAllDocs, toYearAllDocs);

        logger.info("\n\n\n\n#################################################################" +
                "\nALL DOCUMENTS TO "+toYearAllDocs+" DONE, check for periodicals and their volumes from "+fromYearPeriodicals+" to "+toYearPeriodicals+":\n#################################################################" +
                "\n\n\n\n");

        /* interval for checking just periodicals, which were not checked in interval of all types
            190
            1901-1911
         */

        query = publicationYearSolrQ + ":190 AND " + documentTypeSolrQ + ":periodical AND " + privacySolrQ + ":private AND NOT " + dnntLabelSolrQ + ":license";
        List<String> pidsPerioPrivate = solrUtils.getPids(query, maxPidsToRead, true);

        query = publicationYearSolrQ + ":["+fromYearPeriodicals+" TO "+toYearPeriodicals+"] AND " + documentTypeSolrQ + ":periodical AND " + privacySolrQ + ":private AND NOT " + dnntLabelSolrQ + ":license";
        pidsPerioPrivate.addAll(solrUtils.getPids(query, maxPidsToRead, true));


        //private periodicals from fromYearPeriodicals to toYearPeriodicals
        pidsForPrivacyChange.addAll(getPidsForPrivacyChange(pidsPerioPrivate, false, fromYearPeriodicals, toYearPeriodicals));


        //#################################################
        query = publicationYearSolrQ + ":190 AND " + documentTypeSolrQ + ":periodical AND " + privacySolrQ + ":public AND NOT " + dnntLabelSolrQ + ":license";
        List<String> pidsPerioPublic = solrUtils.getPids(query, maxPidsToRead, true);

        query = publicationYearSolrQ + ":["+fromYearPeriodicals+" TO "+toYearPeriodicals+"] AND " + documentTypeSolrQ + ":periodical AND " + privacySolrQ + ":public AND NOT " + dnntLabelSolrQ + ":license";
        pidsPerioPublic.addAll(solrUtils.getPids(query, maxPidsToRead, true));

        //public periodicals from fromYearPeriodicals to toYearPeriodicals
        pidsForPrivacyChange.addAll(getPidsForPrivacyChange(pidsPerioPublic, true, fromYearPeriodicals, toYearPeriodicals));


        //unnecessary test, solr query guarantee, that there are no dnnt labels "license"
        List<String> pidsToMakePublic = new ArrayList<>();
        for (String pidForChange: pidsForPrivacyChange){
            String dnntLabel = solrUtils.getSolrParameterByPid(pidForChange, dnntLabelSolrQ, false);
            if (!dnntLabel.equals("license"))
                pidsToMakePublic.add(pidForChange);
        }

        fileIO.toOutputFile(pidsToMakePublic, "IO/438/pidsToMakePublic.txt");
        fileIO.toOutputFile(notFoundInFedora, "IO/438/notFoundInFedora.txt");
    }
}

/*
    OTAZKY
    1. Existuji rocniky(potomci), kteri maji rok vydani treba mensi, nez 1900 nebo mensi, nez 1911,
    ale jejich root pid ma rok vydani 0, co s tim? Je treba je menit? Je treba nejak opravit root pid?

    2. Spise zajimavost, vickrat je tam situace, kdy root dokument je v intervalu, ale ani jeden jeho rocnik ne. Asi netreba resit
 */




















