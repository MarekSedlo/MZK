package cz.mzk.scripts;

import cz.mzk.services.FileIO;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class checkSysno implements Script {

    FileIO fileService = new FileIO();

    public void start(Properties prop) {
        List<String> aleph = fileService.readFileLineByLine("IO/sysna/aleph.txt");
        List<String> textak = fileService.readFileLineByLine("IO/sysna/textak.txt");
        List<String> result = new ArrayList<>();

        int i = 0;
        for (String item : aleph){
            String[] parts = item.split(" ");
            String startIdentificator = parts[0]; //pro kontrolu zacatku
            String endIdentificator = parts[parts.length-1]; //pro kontrolu koncu
            endIdentificator = endIdentificator.substring(endIdentificator.lastIndexOf("@")+1);

            if (!startIdentificator.equals(endIdentificator))
                System.out.println("different start and end IDs, startID: " + startIdentificator + " endID: " + endIdentificator);
            if (!textak.contains(endIdentificator)){
                result.add(item);
            }
            if (i % 1000 == 0) {
                System.out.println(i + " out of " + aleph.size() + " done");
            }
            i++;
        }

        fileService.toOutputFile(result, "IO/sysna/result.txt");
    }
}
