package cz.mzk.scripts;

import java.util.Properties;
import cz.mzk.services.SolrUtils;

/*
    Author: Marek Sedlo
    Description:
    Script for issue 493
*/

public class checkYearOfPublication implements Script {
    @Override
    public void start(Properties prop) {
        SolrUtils solrConn = new SolrUtils();
        //Periodika 2011+ nemaji mit zadny dnnt label
        String periodicalFrom2011 = "fedora.model:periodical AND rok:[2011 TO *]";

        String dnntO = solrConn.getPids(periodicalFrom2011 + "AND dnnt-labels:dnnto");
        String dnntT = solrConn.getPids(periodicalFrom2011 + "AND dnnt-labels:dnntt");
        String covid = solrConn.getPids(periodicalFrom2011 + "AND dnnt-labels:covid");

        System.out.println("dnntO:\n" + dnntO);
        System.out.println("dnntT:\n" + dnntT);
        System.out.println("covid:\n" + covid);
    }
}


