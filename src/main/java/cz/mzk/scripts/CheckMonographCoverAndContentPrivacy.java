package cz.mzk.scripts;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/*
//TODO should be changed

Author: Marek Sedlo
Description:
INPUT:  Uuids of monographs in input file monographs
OUTPUT: Uuids of pages (in output file), which are private and should be public (covers, content and titlepages) (output.txt)
BONUSOUTPUT: Uuids of monographs, which does not exist in fedora FOXML database (solrUUIDSNotInFedora.txt)

This script is useless, cos of we can (and it is right method) change global settings for reading covers, titlepages and content of the books

*/

public class CheckMonographCoverAndContentPrivacy implements Script{
    public static final boolean DEBUG = false;
    public final String host = "dk-fedora.infra.mzk.cz";
    // dk-fedora.infra.mzk.cz
    // localhost:8080
    private int connectionResponseCode = 0;
    private String uuid = null;
    private String url = null;
    private List<String> uuids = null; //uuids from file
    //private final String[] checkForThesePageTypes = new String[]{"TableOfContents", "FrontCover", "TitlePage", "BackCover"};
    private final String[] checkForTheseFrontPageTypes = new String[]{"tableofcontents", "frontcover", "titlepage"}; // page type is made to lowercase
    private final String[] checkForTheseBackPageTypes = new String[]{"backcover"}; // page type is made to lowercase
    private final String inputFile = "IO/monographs"; //path marekscripts/IO/monographs
    private final int numOfNormalPages = 5; //how many normal pages read before skipping

    private List<String> outputUUIDS = new ArrayList<>();
    private List<String> solrUUIDSNotFoundInFedora = new ArrayList<>();


