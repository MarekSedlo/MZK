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

    public SdnntConnNEW(String sdnntHost){
        this.sdnntHost = sdnntHost;

        String resp = "";

        restTemplate = getRestTemplate();
        String apikey = "29039fda-5065-46aa-8ac8-b8de348c41e9"; //TODO move to config
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/json");
        headers.set("X-API-KEY", apikey);
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(sdnntHost, HttpMethod.GET, entity, String.class);
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

    public List<String> getSdnntLicences(){
        JSONObject jsonObj = new JSONObject(jsonResponse);
        JSONArray docs = jsonObj.getJSONArray("docs");
        JSONArray licences = null;
        ArrayList<String> jsonArrToList = new ArrayList<String>();

        for (int i = 0; i<docs.length(); i++){
            if (docs.getJSONObject(i).has("license")) //licenses found in JSON
                licences = docs.getJSONObject(i).getJSONArray("license");
            else
                return jsonArrToList;
        }

        if (licences != null) {
            for (int i=0;i<licences.length();i++){
                jsonArrToList.add(licences.getString(i));
            }
        }
        return jsonArrToList;
    }
}