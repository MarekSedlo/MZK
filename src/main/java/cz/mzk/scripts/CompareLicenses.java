package cz.mzk.scripts;

import cz.mzk.services.FileIO;
import cz.mzk.services.SdnntConnNEW;
import cz.mzk.services.SolrUtils;

import java.util.*;

//this script compares licenses in MZK and SDNNT
//input: expects input file of uuids
//output: differences are in LOG output file

public class CompareLicenses implements Script {
    FileIO fileService = new FileIO();
    SolrUtils solrConn = new SolrUtils();
    public void start(Properties prop) {
        final String sdnntHost = prop.getProperty("SDNNT_HOST_PRIVATE_PART_API_CATALOG");
        List<String> input = fileService.readFileLineByLine("IO/compareLicenses/input");
        List<String> LOG = new ArrayList<>();
        List<String> removeDNNTO = new ArrayList<>();
        List<String> removeDNNTT = new ArrayList<>();
        List<String> addDNNTO = new ArrayList<>();
        List<String> addDNNTT = new ArrayList<>();

        for (String uuid : input){
            SdnntConnNEW sdnntConnectionNEW = new SdnntConnNEW(sdnntHost, "?query=" + uuid);
            List<String> SDNNTlicences = sdnntConnectionNEW.getSdnntLicences();
            String MZKlics = solrConn.getSolrParameterByPid(uuid, "dnnt-labels");
            List<String> MZKlicenses = new ArrayList<>();
            if (!Objects.equals(MZKlics, "null"))
                MZKlicenses = makeList(MZKlics);

            if (SDNNTlicences.isEmpty()){
                if (!MZKlicenses.isEmpty()){ //MZK got license and SDNNT not --> remove MZK license
                    LOG.add("No SDNNT license for MZK licensed doc: " + uuid + " with MZK license: " + MZKlicenses);
                    if (MZKlicenses.contains("dnnto")){
                        removeDNNTO.add(uuid);
                    }
                    if (MZKlicenses.contains("dnntt")){
                        removeDNNTT.add(uuid);
                    }
                }

            } else {
                for (String lic : SDNNTlicences){
                    if (!MZKlicenses.contains(lic)){//SDNNT got license and MZK not --> add MZK license
                        LOG.add("No MZK license " + lic + ", which was found in SDNNT licenses for " + uuid);
                        if (lic.equals("dnnto")){
                            addDNNTO.add(uuid);
                        }
                        if (lic.equals("dnntt")){
                            addDNNTT.add(uuid);
                        }
                    }
                }
            }
        }
        fileService.toOutputFile(LOG, "IO/compareLicenses/LOG");
        fileService.toOutputFile(removeDNNTO, "IO/compareLicenses/removeDNNTO");
        fileService.toOutputFile(removeDNNTT, "IO/compareLicenses/removeDNNTT");
        fileService.toOutputFile(addDNNTO, "IO/compareLicenses/addDNNTO");
        fileService.toOutputFile(addDNNTT, "IO/compareLicenses/addDNNTT");
    }

    private List<String> makeList(String strToList){
        List<String> result = new ArrayList<>();
        if (!strToList.contains(" ")){ //strToList is one item
            String modifiedStr = strToList.replace("\n", "");
            result.add(modifiedStr);
        }

        else {
            String modifiedStr = strToList.replace("[", "");
            modifiedStr = modifiedStr.replace("]", "");
            modifiedStr = modifiedStr.replace(" ", "");
            modifiedStr = modifiedStr.replace("\n", "");
            result = new ArrayList<String>(Arrays.asList(modifiedStr.split(",")));
        }
        return result;
    }
}
