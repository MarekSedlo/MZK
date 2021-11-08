package cz.mzk.scripts;

import cz.mzk.services.Connection;
import cz.mzk.services.FileIO;
import cz.mzk.services.SolrUtils;
import cz.mzk.services.XMLparser;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class updatePrivacyRegularly implements Script{
    private final boolean DEBUG = true;
    private final int maxPidsToRead = 1000000;
    private final boolean K7SolrNames = false;
    SolrUtils solrUtils = new SolrUtils();
    FileIO fileIO = new FileIO();
    Connection con = new Connection();
    XMLparser parser = new XMLparser();

    private String publicationYearSolrQ = "datum_begin";
    private String privacySolrQ = "dostupnost";
    private String documentTypeSolrQ = "fedora.model";
    private String rootPidSolrQ = "root_pid";

    /*private void getDeeper(String parentUuid, boolean isList4){
        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/uuid:" + parentUuid + "/datastreams/RELS-EXT/content", DEBUG);
        StringBuilder FOXML = con.read(connection);
        List<String> childrenUuids = parser.getFOXMLChildrenUuids(FOXML);
        //TODO rok si vezmu ze solru

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


    /*private boolean isWholeDocPrivate(String parentPID){
        String yearOfPublicationStr = solrUtils.getSolrParameterByPid(parentPID, publicationYearSolrQ, false);
        int yearOfPublication = Integer.parseInt(yearOfPublicationStr);

        if ((250 <= yearOfPublication) && (yearOfPublication <= 1900)) //Pozor tady by to mohlo spadnout, kdyz tam nebude validni hodnota
            return //TODO HERE!!!!!!!!!!

        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/uuid:" + parentPID + "/datastreams/RELS-EXT/content", DEBUG);
        StringBuilder FOXML = con.read(connection);
        List<String> childrenUuids = parser.getFOXMLChildrenUuids(FOXML);



        if (childrenUuids)
    }*/



    private List <String> getRootPidsForPrivacyChange(List<String> inputRootPids){
        List<String> result = new ArrayList<>();
        //TODO u kazdeho pidu v parametru inputRootPids zjistit, jestli ma nejakeho childa s vetsim rokem, nez 1900
        //TODO output bude root pids, ktere se maji cele zmenit

        boolean privacyOfWholeDocument = false;
        for (String rootPid:inputRootPids){
            //privacyOfWholeDocument = isWholeDocPrivate(rootPid);

        }

        return result;
    }

    private List<String> getRootPidsWithoutPeriodicals(List<String> inputPids){
        List <String> pidsNOTperiodical = new ArrayList<>();
        String rootPid = "";
        String docType = "";
        for (String pid : inputPids){
            rootPid = solrUtils.getSolrParameterByPid(pid, rootPidSolrQ, false);
            docType = solrUtils.getSolrParameterByPid(rootPid, documentTypeSolrQ, false);
            if ((!docType.equals("periodical")) && (!pidsNOTperiodical.contains(rootPid)))
                pidsNOTperiodical.add(rootPid);
        }
        return pidsNOTperiodical;
    }

    public void start(Properties prop) {
        if (K7SolrNames){
            publicationYearSolrQ = "date_range_start.year";
            privacySolrQ = "accessibility";
            documentTypeSolrQ = "model";
            rootPidSolrQ = "root.pid";
        }

        //TODO VSECHNY DOKUMENTY, KTERE NEJSOU PERIODIKA --> ktere nemaji root pid periodical
        //TODO u kazdeho takoveho dokumentu zjistit, jestli to ma child, ktery ma vetsi rok, nez 1900, pokud ne, tak zmenit cely dokument
        //TODO tyto dokumenty, ktere maji nejaky takovy child menit nebudu, jen si vyfiltruju tyto children a ty pak zmenim

        //TODO pridat rocniky typu 199- apod., ktere tam patri take
        String query = publicationYearSolrQ + ":[250 TO 1900] AND " + privacySolrQ + ":private";
        List <String> pidsAllTypes = solrUtils.getPids(query, maxPidsToRead, true); //z tohoto je jeste nutne odfiltrovat ty co maji root pid periodical
        List <String> rootPidsNOTper = getRootPidsWithoutPeriodicals(pidsAllTypes);
        List <String> rootPidsNotPerForPrivacyChange = getRootPidsForPrivacyChange(rootPidsNOTper);

        //fileIO.toOutputFile(rootPidsNOTper, "IO/438/DEBUG.txt");


        //TODO PERIODIKA pokud nebudou mit zadne rocniky, ktere jsou vetsi, nez 1910, tak zmenit cela periodika
        //TODO pridat rocniky typu 190- apod., ktere tam patri take
        query = publicationYearSolrQ + ":[1901 TO 1910] AND " + documentTypeSolrQ + ":periodical AND " + privacySolrQ + ":private";
        List<String> pidsPer = solrUtils.getPids(query, maxPidsToRead, true);

        //TODO periodika, ktere budou mit nejake takove rocniky, tak u nich jen zmenim rocniky
        query = publicationYearSolrQ + ":[1901 TO 1910] AND " + documentTypeSolrQ + ":periodicalvolume AND " + privacySolrQ + ":private";
        List<String> pidsPerVol = solrUtils.getPids(query, maxPidsToRead, true);

    }
}
