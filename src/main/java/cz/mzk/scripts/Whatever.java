package cz.mzk.scripts;

import cz.mzk.services.FileIO;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Whatever implements Script {
    FileIO fileIO = new FileIO();

    public void start(Properties prop) {
        String file = fileIO.readFileToStr("IO/437/SOLRPUBLIC.txt");
        List<String> uuids = new ArrayList<>();
        //parsing
        String[] parts = file.split(" ");
        int sizeShouldBe = Integer.parseInt(parts[0]);

        assert (parts.length == sizeShouldBe);

        for (String part : parts){
            String uuid = "NOTHING";
            if (part.length() > 2){
                /*if ((part.charAt(part.length()-1) == '[') && (part.charAt(part.length()-2) == ','))
                    uuid = part.substring(0, part.length()-2);*/
                if ((part.charAt(part.length()-1) == ',') || (part.charAt(part.length()-1) == '[')|| (part.charAt(part.length()-1) == ']'))
                    uuid = part.substring(0, part.length()-1);
                if (uuid.charAt(0) == 'u')
                    uuids.add(uuid);
                else if ((uuid.charAt(0) == '[') && (uuid.charAt(1) == 'u'))
                    uuids.add(uuid.substring(1));
                else
                    System.out.println(part);
            }
        }

        assert (uuids.size() == sizeShouldBe);

        fileIO.toOutputFile(uuids, "IO/437/output.txt");
    }
}
