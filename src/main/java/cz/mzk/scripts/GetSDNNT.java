package cz.mzk.scripts;

import cz.mzk.services.FileIO;

import java.util.*;

import cz.mzk.services.SdnntConnectionOLD;
import cz.mzk.services.SolrUtils;

/*
    Author: Marek Sedlo
    Description:
    Script for issue 452, deleting dnnt labels for given list of documents (public documents, which are not in SDNNT)
    Result is for fofola usage
*/

public class GetSDNNT implements Script{
    public static final boolean DEBUG = false;
    private List<String> LOG = new ArrayList<>();
    private final String inputFileCOVID = "IO/452/publicCOVID";
    private final String inputFileDNNTO = "IO/452/publicDNNTO";
    private final String inputFileDNNTT = "IO/452/publicDNNTT";
    FileIO fileService = new FileIO();
    private List<String> uuidStatusLicenceCnb = new ArrayList<>();
    private List<String> uuidNotInSDNNT = new ArrayList<>();
    private List<String> uuidCnbfalse = new ArrayList<>();


    private String parseIdentifiers (String IDs){
        String separatorBegin = "ccnb:";
        String separatorEnd = ",";
        int objIndex = IDs.indexOf(separatorBegin);
        //int cnbWithoutPrefixIndex = IDs.indexOf("cnb");
        if (objIndex == -1){
            separatorBegin = "cnb";
            objIndex = IDs.indexOf(separatorBegin);
            if (objIndex == -1)
                return "cnb NOT FOUND";
        }

        if (separatorBegin.equals("ccnb:"))
            objIndex += separatorBegin.length();
        String result = IDs.substring(objIndex);
        result = result.split(separatorEnd)[0];
        result = result.replaceAll("\\s+",""); //remove all whitespaces
        if (result.charAt(0) != 'c'){
            StringBuilder sb = new StringBuilder();
            boolean cnbStartFound = false;
            for (int i=0; i<result.length();i++){
                if (result.charAt(i) == 'c')
                    cnbStartFound = true;
                if (cnbStartFound)
                    sb.append(result.charAt(i));
            }
            result = sb.toString();
        }
        System.out.println(result);
        return result;
    }

    private String getCnb(String pid){
        SolrUtils solrConn = new SolrUtils();
        String identifiers = solrConn.getSolrParameterByPid(pid, "dc.identifier");
        String cnb = parseIdentifiers(identifiers);
        return cnb;
    }

    private List<String> cutUuidPrefixOut(List<String> uuids){
        List<String> output = uuids;
        String uuid;
        for (int i = 0; i < output.size(); i++){
            uuid = output.get(i);
            uuid = uuid.replace("uuid:", "");
            output.set(i, uuid);
        }
        return output;
    }

    private void createUuidsForDNNTremove(String inputType, String sdnntHost){
        List<String> rootDnntDocsUuids = new ArrayList<>();
        if (inputType.equals("DNNTO"))
            rootDnntDocsUuids = fileService.readFileLineByLine(inputFileDNNTO);
        if (inputType.equals("DNNTT"))
            rootDnntDocsUuids = fileService.readFileLineByLine(inputFileDNNTT);
        if (inputType.equals("COVID"))
            rootDnntDocsUuids = fileService.readFileLineByLine(inputFileCOVID);
        rootDnntDocsUuids = cutUuidPrefixOut(rootDnntDocsUuids);

        String url = "";
        Object cnb = "";
        Object statusUuid = "";
        Object statusCnb = "";
        Object licence = "";
        SdnntConnectionOLD sdnntCon;
        for (String rootUuid:rootDnntDocsUuids){
            cnb = "";
            statusUuid = "";
            statusCnb = "";
            licence = "";

            url = sdnntHost + "?uuid=" + rootUuid;
            sdnntCon = new SdnntConnectionOLD(url);

            statusUuid = sdnntCon.getSdnntJsonItem("status");
            licence = sdnntCon.getSdnntJsonItem("licence");
            cnb = sdnntCon.getSdnntJsonItem("ccnb_id");

            if (statusUuid instanceof Boolean){
                if ((Boolean) statusUuid)
                    statusUuid = "true";
                else{ //make connection by cnb
                    statusUuid = "false";
                    cnb = getCnb(rootUuid);
                    url = sdnntHost + "?cnb=" + cnb;
                    sdnntCon = new SdnntConnectionOLD(url);
                    statusCnb = sdnntCon.getSdnntJsonItem("status");
                    licence = sdnntCon.getSdnntJsonItem("licence");
                    if (statusCnb instanceof Boolean){
                        if ((Boolean) statusCnb)
                            statusCnb = "true";
                        else
                            statusCnb = "false";
                    }
                }
            }

            if (statusUuid == null)
                statusUuid = "null";
            if (statusCnb == null)
                statusCnb = "null";
            if (licence == null)
                licence = "null";
            if (cnb == null)
                cnb = "null";

            uuidStatusLicenceCnb.add("uuid: " + rootUuid + ", statusUuid: " + statusUuid + ", statusCnb: " + statusCnb + ", licence: " + licence + ", cnb: " + cnb);

            if (!((statusUuid.equals(licence)) || (statusCnb.equals(licence))))
                LOG.add("Status and licence are different! uuid: " + rootUuid + " statusUuid: " + statusUuid + ", statusCnb: " + statusCnb + " licence: " + licence);

            if (statusUuid.equals("X") || statusCnb.equals("X") || (licence.equals("N")) || (licence.equals("PN")))
                uuidNotInSDNNT.add(rootUuid);

            if (statusCnb.equals("false") && (statusUuid.equals("false"))){
                uuidCnbfalse.add(rootUuid);
            }

        }
        if (!DEBUG){
            fileService.toOutputFile(uuidStatusLicenceCnb, "IO/452/uuidStatusLicenceCnb" + inputType + ".txt");
            fileService.addUuidPrefix(uuidNotInSDNNT);
            fileService.toOutputFile(uuidNotInSDNNT, "IO/452/uuidNotInSDNNT" + inputType + ".txt");
            fileService.addUuidPrefix(uuidCnbfalse);
            fileService.toOutputFile(uuidCnbfalse, "IO/452/uuidCnbfalse" + inputType + ".txt");
        }

        uuidCnbfalse.clear();
        uuidStatusLicenceCnb.clear();
        uuidNotInSDNNT.clear();
    }

    @Override
    public void start(Properties prop) {
        String sdnntHost = prop.getProperty("SDNNT_HOST_OAI_API");
        System.out.println("DNNTO:");
        createUuidsForDNNTremove("DNNTO", sdnntHost);
        System.out.println("DNNTT:");
        createUuidsForDNNTremove("DNNTT", sdnntHost);
        System.out.println("COVID:");
        createUuidsForDNNTremove("COVID", sdnntHost);

        fileService.toOutputFile(LOG, "IO/452/LOG");
    }
}
