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
*/

public class checkYearOfPublication implements Script {
    public static final boolean DEBUG = false;
    private List<String> LOG = new ArrayList<>();
    private List<String> removeDNNTO = new ArrayList<>();
    private List<String> removeDNNTT = new ArrayList<>();
    private List<String> addDNNTO = new ArrayList<>();
    private List<String> addDNNTT = new ArrayList<>();
    FileIO fileService = new FileIO();
    SolrUtils solrConn = new SolrUtils();
    List<String> forReindex = new ArrayList<>();


    @Override
    public void start(Properties prop) {
        HashMap<String, Integer> pers = new HashMap<>();
        HashMap<String, Integer> perVols = new HashMap<>();
        HashMap<String, Integer> mons2008 = new HashMap<>();
        HashMap<String, Integer> monUnits2008 = new HashMap<>();
        HashMap<String, Integer> mons2001 = new HashMap<>();
        HashMap<String, Integer> monUnits2001 = new HashMap<>();

        final String sdnntHost = prop.getProperty("SDNNT_HOST_PRIVATE_PART_API_CATALOG");


        //Periodika 2011+ nemaji mit zadny dnnt label, musi byt v soulasu se SDNNT
        String periodicalFrom2011 = "fedora.model:periodical AND datum_begin:[2011 TO *]"; //841
        String periodicalvolumeFrom2011 = "fedora.model:periodicalvolume AND datum_begin:[2011 TO *]"; //1920
        //Monografie 2008+ nemaji mit dnnt-t --> nemaji mit zadny dnnt label, musi byt v souladu se SDNNT
        String monographFrom2008 = "fedora.model:monograph AND datum_begin:[2008 TO *]"; //35007
        String monographunitFrom2008 = "fedora.model:monographunit AND datum_begin:[2008 TO *]"; //1154
        //Monografie 2001+ nemaji mit dnnt-o, musi byt v soulasu se SDNNT
        String monographFrom2001 = "fedora.model:monograph AND datum_begin:[2001 TO *]"; //75498
        String monographunitFrom2001 = "fedora.model:monographunit AND datum_begin:[2001 TO *]"; //2087

        if (!DEBUG){
            pers = getSolrDocuments(periodicalFrom2011);
            perVols = getSolrDocuments(periodicalvolumeFrom2011);
            mons2008 = getSolrDocuments(monographFrom2008);
            monUnits2008 = getSolrDocuments(monographunitFrom2008);
            mons2001 = getSolrDocuments(monographFrom2001);
            monUnits2001 = getSolrDocuments(monographunitFrom2001);
        }

        if (DEBUG){
            pers.put("uuid:f41ba5e1-4bfc-11e1-8bb9-005056a60003", 2003);
            pers.put("uuid:51596ad0-68a3-11e4-8d66-5ef3fc9bb22f", 2003);
            pers.put("uuid:94cd1ef0-0b88-11ea-9e5a-5ef3fc9bb22f", 2012);
            pers.put("uuid:169c1730-3d6f-11e4-bdb5-005056825209", 9999);
            pers.put("uuid:0557fa00-f23f-11e3-97c9-001018b5eb5c", 9999);
            perVols.put("uuid:69016ad0-5eba-11ea-a5e6-005056825209", 2018);
        }

        compareSDNNTlicences(sdnntHost, pers, "periodical");
        compareSDNNTlicences(sdnntHost, perVols, "periodicalvolume");
        compareSDNNTlicences(sdnntHost, mons2008, "monograph");
        compareSDNNTlicences(sdnntHost, monUnits2008, "monographunit");
        compareSDNNTlicences(sdnntHost, mons2001, "monograph");
        compareSDNNTlicences(sdnntHost, monUnits2001, "monographunit");


        fileService.toOutputFile(LOG, "IO/493/LOG");
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

    private void compareSDNNTlicences(String sdnntHost, HashMap<String, Integer> docsToCompare, String fedoraModel){
        removeDNNTO.clear();
        removeDNNTT.clear();
        addDNNTO.clear();
        addDNNTT.clear();
        forReindex.clear();

        LOG.add(fedoraModel);
        for (String pid : docsToCompare.keySet()){
            LOG.add("CHECKING UUID: " + pid);
            SdnntConnNEW sdnntConnNEW = findDocInSDNNT(sdnntHost, pid);
            if (sdnntConnNEW != null) //document found in SDNNT
                if (sdnntConnNEW.getDocsFound() > 1)
                    LOG.add("SDNNT response has multiple docs found for this doc: " + pid);
                else {
                    if (isSdnntFoundDocSame(sdnntConnNEW.getJsonResponse(), pid)){
                        LOG.add("Documents matches!");
                        //compare licences
                        List<String> SDNNTlicences = new ArrayList<>();
                        if (fedoraModel.equals("periodical") || fedoraModel.equals("monograph")) //it's root
                            SDNNTlicences = sdnntConnNEW.getSdnntLicences(true);
                        else //it's child
                            SDNNTlicences = sdnntConnNEW.getSdnntLicences(false);
                        String MZKlics = solrConn.getSolrParameterByPid(pid, "dnnt-labels");
                        compareLics(SDNNTlicences, MZKlics, pid, docsToCompare.get(pid));
                    }
                }
        }
        fileService.toOutputFile(removeDNNTO, "IO/493/removeDNNTO" + fedoraModel + ".txt");
        fileService.toOutputFile(removeDNNTT, "IO/493/removeDNNTT" + fedoraModel + ".txt");
        fileService.toOutputFile(addDNNTO, "IO/493/addDNNTO" + fedoraModel + ".txt");
        fileService.toOutputFile(addDNNTT, "IO/493/addDNNTT" + fedoraModel + ".txt");
        fileService.toOutputFile(forReindex, "IO/493/forReindex" + fedoraModel + ".txt");
    }

    private void compareLics(List<String> SDNNTlics, String MZKlics, String uuid, int datum_begin){
        List<String> MZKlicenses = new ArrayList<>();
        if (!Objects.equals(MZKlics, "null"))
            MZKlicenses = makeList(MZKlics);

        if (SDNNTlics.isEmpty()){
            if (!MZKlicenses.isEmpty()){ //MZK got license and SDNNT not --> remove MZK license
                LOG.add("No SDNNT license for MZK licensed doc: " + uuid + " with MZK license: " + MZKlicenses);
                if (MZKlicenses.contains("dnnto")){
                    if (datum_begin == 9999)
                        forReindex.add(uuid);
                    removeDNNTO.add(uuid);
                }
                if (MZKlicenses.contains("dnntt")){
                    if (datum_begin == 9999)
                        forReindex.add(uuid);
                    removeDNNTT.add(uuid);
                }
            }

        } else {
            for (String lic : SDNNTlics){
                if (!MZKlicenses.contains(lic)){//SDNNT got license and MZK not --> add MZK license
                    LOG.add("No MZK license " + lic + ", which was found in SDNNT licenses for " + uuid);
                    if (lic.equals("dnnto")){
                        if (datum_begin == 9999)
                            forReindex.add(uuid);
                        addDNNTO.add(uuid);
                    }
                    if (lic.equals("dnntt")){
                        if (datum_begin == 9999)
                            forReindex.add(uuid);
                        addDNNTT.add(uuid);
                    }
                }
            }
        }
    }

    private List<String> makeList(String strToList){
        List<String> result = new ArrayList<>();
        if (!strToList.contains(" ")){ //strToList is one item
            String modifiedStr = strToList.replace("\n", "");
            result.add(modifiedStr);
        }

        else {
            String modifiedStr = strToList.replace("[", "");
            modifiedStr = modifiedStr.replace("]", "");
            modifiedStr = modifiedStr.replace(" ", "");
            modifiedStr = modifiedStr.replace("\n", "");
            result = new ArrayList<String>(Arrays.asList(modifiedStr.split(",")));
        }
        return result;
    }


    //response is SDNNT response
    //pid is from SOLR
    // its rly basic check
    private boolean isSdnntFoundDocSame(String response, String pid){
        if (response.contains(pid)) // SDNNT response contains SOLR pid
            return true;
        else {
            String solrDocName = solrConn.getSolrParameterByPid(pid, "root_title"); //periodical volume can contain an empty title
            if (response.contains(solrDocName))
                return true;
            else{
                LOG.add("Documents does NOT match");
                return false;
            }
        }
    }


    // returns sdnntConnNEW with found document
    // if document is not found, than returns null
    private SdnntConnNEW findDocInSDNNT(String sdnntHost, String pid){
        SdnntConnNEW sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + pid);
        boolean docFound = false;
        //check for uuid in SDNNT
        if (sdnntConnNEW.isDocumentFound()){
            LOG.add("UUID found in SDNNT!");
            docFound = true;
        }
        else {
            LOG.add("UUID NOT found in SDNNT! ");

            String cnb = getNewIdentifierFromSolr(pid, "cnb"); //this logs info about finding CNB in solr
            assert (cnb != null);
            if (isCnbFoundInSolr(cnb)){
                sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + cnb);
                //check for cnb in SDNNT
                if (sdnntConnNEW.isDocumentFound()){
                    LOG.add("CNB found in SDNNT! " + cnb);
                    docFound = true;
                }
                else { //cnb not found in SDNNT, try issn
                    LOG.add("CNB NOT found in SDNNT! " + cnb);
                    String issn = getNewIdentifierFromSolr(pid, "issn"); //this logs info about finding ISSN in solr
                    assert (issn != null);
                    if (isIssnNotEmptyInSolr(issn)){
                        sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + issn);
                        //check for issn in SDNNT
                        if (sdnntConnNEW.isDocumentFound()){
                            LOG.add("ISSN found in SDNNT! " + issn);
                            docFound = true;
                        }
                    }
                }
            }
            else { // cnb not found in solr, try issn
                String issn = getNewIdentifierFromSolr(pid, "issn"); //this logs info about finding ISSN in solr
                assert (issn != null);
                if (isIssnNotEmptyInSolr(issn)){
                    sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + issn);
                    //check for issn in SDNNT
                    if (sdnntConnNEW.isDocumentFound()){
                        LOG.add("ISSN found in SDNNT! " + issn);
                        docFound = true;
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
                LOG.add("SOLR CNB NOT found!");
            else
                LOG.add("SOLR CNB found! " + cnb);
            return cnb;
        }
        else if (identifier.equals("issn")){
            String issn = solrConn.getSolrParameterByPid(pid, "issn");
            if (issn.equals("\n"))
                LOG.add("SOLR ISSN EMPTY! ");
            else
                LOG.add("SOLR ISSN found! " + issn);
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
        result = result.replaceAll("\\s+",""); //try to remove all whitespaces
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


    TODO PRIPADNE PRIDAT KONTROLU PODLE ISBN - a mozna taky ne, kdyz na miste issn dane isbn
    TODO pridat kontrolu podle nazvu

 */


















