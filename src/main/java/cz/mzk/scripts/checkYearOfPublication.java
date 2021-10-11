package cz.mzk.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cz.mzk.services.SdnntConnNEW;
import cz.mzk.services.SdnntConnectionOLD;
import cz.mzk.services.SolrUtils;

/*
    Author: Marek Sedlo
    Description:
    Script for issue 493
*/

public class checkYearOfPublication implements Script {
    SolrUtils solrConn = new SolrUtils();
    List<String> DNNTO = new ArrayList<>();
    List<String> DNNTT = new ArrayList<>();
    List<String> COVID = new ArrayList<>();
    StringBuilder results = new StringBuilder("");


    @Override
    public void start(Properties prop) {
        final String sdnntHost = prop.getProperty("SDNNT_HOST_PRIVATE_PART_API_CATALOG");
        checkPidsInSDNNT(sdnntHost);



        //Periodika 2011+ nemaji mit zadny dnnt label, musi byt v soulasu se SDNNT
        String periodicalFrom2011 = "fedora.model:periodical AND rok:[2011 TO *]";
        String periodicalvolumeFrom2011 = "fedora.model:periodicalvolume AND rok:[2011 TO *]";
        //Monografie 2008+ nemaji mit dnnt-t --> nemaji mit zadny dnnt label, musi byt v souladu se SDNNT
        String monographFrom2008 = "fedora.model:monograph AND rok:[2008 TO *]";
        String monographunitFrom2008 = "fedora.model:monographunit AND rok:[2008 TO *]";
        //Monografie 2001+ nemaji mit dnnt-o, musi byt v soulasu se SDNNT
        String monographFrom2001 = "fedora.model:monograph AND rok:[2001 TO *]";
        String monographunitFrom2001 = "fedora.model:monographunit AND rok:[2001 TO *]";



        results.append("\nPeriodical 2011+\n");
        getDnntDocuments(periodicalFrom2011);
        results.append("Periodicalvolume 2011+\n");
        getDnntDocuments(periodicalvolumeFrom2011);

        results.append("Monograph 2008+\n");
        getDnntDocuments(monographFrom2008);
        results.append("Monographunit 2008+\n");
        getDnntDocuments(monographunitFrom2008);

        results.append("Monograph 2001+\n");
        getDnntDocuments(monographFrom2001);
        results.append("Monographunit 2001+\n");
        getDnntDocuments(monographunitFrom2001);

        System.out.println(results);

    }

    private void checkPidsInSDNNT(String sdnntHost){
        //SdnntConnNEW sdnntConnectionNEW = new SdnntConnNEW("https://195.113.133.53/sdnnt/api/v1.0/catalog?query=000197071&fullCatalog=false&rows=20&page=0");
        SdnntConnNEW sdnntConnectionNEW = new SdnntConnNEW("https://195.113.133.53/sdnnt/api/v1.0/catalog?query=uuid:df939db0-c44f-11e2-8b87-005056827e51");
        //SdnntConnNEW sdnntConnectionNEW = new SdnntConnNEW(sdnntHost + "?query=uuid%3Adf939db0-c44f-11e2-8b87-005056827e51&fullCatalog=false&rows=20&page=0");
        List<String> licences = sdnntConnectionNEW.getSdnntLicences();
        System.out.println(licences);
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

}