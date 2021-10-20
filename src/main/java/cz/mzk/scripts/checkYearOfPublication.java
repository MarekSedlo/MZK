package cz.mzk.scripts;

import java.util.*;

import cz.mzk.services.FileIO;
import cz.mzk.services.SdnntConnNEW;
import cz.mzk.services.SolrUtils;
import org.yaml.snakeyaml.events.Event;

/*
    Author: Marek Sedlo
    Description:
    Script for issue 493
    this script DOES NOT check if it has no licence and should have, it just checks if existing licenses dnntt and dnnto are same in the SDNNT
    TODO ATTENTION!!!!!!!!!!!!!! Pozor, tenhle skript taha data ze spatneho mista v SDNNT! PREDELAT!!!
*/

public class checkYearOfPublication implements Script {
    public static final boolean DEBUG = false;
    private List<String> LOG = new ArrayList<>();
    FileIO fileService = new FileIO();
    SolrUtils solrConn = new SolrUtils();
    /*List<String> DNNTO = new ArrayList<>();
    List<String> DNNTT = new ArrayList<>();
    List<String> COVID = new ArrayList<>();*/
    List<String> forReindex = new ArrayList<>();
    StringBuilder results = new StringBuilder("");


    @Override
    public void start(Properties prop) {
        HashMap<String, Integer> pers = new HashMap<>();
        HashMap<String, Integer> perVols = new HashMap<>();
        HashMap<String, Integer> mons = new HashMap<>();
        HashMap<String, Integer> monUnits = new HashMap<>();

        final String sdnntHost = prop.getProperty("SDNNT_HOST_PRIVATE_PART_API_CATALOG");


        //Periodika 2011+ nemaji mit zadny dnnt label, musi byt v soulasu se SDNNT
        String periodicalFrom2011 = "fedora.model:periodical AND datum_begin:[2011 TO *]";
        String periodicalvolumeFrom2011 = "fedora.model:periodicalvolume AND datum_begin:[2011 TO *]";
        //Monografie 2008+ nemaji mit dnnt-t --> nemaji mit zadny dnnt label, musi byt v souladu se SDNNT
        String monographFrom2008 = "fedora.model:monograph AND datum_begin:[2008 TO *]";
        String monographunitFrom2008 = "fedora.model:monographunit AND datum_begin:[2008 TO *]";
        //Monografie 2001+ nemaji mit dnnt-o, musi byt v soulasu se SDNNT
        String monographFrom2001 = "fedora.model:monograph AND datum_begin:[2001 TO *]";
        String monographunitFrom2001 = "fedora.model:monographunit AND datum_begin:[2001 TO *]";


        results.append("\nPeriodical 2011+\n");
        if (!DEBUG){
            pers = getSolrDocuments(periodicalFrom2011);
            //perVols = getSolrDocuments(periodicalvolumeFrom2011);
        }

        if (DEBUG){
            pers.put("uuid:f41ba5e1-4bfc-11e1-8bb9-005056a60003", 2003);
            pers.put("uuid:51596ad0-68a3-11e4-8d66-5ef3fc9bb22f", 2003);
            pers.put("uuid:94cd1ef0-0b88-11ea-9e5a-5ef3fc9bb22f", 2012);
            pers.put("uuid:169c1730-3d6f-11e4-bdb5-005056825209", 9999);
            pers.put("uuid:0557fa00-f23f-11e3-97c9-001018b5eb5c", 9999);
        }

        compareSDNNTlicences(sdnntHost, pers);


        /*results.append("\nPeriodical 2011+\n");
        getDnntDocuments(periodicalFrom2011);
        checkPidsInSDNNT(sdnntHost, true, "Periodical 2011+\n");

        results.append("Periodicalvolume 2011+\n");
        getDnntDocuments(periodicalvolumeFrom2011);
        checkPidsInSDNNT(sdnntHost, true, "Periodicalvolume 2011+\n");


        results.append("Monograph 2008+\n");
        getDnntDocuments(monographFrom2008);
        checkPidsInSDNNT(sdnntHost, true, "Monograph 2008+\n");

        results.append("Monographunit 2008+\n");
        getDnntDocuments(monographunitFrom2008);
        checkPidsInSDNNT(sdnntHost, true, "Monographunit 2008+\n");


        results.append("Monograph 2001+\n");
        getDnntDocuments(monographFrom2001);
        checkPidsInSDNNT(sdnntHost, false, "Monograph 2001+\n");

        results.append("Monographunit 2001+\n");
        getDnntDocuments(monographunitFrom2001);
        checkPidsInSDNNT(sdnntHost, false, "Monographunit 2001+\n");


        fileService.toOutputFile(LOG, "IO/493/LOG");
        logDocsForReindex();*/
        System.out.println(results);
    }

