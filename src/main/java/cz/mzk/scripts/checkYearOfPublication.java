package cz.mzk.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cz.mzk.services.FileIO;
import cz.mzk.services.SdnntConnNEW;
import cz.mzk.services.SolrUtils;

/*
    Author: Marek Sedlo
    Description:
    Script for issue 493
    this script DOES NOT check if it has no licence and should have, it just checks if existing licenses dnntt and dnnto are same in the SDNNT
*/

public class checkYearOfPublication implements Script {
    private List<String> LOG = new ArrayList<>();
    FileIO fileService = new FileIO();
    SolrUtils solrConn = new SolrUtils();
    List<String> DNNTO = new ArrayList<>();
    List<String> DNNTT = new ArrayList<>();
    List<String> COVID = new ArrayList<>();
    List<String> forReindex = new ArrayList<>();
    StringBuilder results = new StringBuilder("");


    @Override
    public void start(Properties prop) {
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
        logDocsForReindex();
        System.out.println(results);
    }

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
                //LOG.add(uuid);
            if (licences.contains("dnntt")){
                shouldBeDnntt.add(uuid);
            }
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
                if (licences.contains("dnnto")){
                    shouldBeDnnto.add(uuid);
                }
            }

            LOG.add(doctype + "MZK dnntt NOT in SDNNT");
            LOG.addAll(dnnttNotFound);

            LOG.add(doctype + "MZK dnntt, whose are dnnto in SDNNT");
            LOG.addAll(shouldBeDnnto);
        }

        /*for (String uuid : COVID){
            SdnntConnNEW sdnntConnectionNEW = new SdnntConnNEW(sdnntHost + "?query=" + uuid);
            List<String> licences = sdnntConnectionNEW.getSdnntLicences();
            if (!licences.contains("covid"))
                LOG.add(uuid + " is NOT covid in SDNNT");
        }*/
    }

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
}



















