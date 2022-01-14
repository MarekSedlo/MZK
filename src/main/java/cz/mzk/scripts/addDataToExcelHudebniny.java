package cz.mzk.scripts;

import cz.mzk.KrameriusVersion;
import cz.mzk.services.Connection;
import cz.mzk.services.FileIO;
import cz.mzk.services.SolrUtils;
import cz.mzk.services.XMLparser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class addDataToExcelHudebniny implements Script{
    SolrUtils solrUtils = new SolrUtils(KrameriusVersion.K5);
    String excelFilePath = "IO/hudebniny/hudebniny.xlsx";
    Connection con = new Connection();
    List<String> notFoundInFedora = new ArrayList<>();
    FileIO fileIO = new FileIO();
    XMLparser parser = new XMLparser();

    private boolean isEmpty(Row row){
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++){ //ODKOMENTOVAT PRO KONTROLU CELEHO RADKU
            Cell cell = row.getCell(c);
            if ((cell != null) && (cell.getCellType() != CellType.BLANK))
                return false;
        }
        return true;
    }

    private int getFirstEmptyRowIndex(XSSFSheet sheet){
        int lastRow = sheet.getLastRowNum();
        for (int i = 0; i<sheet.getLastRowNum(); i++){
            if (isEmpty(sheet.getRow(i))){
                lastRow = i;
                break;
            }
        }
        if (!isEmpty(sheet.getRow(lastRow)))
            lastRow++;
        return lastRow;
    }

    private void addRowToSheet(XSSFSheet sheet, String pid, String nazev, String autor, String datumVydani, String sigla){
        int rowCount = getFirstEmptyRowIndex(sheet);
        XSSFRow row = sheet.getRow(rowCount);
        if (row == null)
            row = sheet.createRow(rowCount);
        XSSFCell cell = row.createCell(0);
        cell.setCellValue(nazev);
        cell = row.createCell(1);
        cell.setCellValue(autor);
        cell = row.createCell(2);
        cell.setCellValue(datumVydani);
        cell = row.createCell(3);
        cell.setCellValue(sigla);
        cell = row.createCell(4);
        cell.setCellValue(pid);
    }

    private String getSigla(String pid){
        String sigla = "TODO";
        if (!pid.startsWith("uuid:"))
            pid = "uuid:" + pid;
        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/" + pid + "/datastreams/BIBLIO_MODS/content", true);
        try {
            int response = connection.getResponseCode();
            if (response == 200){
                StringBuilder FOXML = con.read(connection);
                sigla = parser.extractSigla(FOXML);
            }
            else if (response == 404){
                System.out.println("PID NOT FOUND IN FEDORA: " + pid + " SKIPPING\n");
                notFoundInFedora.add(pid);
            }
            else
                System.out.println("Unexpected error " + response + ", when fedora connecting to pid: " + pid + " SKIPPING\n");
        } catch (Exception e){
            e.printStackTrace();
        }
        return sigla;
    }

    public void start(Properties prop) {
        String query = "fedora.model:sheetmusic AND datum_begin:[1900 TO *] AND dostupnost:private";
        List<String> pids = solrUtils.getPids(query, 1000000, false);

        try {
            FileInputStream inputStream = new FileInputStream((excelFilePath));
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheetOne = xssfWorkbook.getSheetAt(0);

            for (String pid:pids){
                String nazev = solrUtils.getSolrParameterByPid(pid, "title", false);
                String autor = solrUtils.getSolrParameterByPid(pid, "dc.creator", false);
                String datumVydani = solrUtils.getSolrParameterByPid(pid, "datum_begin", false);
                String sigla = getSigla(pid);
                addRowToSheet(sheetOne, pid, nazev, autor, datumVydani, sigla);
            }
            inputStream.close();
            // zapsat do souboru
            FileOutputStream outputStream = new FileOutputStream(excelFilePath);
            xssfWorkbook.write(outputStream);
            xssfWorkbook.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileIO.toOutputFile(notFoundInFedora, "IO/hudebniny/notFoundInFedora.txt");
    }
}