    private HashMap<String, Integer> getSolrDocuments(String solrQuery){
        HashMap<String, Integer> result = new HashMap<>();
        List<String> pids = solrConn.getPids(solrQuery);

        for (String pid : pids){
            String datumBegin = solrConn.getSolrParameterByPid(pid, "datum_begin");
            datumBegin = datumBegin.replace("\n", "");
            result.put(pid, Integer.parseInt(datumBegin));
        }
        return result;
    }

    private void compareSDNNTlicences(String sdnntHost, HashMap<String, Integer> docsToCompare){
        for (String pid : docsToCompare.keySet()){
            SdnntConnNEW sdnntConnNEW = findDocInSDNNT(sdnntHost, pid);
            if (sdnntConnNEW != null) //document was not found in SDNNT
                if (sdnntConnNEW.getJsonResponse().contains(pid)) //result is probably right, here check licences
                    results.append("SDNNT result contains pid: ").append(pid).append("\n");
                else
                    results.append("SDNNT result NOT contains pid: ").append(pid).append("\n");
        }
    }


    // returns sdnntConnNEW with found document
    // if document is not found, than returns null
    private SdnntConnNEW findDocInSDNNT(String sdnntHost, String pid){
        SdnntConnNEW sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + pid);
        boolean docFound = false;
        //check for uuid in SDNNT
        if (sdnntConnNEW.isDocumentFound()){
            results.append("UUID found in SDNNT! ").append(pid).append("\n");
            docFound = true;
        }
        else {
            results.append("UUID NOT found in SDNNT! ").append(pid).append("\n");

            String cnb = getNewIdentifierFromSolr(pid, "cnb"); //this logs info about finding CNB in solr
            assert (cnb != null);
            if (isCnbFoundInSolr(cnb)){
                sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + cnb);
                //check for cnb in SDNNT
                if (sdnntConnNEW.isDocumentFound()){
                    results.append("CNB found in SDNNT! ").append(cnb).append("\n");
                    docFound = true;
                }
                else {
                    results.append("CNB NOT found in SDNNT! ").append(cnb).append("\n");
                    String issn = getNewIdentifierFromSolr(pid, "issn"); //this logs info about finding ISSN in solr
                    assert (issn != null);
                    if (isIssnNotEmptyInSolr(issn)){
                        sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + issn);
                        //check for issn in SDNNT
                        if (sdnntConnNEW.isDocumentFound()){
                            results.append("ISSN found in SDNNT! ").append(issn);
                            docFound = true;
                        }
                    }
                }
            }
        }
        if (docFound)
            return sdnntConnNEW;
        else
            return null;
    }

    private boolean isCnbFoundInSolr(String cnb){
        return !cnb.equals("CNB NOT found");
    }

    private boolean isIssnNotEmptyInSolr(String issn){
        return !issn.equals("\n");
    }

    private String getNewIdentifierFromSolr(String pid, String identifier){
        if (identifier.equals("cnb")){
            String IDs = solrConn.getSolrParameterByPid(pid, "dc.identifier");
            String cnb = parseCnb(IDs);
            if (cnb.equals("CNB NOT found"))
                results.append("SOLR CNB NOT found! ").append("\n");
            else
                results.append("SOLR CNB found! ").append(cnb).append("\n");
            return cnb;
        }
        else if (identifier.equals("issn")){
            String issn = solrConn.getSolrParameterByPid(pid, "issn");
            if (issn.equals("\n"))
                results.append("SOLR ISSN EMPTY! ").append("\n");
            else
                results.append("SOLR ISSN found! ").append(issn);
            return issn;
        }
        else
            return null;
    }

    private String parseCnb (String IDs){
        String separatorBegin = "ccnb:";
        String separatorEnd = ",";
        int objIndex = IDs.indexOf(separatorBegin);
        if (objIndex == -1){
            separatorBegin = "cnb";
            objIndex = IDs.indexOf(separatorBegin);
            if (objIndex == -1)
                return "CNB NOT found";
        }

        if (separatorBegin.equals("ccnb:"))
            objIndex += separatorBegin.length();
        String result = IDs.substring(objIndex);
        result = result.split(separatorEnd)[0];
        result = result.replaceAll("\\s+",""); //remove all whitespaces
        if (result.charAt(0) != 'c'){
            StringBuilder sb = new StringBuilder();
            boolean cnbStartFound = false;
            for (int i=0; i<result.length();i++){
                if (result.charAt(i) == 'c')
                    cnbStartFound = true;
                if (cnbStartFound)
                    sb.append(result.charAt(i));
            }
            result = sb.toString();
        }
        return result;
    }

    /*

    //expecting data in DNNT lists
    private void checkPidsInSDNNT(String sdnntHost, boolean checkDNNTT, String doctype){
        //SdnntConnNEW sdnntConnectionNEW = new SdnntConnNEW("https://195.113.133.53/sdnnt/api/v1.0/catalog?query=uuid:df939db0-c44f-11e2-8b87-005056827e51");
        List<String> dnntoNotFound = new ArrayList<>();
        List<String> shouldBeDnntt = new ArrayList<>();

        for (String uuid : DNNTO){
            SdnntConnNEW sdnntConnectionNEW = new SdnntConnNEW(sdnntHost + "?query=" + uuid);
            List<String> licences = sdnntConnectionNEW.getSdnntLicences();
            if (!licences.contains("dnnto"))
                dnntoNotFound.add(uuid);
            else
                results.append(uuid); //DEBUG
            if (licences.contains("dnntt"))
                shouldBeDnntt.add(uuid);
            else
                results.append(uuid); //DEBUG
        }

        LOG.add(doctype + "MZK dnnto NOT in SDNNT");
        LOG.addAll(dnntoNotFound);

        LOG.add(doctype + "MZK dnnto, whose are dnntt in SDNNT");
        LOG.addAll(shouldBeDnntt);

        List<String> dnnttNotFound = new ArrayList<>();
        List<String> shouldBeDnnto = new ArrayList<>();

        if (checkDNNTT){
            for (String uuid : DNNTT){
                SdnntConnNEW sdnntConnectionNEW = new SdnntConnNEW(sdnntHost + "?query=" + uuid);
                List<String> licences = sdnntConnectionNEW.getSdnntLicences();
                if (!licences.contains("dnntt"))
                    dnnttNotFound.add(uuid);
                else
                    results.append(uuid); //DEBUG
                if (licences.contains("dnnto"))
                    shouldBeDnnto.add(uuid);
                else
                    results.append(uuid); //DEBUG
            }

            LOG.add(doctype + "MZK dnntt NOT in SDNNT");
            LOG.addAll(dnnttNotFound);

            LOG.add(doctype + "MZK dnntt, whose are dnnto in SDNNT");
            LOG.addAll(shouldBeDnnto);
        }

        //for (String uuid : COVID){
        //    SdnntConnNEW sdnntConnectionNEW = new SdnntConnNEW(sdnntHost + "?query=" + uuid);
        //    List<String> licences = sdnntConnectionNEW.getSdnntLicences();
        //    if (!licences.contains("covid"))
        //        LOG.add(uuid + " is NOT covid in SDNNT");
        //}
    }



    // gets dnnt documents from SOLR, dnnt documents are stored in DNNTO, DNNTT, COVID lists
    private void getDnntDocuments(String queryPrefix){
        DNNTO.clear();
        DNNTT.clear();
        COVID.clear();

        DNNTO = solrConn.getPids(queryPrefix + " AND dnnt-labels:dnnto");
        DNNTT = solrConn.getPids(queryPrefix + " AND dnnt-labels:dnntt");
        COVID = solrConn.getPids(queryPrefix + " AND dnnt-labels:covid");

        results.append("DNNTO: ").append(DNNTO.size()).append("\n");
        results.append("DNNTT: ").append(DNNTT.size()).append("\n");
        results.append("COVID: ").append(COVID.size()).append("\n");
    }

    private void logDocsForReindex(){
        for (String line : LOG){
            if (line.startsWith("uuid:")){
                String datum = solrConn.getSolrParameterByPid(line, "datum_begin");
                if (datum.equals("9999"))
                    forReindex.add(line);
            }
        }
        fileService.toOutputFile(forReindex, "IO/493/forReindex.txt");
    }


    */

}


