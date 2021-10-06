package cz.mzk.scripts;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/*
//TODO should be changed
Author: Marek Sedlo
Description:
INPUT: input json file "processesALL.json", which contains json of processes we want to filter and kill
OUTPUT: HTTP requests for killing the processes on krameriusbatch.mzk.cz
This script kills the reindex child processes of the set public parent, where is BATCH_STARTED
TODO make it more general, this single usage is bad
*/

public class CancelTheProcesses implements Script {
    private final String inputFile = "IO/processesALL.json";

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

    private List<String> parse(List<String> procs){
        boolean isNextReindex = false;
        List<String> uuids = new ArrayList<>();
        String separator = "uuid\":\"";
        String procuuid = null;

        for (String proc : procs){
            procuuid = proc;

            if (isNextReindex && proc.contains("reindex")){
                int objIndex = proc.indexOf(separator);
                objIndex += separator.length();

                procuuid = proc.substring(objIndex);
                procuuid = procuuid.split("\"")[0];
                uuids.add(procuuid);
            }

            if (proc.contains("setpublic") && proc.contains("BATCH_STARTED"))
                isNextReindex = true;
            else
                isNextReindex = false;
        }
        return uuids;
    }

    // function for reading the file
    // reading file char by char and separate it to strings splitted by char '}'
    // each of this strings is item of returning List

    private List<String> readData(String path){
        List<String> processes = new ArrayList<>();
        try {
            FileReader inputStream = null;

            inputStream = new FileReader(path);

            String proccess = null;
            int c;
            while ((c = inputStream.read()) != -1) {
                proccess += ((char) c);
                if ((char)c == '}'){
                    proccess += '\n';
                    processes.add(proccess);
                    proccess = "";
                }
            }
            inputStream.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return processes;
    }


    // here is just calling the functions above and making connection
    // TODO make function doing connection separated from this and make better authorization, authorization should be at begging of this script
    public void start(Properties prop){
        List<String> allProcesses = new ArrayList<>();
        List<String> uuidsToDelete = new ArrayList<>();
        allProcesses = this.readData(inputFile);
        uuidsToDelete = this.parse(allProcesses);
        this.toOutputFile(uuidsToDelete, "IO/processesOUTPUT");

        for (String uuid:uuidsToDelete){
            try { //426ccc17-25ea-4919-b08a-75bc2a0e6ae8
                System.out.println(uuid);
                URL url = new URL("http://krameriusbatch.mzk.cz/search/api/v4.6/processes/" + uuid + "?stop"); //PUT http://xxx.xxx.xxx.xxx/search/api/v4.6/processes/<uuid>?stop
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                String userCredentials = "krameriusAdmin:krameriusAdmin";
                String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));

                connection.setRequestProperty ("Authorization", basicAuth);
                connection.setRequestMethod("PUT");

                int responseCode = connection.getResponseCode();
                System.out.println(responseCode);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
