package cz.mzk.services;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SdnntConnNEW {
    private final String sdnntHost;
    private final RestTemplate restTemplate;
    private String jsonResponse;
    //private Map<String,Object> map;
    //private List<Object> list;
    //private boolean isSingleJsonItem = false;

    //query parameter example https://195.113.133.53/sdnnt/api/v1.0/catalog?query=uuid:df939db0-c44f-11e2-8b87-005056827e51
    public SdnntConnNEW(String sdnntHost, String query){
        this.sdnntHost = sdnntHost;
        query = sdnntHost + query;

        String resp = "";

        restTemplate = getRestTemplate();
        String apikey = "29039fda-5065-46aa-8ac8-b8de348c41e9"; //TODO move to config
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/json");
        headers.set("X-API-KEY", apikey);
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(query, HttpMethod.GET, entity, String.class);
        resp = responseEntity.getBody();

        assert resp != null;
        jsonResponse = resp;
    }

    //https://stackoverflow.com/questions/4072585/disabling-ssl-certificate-validation-in-spring-resttemplate
    //code to disable ssl certificate validation
    public RestTemplate getRestTemplate(){
        try {
            TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
            SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
            CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            return restTemplate;
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e){
            e.printStackTrace();
        }
        return null;
    }

    //expecting data in jsonResponse
    public List<String> getSdnntLicences(boolean isRoot, String pid){
        JSONObject jsonObj = new JSONObject(jsonResponse);
        JSONArray docs = jsonObj.getJSONArray("docs");
        JSONArray licences = null;
        ArrayList<String> jsonArrToList = new ArrayList<String>();

        for (int i = 0; i<docs.length(); i++){
            if (isRoot){
                if (docs.getJSONObject(i).has("license")) //licenses found in JSON
                    licences = docs.getJSONObject(i).getJSONArray("license");
                else
                    return jsonArrToList; //empty
            }
            else {
                if (docs.getJSONObject(i).has("granularity")){
                    JSONArray granularity = docs.getJSONObject(i).getJSONArray("granularity");
                    for (int j = 0; j<granularity.length(); j++){
                        if (granularity.getJSONObject(j).has("link")){
                            String link = granularity.getJSONObject(j).getString("link");
                            if (link.contains(pid)){
                                if (granularity.getJSONObject(j).has("license")){
                                    jsonArrToList.add(granularity.getJSONObject(j).getString("license"));
                                    return jsonArrToList; //child licence
                                }
                                else
                                    return jsonArrToList; //empty
                            }
                        }
                    }
                }
                else if (docs.getJSONObject(i).has("pids")){ // nema granularity, tak se podivam do pids, ktere maji mit stejny label jako root
                    JSONArray sdnntPids = docs.getJSONObject(i).getJSONArray("pids");
                    for (int p = 0; p < sdnntPids.length(); p++){
                        String sdnntChildPid = sdnntPids.getString(p);
                        if (sdnntChildPid.contains(pid)){ //get root
                            if (docs.getJSONObject(i).has("license")) //licenses found in JSON
                                licences = docs.getJSONObject(i).getJSONArray("license");
                            else
                                return jsonArrToList; //empty
                        }
                    }
                }
                else
                    return jsonArrToList; //empty
            }
        }

        if (licences != null) {
            for (int i=0;i<licences.length();i++){
                jsonArrToList.add(licences.getString(i));
            }
        }
        return jsonArrToList;
    }

    public String getJsonResponse(){
        return this.jsonResponse;
    }


    public boolean isDocumentFound(){
        JSONObject jsonObj = new JSONObject(jsonResponse);
        int numFound = jsonObj.getInt("numFound");
        JSONArray docs = jsonObj.getJSONArray("docs");
        int docsLength = docs.length();

        return (numFound != 0) || (docsLength != 0); //return true if numFound != 0 and docsLength != 0
    }

    //return number of found documents
    public int getDocsFound(){
        JSONObject jsonObj = new JSONObject(jsonResponse);
        return jsonObj.getInt("numFound");
    }
}