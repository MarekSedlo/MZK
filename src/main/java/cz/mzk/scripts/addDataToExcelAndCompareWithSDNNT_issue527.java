package cz.mzk.scripts;

import cz.mzk.KrameriusVersion;
import cz.mzk.services.FileIO;
import cz.mzk.services.SdnntConnNEW;
import cz.mzk.services.SolrUtils;
import javafx.util.Pair;
import org.apache.http.HttpStatus;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


//TODO 1. nacist excel, najit data, pridat do excelu - DONE
//TODO 2. pridat kontrolu SDNNT


public class addDataToExcelAndCompareWithSDNNT_issue527 implements Script{
    SolrUtils solrUtils = new SolrUtils(KrameriusVersion.K5);
    List<String> anomaly = new ArrayList<>();
    FileIO fileIO = new FileIO();
    String sdnntHost;
    List<String> differentDocs = new ArrayList<>();
    List<String> multipleDocs = new ArrayList<>();
    List<String> moreStatesDocs = new ArrayList<>();
    List<String> error500Docs = new ArrayList<>();

    private XSSFCell getCell(XSSFRow row, Integer cellIndex){
        XSSFCell cell = row.getCell(cellIndex);
        if (cell == null)
            cell = row.createCell(cellIndex);
        return cell;
    }

    private String checkPidPrefix(String pid){
        String uuid = pid;
        if (!pid.startsWith("uuid:"))
            uuid = "uuid:" + pid;
        return uuid;
    }

    private String parseCnb (String IDs){
        String separatorBegin = "ccnb:";
        String separatorEnd = ",";
        int objIndex = IDs.indexOf(separatorBegin);
        if (objIndex == -1){
            separatorBegin = "cnb";
            objIndex = IDs.indexOf(separatorBegin);
            if (objIndex == -1)
                return "CNB NOT found";
        }

        if (separatorBegin.equals("ccnb:"))
            objIndex += separatorBegin.length();
        String result = IDs.substring(objIndex);
        result = result.split(separatorEnd)[0];
        result = result.replaceAll("\\s+",""); //try to remove all whitespaces
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
        return result;
    }

    private String getNewIdentifierFromSolr(String pid, String identifier){
        if (identifier.equals("cnb")){
            String IDs = solrUtils.getSolrParameterByPid(pid, "dc.identifier", true);
            String cnb = parseCnb(IDs);
            return cnb;
        }
        else if (identifier.equals("issn")){ //Is sometimes issn, sometimes isbn in K5
            String issn = solrUtils.getSolrParameterByPid(pid, "issn", true);
            return issn;
        }
        else
            return null;
    }
    private boolean isCnbFoundInSolr(String cnb){
        return !cnb.equals("CNB NOT found");
    }
    private boolean isIssnNotEmptyInSolr(String issn){
        return !issn.equals("\n");
    }

    // returns sdnntConnNEW with found document
    // if document is not found, than returns null
    private SdnntConnNEW findDocInSDNNT(String sdnntHost, String pid){
        SdnntConnNEW sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + pid);
        if (sdnntConnNEW.getResponseCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR)
            return sdnntConnNEW;

