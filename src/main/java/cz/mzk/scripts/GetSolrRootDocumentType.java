package cz.mzk.scripts;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.Properties;

public class GetSolrRootDocumentType implements Script{
    @Override
    public void start(Properties prop) {
        String url = "http://localhost:8983/solr/kramerius";
        HttpSolrClient solr = new HttpSolrClient.Builder(url).build();
        solr.setParser(new XMLResponseParser());

        SolrQuery query = new SolrQuery();
        query.setQuery("PID:\"uuid:899d36ae-d7f0-11e1-967e-0050569d679d\"").setRows(1);
        query.addField("root_pid");

        try {
            QueryResponse response = solr.query(query);
            SolrDocumentList docList = response.getResults();
            System.out.println("root PID: " + docList.get(0).getFieldValue("root_pid"));

            query.setQuery("PID:\"" + docList.get(0).getFieldValue("root_pid") + "\"").setRows(1);
            query.addField("fedora.model");

            response = solr.query(query);
            docList = response.getResults();
            System.out.println("root document type: " + docList.get(0).getFieldValue("fedora.model"));

            if (docList.get(0).getFieldValue("fedora.model").equals("periodical"))
                System.out.println(true);

        } catch (Exception e){
            e.printStackTrace();
        }

    }
}
