package cz.mzk.services;

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

    public String getPids(String q){ //TODO should return List<String>, max 50000 pids
        StringBuilder pids = new StringBuilder();
        //List<String> pids = new ArrayList<>();
        SolrQuery query = new SolrQuery();
        query.setQuery(q);
        query.addField("PID");
        query.add("rows", "50000");

        try {
            QueryResponse response = solr.query(query);
            SolrDocumentList docList = response.getResults();
            for (org.apache.solr.common.SolrDocument entries : docList) {
                pids.append(entries.getFieldValue("PID"));
                pids.append("\n");
            }
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
        return pids.toString();
    }
}
