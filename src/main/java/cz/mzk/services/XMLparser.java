package cz.mzk.services;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XMLparser {

    public List<String> getChildrenUuids(String parentUuid, boolean DEBUG){
        Connection conn = new Connection();
        HttpURLConnection connection = conn.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/uuid:" + parentUuid + "/datastreams/RELS-EXT/content", DEBUG);
        StringBuilder FOXML = conn.read(connection);
        List<String> childrenUuids = getFOXMLChildrenUuids(FOXML);
        return childrenUuids;
    }

    //private final String[] childrenTypes = new String[]{"volume", "item, page"};

    private int countSubstrings(String fromHere, String countThis){
        int lastIndex = 0;
        int count = 0;
        while (lastIndex != -1){
            lastIndex = fromHere.indexOf(countThis, lastIndex);

            if (lastIndex != -1){
                count++;
                lastIndex += countThis.length();
            }
        }
        return count;
    }

    //public List<String> getFOXMLChildrenUuids(StringBuilder FOXMLparent, String childType){
    public List<String> getFOXMLChildrenUuids(StringBuilder FOXMLparent){
        //assert(Arrays.asList(childrenTypes).contains(childType));
        List<String> childrenUuids = new ArrayList<>();

        String parent = FOXMLparent.toString();

        int childOccurs = countSubstrings(parent, "info:fedora/uuid:");

        String childUuid = null;

        for (int i=0; i<childOccurs; i++){
            String separatorBegin = "info:fedora/uuid:";
            String separatorEnd = "\"";
            int objIndex = parent.indexOf(separatorBegin);
            objIndex += separatorBegin.length();

            childUuid = parent.substring(objIndex);
            childUuid = childUuid.split(separatorEnd)[0];

            parent = parent.substring(objIndex, parent.length());
            childrenUuids.add(childUuid);
        }

        if (childrenUuids.size()>0)
            childrenUuids.remove(0); //remove the parent url
        else
            System.err.println("WARNING FOXML has no children");
        return childrenUuids;
    }

    //argument is RELS-EXT datastream of page FOXML
    public String getImgUrl(String pageFOXML){
        String imgUrl = pageFOXML;

        int imgOccurs = countSubstrings(pageFOXML, "http://imageserver");
        if (imgOccurs < 1)
            System.err.println("ERROR page has no images!");
        if (imgOccurs > 1)
            System.err.println("ERROR page has several images, this function reads only first one!");

        int objIndex = imgUrl.indexOf("http://imageserver");
        imgUrl = imgUrl.substring(objIndex);
        imgUrl = imgUrl.split("</tiles-url>")[0];

        return imgUrl;
    }

    public String extractSigla(StringBuilder RELS_EXT_FOXML){
        String sigla = RELS_EXT_FOXML.toString();
        String[] lines = sigla.split("\n");
        String parsedSigla = "";
        for (String line:lines){
            if (line.contains("mods:physicalLocation")){
                sigla = line.split(">")[1];
                for (int i=0; i<sigla.length(); i++){
                    if (sigla.charAt(i) == '<')
                        break;
                    parsedSigla += sigla.charAt(i);
                }
            }
        }
        return parsedSigla;
    }
}