/*
    postup
    1. najdu v solru uuid s danymi roky vydani, roztridit do kategorii podle typu dokumentu
    (mozna si to ukladat do dictionary, [uuid : rok_vydani] - kvuli pozdejsi kontrole roku 9999)
        1.1 ASI RADEJI UDELAT AZ V BODE 4
        Zkontrolovat, ze rok vydani neni 9999, pripadne takove dokumenty reindexovat
        //TODO da se to reindexovat v krameriu, ale nasel jsem 791 rootu perodik, ktere maji 9999 datum_begin
    2. pri pruchodu si udelam funkci, ktera si pro dane uuid vyhleda a vyparsuje ze solru cnb a issn
    3. prochazim bud cele periodikum nebo rocnik s danym rokem vydani (to stejne pro monografie),
        3.1 musi byt vypsano v logu jaky typ dokumentu menim, tzn. fedora.model
        3.2 pokud hledam root, tak do SDNNT vyzkouset dat uuid
            3.2.1 kdyz nenajde, tak cnb bez prefixu
            3.2.2 kdyz nenajde, tak issn bez prefixu
        3.3 porovnat licence nase a v SDNNT pro dany root dokument, najit licence a states (podivat se asi rovnou i do pids a granularity),
            mel bych sledovat licence i states, pokud je tam states:N, tak by tam pravdepodobne nemelo byt licence,
            zkontrolovat i ohledne toho, ze ve states muze byt pravdepodobne vice polozek
        3.4 pokud hledam child, tak do SDNNT opet vyzkouset dat uuid, cnb, issn
            3.4.1 NAJDE SE JEHO ROOT!!! Takze najit v granularity link, ktery konci s uuid childu (nenalezen vypsat), pozor je tam i root
            3.4.2 v nalezenem childu v granularity porovnat licence podobne jako v kroku 3.3
    4. Zkontrolovat roky vydani vsech dokumentu, ktere budu chtit menit,
            pokud budou 9999, tak zjistit kolik jakych typu dokumentu je potreba reindexovat

    5. fofola
        5.1 pokud menim root, tak odskrtnou checkbar, abych zmenil opravdu jen root
        5.2 pokud menim child (rocnik), tak zaskrtnou checkbar, abych zmenil cely child i s jeho dalsimi potomky

    poznamka: states:N znamena nezarazeno, neni na seznamu
    poznamka: pids pravdepodobne znamena pids, ktere maji stejny dnnt jako root - OVERIT!
    poznamka: granularity pravdepodobne znamena, ze jsou v dokumentu rocniky s ruznymi licencemi - OVERIT!
    poznamka: po porade s Romanem si vypisu i ty dokumenty, kde by se mel zrusit covid, alespon do souboru
                a pripadne je zacnu menit
 */


















