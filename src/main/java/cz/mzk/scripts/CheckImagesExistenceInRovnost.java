package cz.mzk.scripts;

import cz.mzk.services.Connection;
import cz.mzk.services.FileIO;
import cz.mzk.services.XMLparser;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class CheckImagesExistenceInRovnost implements Script{

    public static final boolean DEBUG = false;
    private final String inputFile = "IO/zbytek.txt"; //path marekscripts/zbytek.txt
    FileIO fileService = new FileIO();
    Connection con = new Connection();
    XMLparser parser = new XMLparser();

    List<String> imgsInRovnost = new ArrayList<>();




    private void getDeeper(String parentUuid){
        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/uuid:" + parentUuid + "/datastreams/RELS-EXT/content", DEBUG);
        StringBuilder FOXML = con.read(connection);
        List<String> childrenUuids = parser.getFOXMLChildrenUuids(FOXML);

        if (childrenUuids.size() > 0){
            for (String childUuid : childrenUuids){
                getDeeper(childUuid);
            }
        } else {
            String imgUrl = parser.getImgUrl(FOXML.toString());
            System.out.println(imgUrl);
            imgsInRovnost.add(imgUrl);
        }
    }

    public void start(Properties prop){
        List<String> imagesToCheck = fileService.readFileLineByLine(inputFile); //TODO attention for first two lines
        List<String> imagesToCheckParsed = new ArrayList<>();

        List<String> imagesInRovnost = new ArrayList<>();
        List<String> imagesInRovnostParsed = new ArrayList<>();

        String rovnostUuid = "f1c7c08d-8f64-4b66-be28-5f209c2c7021";

        if (DEBUG)
            imagesInRovnost = fileService.readFileLineByLine("IO/imagesInRovnostBACKUP");
        else{
            getDeeper(rovnostUuid);
            imagesInRovnost = this.imgsInRovnost; //reading from FEDORA
        }



        fileService.toOutputFile(imagesInRovnost, "IO/imagesInRovnost");

        //parsing inputfile zbytek.txt
        for (String imgToCheck : imagesToCheck){
            String imgParsedName = imgToCheck;
            String separator = "/mnt/imageserver/meditor/rovnost/";

            int startIndex = imgParsedName.indexOf(separator);
            startIndex += separator.length();

            imgParsedName = imgParsedName.substring(startIndex);
            imgParsedName = imgParsedName.split("\\.")[0];
            imagesToCheckParsed.add(imgParsedName);
        }

        //parsing images in Rovnost periodicum
        for (String imgToParse : imagesInRovnost){
            String imgParsedName = imgToParse;
            StringBuilder imgParsed = new StringBuilder();
            //String separator = "http://imageserver.mzk.cz/meditor/rovnost/";

            //if (!imgToParse.contains(separator)) //TODO you should control all uuids, change it
            //    continue;

            /*int startIndex = imgParsedName.indexOf(separator);
            startIndex += separator.length();


            imgParsedName = imgParsedName.substring(startIndex);*/
            if (imgParsedName.contains("</kramerius")){
                imgParsedName = imgParsedName.split("<")[0];
            }

            for (int i = imgParsedName.length()-1; i > 0 ; i--){
                if (imgParsedName.charAt(i) == '/'){
                    break;
                }
                else
                    imgParsed.append(imgParsedName.charAt(i));
            }

            //System.out.println(imgParsedName);
            String imgParsedReversed = imgParsed.reverse().toString();
            System.out.println(imgParsedReversed);
            imagesInRovnostParsed.add(imgParsedReversed);
        }

        fileService.toOutputFile(imagesInRovnostParsed, "IO/FROMrovnost");
        fileService.toOutputFile(imagesToCheckParsed, "IO/FROMzbytek");

        //comparing the images
        System.out.println("Are the images the same? : " + !Collections.disjoint(imagesToCheckParsed, imagesInRovnostParsed));
    }
}










