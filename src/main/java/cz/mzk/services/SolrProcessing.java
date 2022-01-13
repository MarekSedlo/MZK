package cz.mzk.services;

import cz.mzk.KrameriusVersion;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class SolrProcessing {
    HttpSolrClient solr;
    private static final GetPropertyValues properties = new GetPropertyValues();
    private static Properties prop;

    public SolrProcessing(KrameriusVersion krameriusVersion){
        try {
            prop = properties.getPropValues();
        } catch (IOException e){
            System.err.println("ERROR: cannot read config.properties");
            e.printStackTrace();
        }
        String solrHost = prop.getProperty("SOLR_HOST_K7_processing");
        assert (krameriusVersion == KrameriusVersion.K7);
        solr = new HttpSolrClient.Builder(solrHost).build();
        solr.setParser(new XMLResponseParser());
    }

    private boolean isChildRelation(String relation){
        List<String> childrenRelations = Arrays.asList("haspage", "hasitem",
                "hasintcomppart", "hasvolume", "hasunit", "containstrack", "hassoundunit", "contains");
        if (relation == null)
            return false;
        else
            return childrenRelations.contains(relation.toLowerCase());
    }

    //expects, that param pid exists in fedora
    //returns children of inputPid, max 50000 children
    public List<String> getChildrenOfCurrentPid(String inputPid){
        String pid = inputPid;
        if (!inputPid.startsWith("uuid:"))
            pid = "uuid:" + inputPid;
        List<String> children = new ArrayList<>();
        SolrQuery query = new SolrQuery();
        query.setQuery("source:\"" + pid + "\"");
        query.addField("relation");
        query.addField("targetPid");
        int start = 0;
        int rows = 50000;
        query.add("start", Integer.toString(start));
        query.add("rows", Integer.toString(rows));
        try {
            QueryResponse response = solr.query(query);
            SolrDocumentList docList = response.getResults();
            for (org.apache.solr.common.SolrDocument entries : docList) {
                String relation = ((String)entries.getFieldValue("relation"));
                if (isChildRelation(relation))
                    children.add(((String)entries.getFieldValue("targetPid")));
            }
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
        return children;
    }
}

/*
    je chidlren, kdyz ma relaci, ktera je
    hasPage
    hasItem
    hasIntCompPart (asi, ze je clanka pouzita v article, pocitam to jako potomka taky)
    hasVolume
    hasUnit
    containsTrack
    hasSoundUnit
    contains
 */