        boolean docFound = false;
        //check for uuid in SDNNT
        if (sdnntConnNEW.isDocumentFound()){ // if something was found, still can be anomaly
            docFound = true;
        }
        else{
            String cnb = getNewIdentifierFromSolr(pid, "cnb");
            assert (cnb != null);
            if (isCnbFoundInSolr(cnb)){
                sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + cnb);
                //check for cnb in SDNNT
                if (sdnntConnNEW.isDocumentFound()){ // if something was found, still can be anomaly
                    docFound = true;
                }
                else { //cnb not found in SDNNT, try issn
                    String issn = getNewIdentifierFromSolr(pid, "issn");
                    assert (issn != null);
                    if (isIssnNotEmptyInSolr(issn)){
                        sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + issn);
                        //check for issn in SDNNT
                        if (sdnntConnNEW.isDocumentFound()){
                            docFound = true;
                        }
                    }
                }
            }
            else { // cnb not found in solr, try issn
                String issn = getNewIdentifierFromSolr(pid, "issn");
                assert (issn != null);
                if (isIssnNotEmptyInSolr(issn)){
                    sdnntConnNEW = new SdnntConnNEW(sdnntHost, "?query=" + issn);
                    //check for issn in SDNNT
                    if (sdnntConnNEW.isDocumentFound()){
                        docFound = true;
                    }
                }
            }
        }
        if (docFound)
            return sdnntConnNEW;
        else
            return null;
    }

    private boolean isSdnntFoundDocSame(SdnntConnNEW sdnntConnNEW, String pid){
        if (sdnntConnNEW.responseContentsPidInRootPidsAndLinks(pid))
            return true;
        String solrTitle = solrUtils.getSolrParameterByPid(pid, "root_title", true);
        if (sdnntConnNEW.responseContentsTitleInRootTitle(solrTitle))
            return true;
        return false;
    }

    String parseSKC(String identifierFromAssocItems){
        String SKC = "";
        String[] parts = identifierFromAssocItems.split(":");
        for (String part:parts){
            if (part.startsWith("SKC01"))
                SKC = part;
        }
        return SKC;
    }

    private Pair<String, String> getSdnntInfo(String pidToCheck){
        Pair<String, String> states_SKC;
        // = new Pair<>(null, null);
        String DOCstate = null;
        String SKC = null;
        String pid = checkPidPrefix(pidToCheck);
        SdnntConnNEW sdnntConnNEW = findDocInSDNNT(sdnntHost, pid);
        if (sdnntConnNEW != null) { //it means document was found in SDNNT
            if ((sdnntConnNEW.getResponseCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) || (sdnntConnNEW.getDocsFound() > 1)){
                DOCstate = "ANOMALY";
                if (sdnntConnNEW.getDocsFound() > 1)
                    multipleDocs.add(pid);
                else
                    error500Docs.add(pid);
            }
            else {
                if (isSdnntFoundDocSame(sdnntConnNEW, pid)){
                    List<String> states = sdnntConnNEW.getStatesOfRootFromCurrentResponse();
                    if (states.size() != 1){
                        DOCstate = "ANOMALY";
                        moreStatesDocs.add(pid);
                    }
                    else
                        DOCstate = states.get(0);
                }
                else{
                    DOCstate = "ANOMALY";
                    differentDocs.add(pid);
                }

            }
        }
        else
            DOCstate = "DOCNOTFOUND";

        if ((DOCstate.equals("ANOMALY")) || (DOCstate.equals("DOCNOTFOUND"))){
            SKC = "";
        } else {
            String IDfromAssociatedItems = sdnntConnNEW.getIDFromAssociatedItems();
            SKC = parseSKC(IDfromAssociatedItems);
        }

        states_SKC = new Pair<>(DOCstate, SKC);
        return states_SKC;
    }

    private void updateRow(XSSFRow row, String pid){
        String title = solrUtils.getSolrParameterByPid(pid, "title", false);
        String yearOfPublication = solrUtils.getSolrParameterByPid(pid, "datum_begin", false);
        XSSFCell cell = getCell(row, 0);
        cell.setCellValue(title);
        cell = getCell(row, 1);
        cell.setCellValue(yearOfPublication);

        Pair<String, String> SDNNTdata = getSdnntInfo(pid);

        if (!SDNNTdata.getValue().isEmpty()){
            cell = getCell(row, 2);
            cell.setCellValue(SDNNTdata.getValue());
        }

        switch (SDNNTdata.getKey()){
            case "A": {
                cell = getCell(row, 4);
                cell.setCellValue(SDNNTdata.getKey());
                break;
            }
            case "N": {
                cell = getCell(row, 5);
                cell.setCellValue(SDNNTdata.getKey());
                break;
            }
            case "PA": {
                cell = getCell(row, 6);
                cell.setCellValue(SDNNTdata.getKey());
                break;
            }
            case "DOCNOTFOUND": {
                cell = getCell(row, 7);
                cell.setCellValue("Není");
                break;
            }
            case "ANOMALY": {
                anomaly.add(pid);
                break;
            }
            default:{ //document has some SDNNT state, but it is not in excel table, do nothing
                break;
            }
        }
    }

    public void start(Properties prop) {
        sdnntHost = prop.getProperty("SDNNT_HOST_PRIVATE_PART_API_CATALOG");
        String excelFilePath = "IO/527/tableLarge.xlsx";
        HashMap<XSSFRow, String> records = new HashMap<>();

        try {
            FileInputStream inputStream = new FileInputStream((excelFilePath));
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheetOne = xssfWorkbook.getSheetAt(0);

            for (int i = 0; i<sheetOne.getPhysicalNumberOfRows(); i++){ // cycle through rows
                if (i != 0){ //first row are names of columns
                    XSSFRow row = sheetOne.getRow(i);
                    XSSFCell uuid = row.getCell(3);
                    if (uuid.toString().isEmpty())
                        break;
                    records.put(row, uuid.toString());
                }
            }

            for (Map.Entry<XSSFRow, String> entry : records.entrySet()){
                String pid = entry.getValue();
                XSSFRow row = entry.getKey();
                updateRow(row, pid);
            }

            inputStream.close();

            // write to file
            FileOutputStream outputStream = new FileOutputStream(excelFilePath);
            xssfWorkbook.write(outputStream);
            xssfWorkbook.close();
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        fileIO.toOutputFile(anomaly, "IO/527/anomaly.txt");
        fileIO.toOutputFile(error500Docs, "IO/527/error500Docs.txt");
        fileIO.toOutputFile(multipleDocs, "IO/527/multipleDocs.txt");
        fileIO.toOutputFile(differentDocs, "IO/527/differentDocs.txt");
        fileIO.toOutputFile(moreStatesDocs, "IO/527/moreStatesDocs.txt");
    }
}
