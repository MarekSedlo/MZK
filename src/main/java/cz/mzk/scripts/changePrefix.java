package cz.mzk.scripts;

import cz.mzk.services.FileIO;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class changePrefix implements Script{

    FileIO fileServ = new FileIO();
    //List<String> toChange = new ArrayList<>();

    private void makeNewFileWithPrefix(String inputFilePath, String outputFilePath){
        List<String> content = fileServ.readFileLineByLine(inputFilePath);
        content = fileServ.addUuidPrefix(content);
        fileServ.toOutputFile(content, outputFilePath);
        content.clear();
    }

    public void start(Properties prop) {
        //makeNewFileWithPrefix("", "");

        /*toChange = fileServ.readFileLineByLine("IO/452/uuidNotInSDNNTDNNTT");
        toChange = fileServ.addUuidPrefix(toChange);
        fileServ.toOutputFile(toChange, "IO/452/uuidNotInSDNNTDNNTTprefix");
        toChange.clear();

        toChange = fileServ.readFileLineByLine("IO/452/uuidNotInSDNNTDNNTO");
        toChange = fileServ.addUuidPrefix(toChange);
        fileServ.toOutputFile(toChange, "IO/452/uuidNotInSDNNTDNNTOprefix");
        toChange.clear();

        toChange = fileServ.readFileLineByLine("IO/452/uuidNotInSDNNTCOVID");
        toChange = fileServ.addUuidPrefix(toChange);
        fileServ.toOutputFile(toChange, "IO/452/uuidNotInSDNNTCOVIDprefix");
        toChange.clear();*/
    }
}
