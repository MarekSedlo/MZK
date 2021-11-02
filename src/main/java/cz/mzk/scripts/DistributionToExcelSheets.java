package cz.mzk.scripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.solr.common.StringUtils;

/*

Tady najdes jak pridat radky do jednotlivych listu: https://www.codejava.net/coding/java-example-to-update-existing-excel-files-using-apache-poi
Tady je jak prochazet radky Sheetu: https://stackoverflow.com/questions/8424327/how-to-loop-through-all-the-rows-and-cells-in-an-excel-file/8424428

*/

public class DistributionToExcelSheets implements Script {
    List<String> anomaly = new ArrayList<>();


    private int parseDatum(Cell datumCell){
        int result = -1;

        String datumCellStr = String.valueOf(datumCell);
        String datumSubStr = "";
        StringBuilder datumParsed = new StringBuilder("");

        if (datumCellStr.endsWith(".0"))
            datumSubStr = datumCellStr.substring(0, datumCellStr.length() - 2);
        else
            datumSubStr = datumCellStr;

        for (int i = 0; i<datumSubStr.length(); i++){
            if (Character.isDigit(datumSubStr.charAt(i)))
                datumParsed.append(datumSubStr.charAt(i));
        }
        result = Integer.parseInt(String.valueOf(datumParsed));
        if (datumParsed.length() < 4){
            anomaly.add(datumSubStr);
            result = -1;
        }

        else if(datumParsed.length() == 4) // datum jsou ctyri cislice - je to ok
            return result;
        else if (datumParsed.length() == 8){ // je tam rozmezi dvou let, vybere se vetsi
            String firstDatum = datumParsed.substring(0,4);
            String secondDatum = datumParsed.substring(4,8);
            result = Integer.max(Integer.parseInt(firstDatum), Integer.parseInt(secondDatum));
        }
        else{
            anomaly.add(datumSubStr);
            result = -1;
        }
        //System.out.println("CELL: " + datumCell);
        //System.out.println("CONVERTED TO INT: " + result);
        return result;
    }

    private XSSFSheet getCorrectSheet(int datum, XSSFWorkbook xssfWorkbook){
        XSSFSheet correctSheet = null;
        String datumStr = Integer.toString(datum);
        for (int i = 0; i<xssfWorkbook.getNumberOfSheets(); i++){
            String sheetName = xssfWorkbook.getSheetName(i);
            if (sheetName.contains(datumStr)){ //pokud nazev listu obsahuje datum, pak je to spravny list
                correctSheet = xssfWorkbook.getSheet(sheetName);
                break;
            }
        }
        return correctSheet;
    }

    private void addRowToSheet(XSSFSheet sheet, XSSFRow inputRow){
        int rowCount = getFirstEmptyRowIndex(sheet);

        Row row = sheet.getRow(rowCount);
        if (row == null)
            row = sheet.createRow(rowCount);

        Cell inputRowCell;
        for (int i=0; i < inputRow.getLastCellNum(); i++){
            inputRowCell = inputRow.getCell(i);
            if (inputRowCell == null) //pokud chybi zaznam, tak preskocit
                continue;

            Cell newCell = row.getCell(i);
            if (newCell == null)
                newCell = row.createCell(i);

            if (i<11){
                if ((i==4) || (i==5)){
                    inputRowCell = inputRow.getCell(i+1);
                }
                if ((i==6) || (i==7) || (i==8))
                    inputRowCell = inputRow.getCell(i+3);
                if ((i==9) || (i==10))
                    inputRowCell = inputRow.getCell(i+4);

                if (inputRowCell == null)
                    continue;

                if (inputRowCell.getCellType() == CellType.FORMULA)
                    newCell.setCellValue(inputRowCell.getBooleanCellValue());
                else if (inputRowCell.getCellType() == CellType.NUMERIC)
                    newCell.setCellValue(inputRowCell.getNumericCellValue());
                else{
                    if (i == 5)
                        newCell.setCellValue("uuid:" + inputRowCell.getStringCellValue());
                    else
                        newCell.setCellValue(inputRowCell.getStringCellValue());
                }

            }
        }
    }

    private boolean isEmpty(Row row){
        int rowCellsToCheck = 4;
        if (row.getLastCellNum() <= rowCellsToCheck)
            rowCellsToCheck = row.getLastCellNum();

        //for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++){ //ODKOMENTOVAT PRO KONTROLU CELEHO RADKU
        for (int c = row.getFirstCellNum(); c < rowCellsToCheck; c++){
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

    public void start(Properties prop) {
        String excelFilePath = "IO/506/1901az1949.xlsx";
        String excelFilePath2 = "IO/506/1950az2020.xlsx";
        HashMap<XSSFRow, Integer> records = new HashMap<>();

        try {
            FileInputStream inputStream = new FileInputStream((excelFilePath));
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);

            XSSFSheet importyNDK2021 = xssfWorkbook.getSheetAt(1); //toto je vyber ktery list chci
            int datumVydani = 0;

            for (int i = 0; i<importyNDK2021.getPhysicalNumberOfRows(); i++){ // cyklus pres radky
                if (i != 0){ //prvni radek jsou nazvy sloupcu, ten preskakuji
                    XSSFRow row = importyNDK2021.getRow(i);
                    XSSFCell datumVydaniCell = row.getCell(2);
                    datumVydani = parseDatum(datumVydaniCell);
                    if (datumVydani != -1){ //datum vydani je validni -> neni to anomalie
                        records.put(row, datumVydani);
                    }
                }
            }

            //ted je vse nacteno v records

            for (Map.Entry<XSSFRow, Integer> entry : records.entrySet()){
                int datum = entry.getValue();
                XSSFRow row = entry.getKey();
                if ((datum > 1900) && (datum < 1950)){
                    XSSFSheet datumSheet = getCorrectSheet(datum, xssfWorkbook);
                    System.out.println("ADDING TO SHEET: " + datum);
                    assert(datumSheet!=null);
                    addRowToSheet(datumSheet, row);
                }
            }

            inputStream.close();

            // zapsat do souboru
            FileOutputStream outputStream = new FileOutputStream(excelFilePath);
            xssfWorkbook.write(outputStream);
            xssfWorkbook.close();
            outputStream.close();

            //druhy soubor radeji zvlast

            inputStream = new FileInputStream(excelFilePath2);
            xssfWorkbook = new XSSFWorkbook(inputStream);

            for (Map.Entry<XSSFRow, Integer> entry : records.entrySet()){
                int datum = entry.getValue();
                XSSFRow row = entry.getKey();
                if ((datum >= 1950) && (datum <= 2020)){
                    XSSFSheet datumSheet = getCorrectSheet(datum, xssfWorkbook);
                    System.out.println("ADDING TO SHEET: " + datum);
                    assert(datumSheet!=null);
                    addRowToSheet(datumSheet, row);
                }
            }

            inputStream.close();

            // zapsat do souboru
            outputStream = new FileOutputStream(excelFilePath2);
            xssfWorkbook.write(outputStream);
            xssfWorkbook.close();
            outputStream.close();

            System.out.println("ANOMALY\n" + anomaly);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


/* 1. nacist si excel soubor
*  2. z listu importy ndk 2021 si nejak ulozit cely zaznam z listu podle toho jaky ma rok vydani
*  3. zaznam ulozit do listu se stejnym rokem vydani
* */