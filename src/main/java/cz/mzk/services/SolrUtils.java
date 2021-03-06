package cz.mzk.services;

import cz.mzk.KrameriusVersion;
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
    private String solrPidFieldName;

    public SolrUtils(KrameriusVersion krameriusVersion){
        try {
            prop = properties.getPropValues();
        } catch (IOException e){
            System.err.println("ERROR: cannot read config.properties");
            e.printStackTrace();
        }
        String solrHost = prop.getProperty("SOLR_HOST_K5");
        solrPidFieldName = "PID";
        if (krameriusVersion == KrameriusVersion.K7){
            solrHost = prop.getProperty("SOLR_HOST_K7_search");
            solrPidFieldName = "pid";
        }
        solr = new HttpSolrClient.Builder(solrHost).build();
        solr.setParser(new XMLResponseParser());
    }

    //this function adds newline right after the parameter
    public String getSolrParameterByPid(String pid, String parameter, boolean addNewlineForEachResult){
        StringBuilder result = new StringBuilder();
        SolrQuery query = new SolrQuery();
        String uuid = pid;

        if (pid.startsWith("uuid:")) //remove prefix if exists
            uuid = pid.substring(5);

        //query.setQuery("PID:\"uuid:" + uuid + "\"").setRows(1);
        query.setQuery(solrPidFieldName + ":\"uuid:" + uuid + "\"");
        query.addField(parameter);

        try {
            QueryResponse response = solr.query(query);
            SolrDocumentList docList = response.getResults();
            for (org.apache.solr.common.SolrDocument entries : docList) {
                result.append(entries.getFieldValue(parameter));
                if (entries.getFieldValue(parameter) != null){
                    if (addNewlineForEachResult)
                        result.append("\n");
                }

            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return result.toString();
    }

    // this function returns list of uuids by given query parameter
    // query parameter is the same string like in solr web GUI
    //

    public List<String> getPids(String q, int maxPids, boolean DEBUG){
        //int maximumPids = 1600000;
        List<String> allPids = new ArrayList<>();
        final int maxBatchSize = 50000;
        int start = 0;
        List<String> batchPids;
        if (maxPids < maxBatchSize)
            batchPids = getPidsBatch(q, start, maxPids);
        else
            batchPids = getPidsBatch(q, start, maxBatchSize);
        allPids.addAll(batchPids);

        while (batchPids.size() == maxBatchSize){
            start += maxBatchSize;
            batchPids.clear();
            batchPids = getPidsBatch(q, start, maxBatchSize);
            allPids.addAll(batchPids);

            if (DEBUG)
                System.out.println(start + " done, Maximum pids: " + maxPids);

            if (allPids.size() >= maxPids)
                break;
        }

        if (DEBUG)
            System.out.println(allPids.size() + " done, Maximum pids: " + maxPids);

        return allPids;
    }

    //subfunction of function getPids
    //one call of this function is one solr query
    //returns batch of pids
    private List<String> getPidsBatch(String q, int start, int rows){
        List<String> pidsBatch = new ArrayList<>();
        SolrQuery query = new SolrQuery();
        query.setQuery(q);
        query.addField(solrPidFieldName);
        query.add("start", Integer.toString(start));
        query.add("rows", Integer.toString(rows));
        try {
            QueryResponse response = solr.query(query);
            SolrDocumentList docList = response.getResults();
            for (org.apache.solr.common.SolrDocument entries : docList) {
                pidsBatch.add((String)entries.getFieldValue(solrPidFieldName));
            }
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
        return pidsBatch;
    }

    public boolean pidExistsInSolr(String pid){
        String pidFound = getSolrParameterByPid(pid, solrPidFieldName, false);
        return !pidFound.isEmpty();
    }
}



























