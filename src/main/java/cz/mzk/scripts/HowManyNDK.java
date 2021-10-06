package cz.mzk.scripts;

import cz.mzk.services.FileIO;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class HowManyNDK implements Script {

    FileIO fileService = new FileIO();
    public void start(Properties prop){
        List<String> images = new ArrayList<>();
        images = fileService.readFileLineByLine("IO/imagesInRovnostBACKUP");

        int count = 0;
        for (String image : images){
            if (image.contains("NDK"))
                count++;
        }

        System.out.println("NDK/ALL: " + count + "/" + images.size());
    }
}
