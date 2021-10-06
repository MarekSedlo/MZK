package cz.mzk.services;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileIO {
    public List<String> readFileLineByLine(String path){
        List<String> fileLines = new ArrayList<>();
        File inputFile = new File(path);
        BufferedReader reader = null;

        try{
            //Open file and load content
            reader = new BufferedReader(new FileReader(inputFile));
            String line = null;

            //readline
            while ((line = reader.readLine()) != null){
                fileLines.add(line);
            }
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: cannot open file" + path);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("ERROR: cannot read file" + path);
            e.printStackTrace();
        } finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("ERROR: cannot close file: " + path);
                    e.printStackTrace();
                }
            }
        }
        return fileLines;
    }


    public void toOutputFile(List<String> linesToOutput, String filepath){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filepath, "UTF-8");
            for (String line : linesToOutput)
                writer.println(line);
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

    public List<String> removeUuidPrefix(List<String> pids){
        String pid;
        for (int i = 0; i < pids.size(); i++){
            pid = pids.get(i);
            pid = pid.replace("uuid:", "");
            pids.set(i, pid);
        }
        return pids;
    }

    public List<String> addUuidPrefix(List<String> pids){
        String pid;
        for (int i = 0; i < pids.size(); i++){
            pid = pids.get(i);
            pid = "uuid:" + pid;
            pids.set(i, pid);
        }
        return pids;
    }
}
