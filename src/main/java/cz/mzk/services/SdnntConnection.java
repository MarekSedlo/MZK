package cz.mzk.services;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class SdnntConnection {
    private final String sdnntHost;
    private final RestTemplate restTemplate;
    private final JsonParser jsonParser;
    private Map<String,Object> map;
    private List<Object> list;
    private boolean isSingleJsonItem = false;

    public SdnntConnection(String sdnntHost){
        this.sdnntHost = sdnntHost;
        restTemplate = new RestTemplate();
        jsonParser = JsonParserFactory.getJsonParser();
        String resp = restTemplate.getForObject(sdnntHost, String.class);
        assert resp != null;
        if (resp.charAt(0) == '{'){
            isSingleJsonItem = true;
            map = jsonParser.parseMap(resp);
        }
        else {
            isSingleJsonItem = false;
            list = jsonParser.parseList(resp);
        }
    }

    public Object getSdnntJsonItem(String jsonItem){
        if (isSingleJsonItem)
            return map.get(jsonItem);
        else { // TODO test it! May be wrong for collections, it returns last item...
            for (Object o : list){
                if (list.size() > 1)
                    System.out.println("LIST IS LONG");
                if (o instanceof Map){
                    map = (Map<String, Object>) o;
                }
            }
        }
        return map.get(jsonItem);
    }
}
