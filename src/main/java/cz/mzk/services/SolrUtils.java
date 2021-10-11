package cz.mzk.services;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SolrUtils {
    HttpSolrClient solr;
    private static final GetPropertyValues properties = new GetPropertyValues();
    private static Properties prop;

    public SolrUtils(){
        try {
            prop = properties.getPropValues();
        } catch (IOException e){
            System.err.println("ERROR: cannot read config.properties");
            e.printStackTrace();
        }
        String solrHost = prop.getProperty("SOLR_HOST");
        solr = new HttpSolrClient.Builder(solrHost).build();
        solr.setParser(new XMLResponseParser());
    }

    public String getSolrParameterByPid(String pid, String parameter){
        StringBuilder result = new StringBuilder();
        SolrQuery query = new SolrQuery();
        //query.setQuery("PID:\"uuid:" + pid + "\"").setRows(1);
        query.setQuery("PID:\"uuid:" + pid + "\"");
        query.addField(parameter);

        try {
            QueryResponse response = solr.query(query);
            SolrDocumentList docList = response.getResults();
            for (org.apache.solr.common.SolrDocument entries : docList) {
                result.append(entries.getFieldValue(parameter));
                result.append("\n");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return result.toString();
    }

    // this function returns list of uuids by given query parameter
    // query parameter is the same string like in solr web GUI

    public List<String> getPids(String q){
        List<String> allPids = new ArrayList<>();
        final int maxBatchSize = 50000;
        int start = 0;
        List<String> batchPids = getPidsBatch(q, start, maxBatchSize);
        allPids.addAll(batchPids);

        while (batchPids.size() == maxBatchSize){
            start += maxBatchSize;
            batchPids.clear();
            batchPids = getPidsBatch(q, start, maxBatchSize);
            allPids.addAll(batchPids);
        }
        return allPids;
    }

    //subfunction of function getPids
    //one call of this functions is one solr query
    //returns batch of pids
    private List<String> getPidsBatch(String q, int start, int rows){
        List<String> pidsBatch = new ArrayList<>();
        SolrQuery query = new SolrQuery();
        query.setQuery(q);
        query.addField("PID");
        query.add("start", Integer.toString(start));
        query.add("rows", Integer.toString(rows));
        try {
            QueryResponse response = solr.query(query);
            SolrDocumentList docList = response.getResults();
            for (org.apache.solr.common.SolrDocument entries : docList) {
                pidsBatch.add((String)entries.getFieldValue("PID") + "\n");
            }
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
        return pidsBatch;
    }
}