    private void toOutputFile(List<String> uuidsToOutput, String fileName){
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(fileName, "UTF-8");
            for (String uuid : uuidsToOutput)
                writer.println(uuid);
            writer.close();
        } catch (Exception e){
            System.err.println("ERROR: cannot create output file");
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    System.err.println("ERROR: cannot close output file");
                    e.printStackTrace();
                }
            }
        }
    }

    // get type of page from BIBLIO_MODS datastream of pageUUID
    private String pageType(String pageUUID){

        String pageURL = "http://" + host + "/fedora/objects/" + pageUUID + "/datastreams/BIBLIO_MODS/content";

        if (DEBUG){
            System.out.println("Reading BIBLIO_MODS from page: ");
            System.out.println(pageUUID);
        }

        //make connection
        HttpURLConnection connection = this.getConnection(pageURL);

        if (this.connectionResponseCode == 404) {
            System.out.println("This page uuid: " + pageUUID + " was NOT FOUND, connection response code: " + this.connectionResponseCode + "\nSkipping");
            return "pageNotFound";
        }

        StringBuilder BIBLIO_MODSresponse = new StringBuilder();

        //read XML and close connection
        BIBLIO_MODSresponse = this.readXML(connection);
        if (BIBLIO_MODSresponse == null)
            System.err.println("ERROR: wrong BIBLIO_MODS page response! url: " + pageUUID);

        List<String> pageTypes = getXMLobjects(BIBLIO_MODSresponse.toString(), "mods:part type=\"", "\"", 0, false); // returns the type of XML object from XML BIBLIO_MODS datastream
        String pageType = "";

        if (pageTypes.size() < 1)
            System.err.println("ERROR: page type not found!");
        else
            pageType = pageTypes.get(0);

        if (DEBUG){
            System.out.println("Page type: " + pageType);
            System.out.print("\n");
        }

        pageType = pageType.toLowerCase(Locale.ENGLISH);
        return pageType;
    }


    private int countSubstrings(String fromHere, String countThis){
        int lastIndex = 0;
        int count = 0;
        while (lastIndex != -1){
            lastIndex = fromHere.indexOf(countThis, lastIndex);

            if (lastIndex != -1){
                count++;
                lastIndex += countThis.length();
            }
        }
        return count;
    }

    //returns text from separatorBegin to separatorEnd for the num of objects got from the maxIndex
    //expects separator begins like: "info:fedora/uuid:"
    //                               "info:fedora/model:"
    //separator ends like: "\""
    //                     "<"
    // this.isPageParent is information about this.url and this.uuid --> url and uuid of page parent
    private List<String> getXMLobjects(String response, String separatorBegin, String separatorEnd,  int maxIndex, boolean getPages){
        List<String> XMLobjects = new ArrayList<>();
        String XMLobj = null;
        String response_for_cutting = response;
        int startNormalPagesCount = 0;
        int endNormalPagesCount = 0;


        for (int i=0; i <= maxIndex; i++){ // finds uuid by index
            if (getPages){
                //index 0 is parent of pages, it is not a page
                if (startNormalPagesCount < numOfNormalPages){ //check pages to sequence of 5 normal pages from begging, if it is shorter, than book with 5 normal pages, read whole book
                    int objIndex = response_for_cutting.indexOf(separatorBegin);
                    objIndex += separatorBegin.length();

                    XMLobj = response_for_cutting.substring(objIndex);
                    XMLobj = XMLobj.split(separatorEnd)[0];

                    response_for_cutting = response_for_cutting.substring(objIndex, response_for_cutting.length());

                    String typeOfPage = "";
                    if (!Objects.equals(this.uuid, "uuid:" + XMLobj)){ //it is not parent
                        if (DEBUG)
                            System.out.println("start page: ");
                        typeOfPage = pageType("uuid:" + XMLobj);
                        if (Objects.equals(typeOfPage, "pageNotFound"))
                            continue;
                        if (Arrays.asList(this.checkForTheseFrontPageTypes).contains(typeOfPage))
                            XMLobjects.add(XMLobj);
                        //if (Objects.equals(typeOfPage, "NormalPage"))
                        if (Objects.equals(typeOfPage, "normalpage")) //pagetype is made to lowercase
                            startNormalPagesCount++;
                    }
                    if (i == 0)
                        XMLobjects.add(XMLobj);
                }
            } else { // current object does not have pages
                int objIndex = response_for_cutting.indexOf(separatorBegin);
                objIndex += separatorBegin.length();

                XMLobj = response_for_cutting.substring(objIndex);
                XMLobj = XMLobj.split(separatorEnd)[0];

                response_for_cutting = response_for_cutting.substring(objIndex, response_for_cutting.length());
                XMLobjects.add(XMLobj);
            }
        }

        response_for_cutting = response;

        if ((getPages) && (startNormalPagesCount == numOfNormalPages)){ //reading pages and book has at least 5 normal pages (it means some pages are skipped)
            //read pages from end to begging and again stop, when count of 5 normal pages is reached
            for (int i = maxIndex; i >= 0; i--) {
                if (endNormalPagesCount < numOfNormalPages){ //check pages to sequence of 5 normal pages from the end
                    int objIndex = response_for_cutting.lastIndexOf(separatorBegin);
                    objIndex += separatorBegin.length();

                    XMLobj = response_for_cutting.substring(objIndex);
                    XMLobj = XMLobj.split(separatorEnd)[0];

                    response_for_cutting = response_for_cutting.substring(0, objIndex-separatorBegin.length());

                    String typeOfPage = "";
                    if (!Objects.equals(this.uuid, "uuid:" + XMLobj)){ //it is not parent (typically monographunit)
                        if (DEBUG)
                            System.out.println("end page: ");
                        typeOfPage = pageType("uuid:" + XMLobj);
                        if (Objects.equals(typeOfPage, "pageNotFound"))
                            continue;
                        if (Arrays.asList(this.checkForTheseBackPageTypes).contains(typeOfPage))
                            XMLobjects.add(XMLobj);
                        //if (Objects.equals(typeOfPage, "NormalPage"))
                        if (Objects.equals(typeOfPage, "normalpage")) //pagetype is made to lowercase
                            endNormalPagesCount++;
                    }
                }
            }
        }

        return XMLobjects;
    }

    // return all children UUIDS of specific XML URL, just children in this url, not children of children
    private List<String> getChildrenUUIDs(String response, boolean getPages){
        List<String> chUUIDs = null;
        int childOccurs = countSubstrings(response, "info:fedora/uuid:");

        chUUIDs = getXMLobjects(response, "info:fedora/uuid:", "\"", childOccurs-1, getPages);

        if (((chUUIDs.size()) != childOccurs) && (!getPages))
            System.err.println("ERROR: Children number is not the same like number of children substrings");

        for (int i=0; i<chUUIDs.size(); i++)
            chUUIDs.set(i, "uuid:" + chUUIDs.get(i));

        chUUIDs.remove(0); //remove the parent url

        return chUUIDs;
    }

    // parse XML monograph and get the page
    // response is one XML object from fedora
    private void parseXML(StringBuilder response){
        assert (response != null);
        assert (response.indexOf("info:fedora/model") != -1); // this substring has to be in XML object (RELS-EXT datastream)
        assert (response.indexOf("info:fedora/uuid:") != -1); // this substring has to be in XML object (RELS-EXT datastream)

        boolean isPageParent = false;

        String XMLobjectType = "";

        List<String> XMLobjectTypes = getXMLobjects(response.toString(), "info:fedora/model:", "\"", 0, false); // returns the type of XML object from XML RELS-EXT datastream
        if (XMLobjectTypes.size() < 1)
            System.err.println("ERROR: XML object type not found!");
        else
            XMLobjectType = XMLobjectTypes.get(0);

        if (DEBUG){
            System.out.println(this.uuid);
            System.out.println(XMLobjectType);
        }

        if (!Objects.equals(XMLobjectType, "page")){ //should have children
            if (response.toString().contains("hasPage"))
                isPageParent = true;
            else
                isPageParent = false;
            if (DEBUG){
                System.out.println("##########################");
                System.out.println("\n\n\n");
            }

            List<String> childrenUUIDs = getChildrenUUIDs(response.toString(), isPageParent);
            if (childrenUUIDs.size() == 0)
                System.out.println("No cover and content children");
            //System.out.println("Children: " + childrenUUIDs);

            for (String childUUID : childrenUUIDs){
                this.uuid = childUUID;
                this.url = "http://" + host + "/fedora/objects/" + this.uuid + "/datastreams/RELS-EXT/content";

                if (DEBUG)
                    System.out.println("Reading RELS-EXT from child: ");
                //make connection
                HttpURLConnection connection = this.getConnection(this.url);

                if (this.connectionResponseCode == 404){
                    System.out.println("This child uuid: " + childUUID + " was NOT FOUND, connection response code: " + this.connectionResponseCode + "\nSkipping");
                    continue;
                }

                StringBuilder childResponse = new StringBuilder();

                //read XML and close connection

                childResponse = this.readXML(connection);
                if (childResponse != null)
                    this.parseXML(childResponse);
                else
                    System.err.println("ERROR: wrong child response! url: " + this.url);

            }
        }
        else { //pages here are filtered by type

            List<String> policy = getXMLobjects(response.toString(), ">policy:", "<", 0, false);
            if (DEBUG)
                System.out.println("Page policy: " + policy.get(0));

            if (Objects.equals(policy.get(0), "private")){
                if (DEBUG)
                    System.out.println("This uuid should be public: " + this.uuid);
                outputUUIDS.add(this.uuid);
            }
            if (DEBUG)
                System.out.println();
        }
    }

    // reading XML response from URL connection and closing connection
    private StringBuilder readXML(HttpURLConnection connection){
        try {
            BufferedReader in = new BufferedReader( new InputStreamReader(connection.getInputStream()));
            String inputLine = null;

            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null){ //reading document line by line
                response.append(inputLine);
                response.append(System.getProperty("line.separator")); //adding newline after every line read
            }
            in.close(); // close connection, data are already read
            return response;
        }
        catch (Exception XMLreadingException){
            XMLreadingException.printStackTrace();
        }
        return null;
    }

    // making connection with URL of document
    private HttpURLConnection getConnection(String url){
        try {
            URL urlObject = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();

            int responseCode = connection.getResponseCode();
            this.connectionResponseCode = responseCode;
            if (DEBUG)
                System.out.println("Response Code : " + responseCode);
            return connection;
        }
        catch (Exception responseException) {
            responseException.printStackTrace();
        }
        return null;
    }

    // function for reading the file, returns list of uuids
    private List<String> loadUUIDs(String path){
        List<String> uuidList = new ArrayList<>();
        File inputFile = new File(path);
        BufferedReader reader = null;

        try {
            //Open file and load content
            reader = new BufferedReader(new FileReader(inputFile));
            String uuid = null;

            //Parse file by line
            while ((uuid = reader.readLine()) != null) {
                if (!uuid.contains("uuid:") && !uuid.contains("vc:")) {
                    System.err.println("ERROR: not a valid pid: " + uuid);
                } else
                    uuidList.add(uuid);
            }
        } catch (FileNotFoundException e){
            System.err.println("ERROR: cannot open file" + path);
            e.printStackTrace();
        }
        catch (IOException e){
            System.err.println("ERROR: cannot read file" + path);
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("ERROR: cannot close file: " + path);
                    e.printStackTrace();
                }
            }
        }
        return uuidList;
    }

    public void start(Properties prop){

        long start = System.nanoTime();
        long end = System.nanoTime();
        double seconds = 0.0;
        double minutes = 0.0;
        double hours = 0.0;
        double days = 0.0;

        //read file
        this.uuids = this.loadUUIDs(this.inputFile);

        //loop throught all files
        int count = 1;
        for (String uuid : this.uuids){
            if (count % 100 == 0){
                end = System.nanoTime();
                seconds = (end - start)/1_000_000_000.0;
                minutes = seconds/60.0;
                hours = minutes/60.0;
                days = hours/24.0;
                System.out.println("\nUuids read: " + (double)((count*100.0)/this.uuids.size()) + "% Time elapsed:\n" + seconds + " secs\n" + minutes + " minutes\n" + hours + " hours\n" + days + " days\n");
            }

            this.uuid = uuid;
            this.url = "http://" + this.host + "/fedora/objects/" + this.uuid + "/datastreams/RELS-EXT/content";


            System.out.println("Reading uuid from input file: " + uuid);
            //make connection
            HttpURLConnection connection = this.getConnection(this.url);

            if (this.connectionResponseCode == 404){
                System.out.println("This monograph uuid: " + this.uuid + " was NOT FOUND, connection response code: " + this.connectionResponseCode + "\nSkipping");
                this.solrUUIDSNotFoundInFedora.add(this.uuid);
                continue;
            }

            StringBuilder response = new StringBuilder();

            //read XML and close connection

            response = this.readXML(connection);
            if (response != null)
                this.parseXML(response);
            else
                System.err.println("ERROR: null response!");

            if (DEBUG){
                for(int i = 0; i < 20; i++)
                    System.out.println("##########################");
            }
            count++;
        }


       this.toOutputFile(this.outputUUIDS, "IO/output.txt");
       this.toOutputFile(this.solrUUIDSNotFoundInFedora, "IO/solrUUIDSNotInFedora.txt");

       System.out.println("Num of uuids for privacy change: " + this.outputUUIDS.size());

        end = System.nanoTime();
        seconds = (end - start)/1_000_000_000.0;
        minutes = seconds/60.0;
        hours = minutes/60.0;
        days = hours/24.0;

        System.out.println("Reading completed, time elapsed:\n" + seconds + " secs\n" + minutes + " minutes\n" + hours + " hours\n" + days + " days\n");
    }
}
