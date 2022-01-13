package cz.mzk.scripts;

import cz.mzk.services.Connection;
import cz.mzk.services.FileIO;
import cz.mzk.services.XMLparser;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Properties;

public class countScans implements Script{
    public static final boolean DEBUG = false;
    FileIO fileIO = new FileIO();
    Connection con = new Connection();
    XMLparser parser = new XMLparser();
    private int scans = 0;

    private String checkPidPrefix(String pid){
        String uuid = pid;
        if (!pid.startsWith("uuid:"))
            uuid = "uuid:" + pid;
        return uuid;
    }

    private void getDeeper(String parentPid){
        parentPid = checkPidPrefix(parentPid);
        HttpURLConnection connection = con.getConnection("http://dk-fedora.infra.mzk.cz/fedora/objects/" + parentPid + "/datastreams/RELS-EXT/content", DEBUG);
        StringBuilder FOXML = con.read(connection);
        List<String> childrenUuids = parser.getFOXMLChildrenUuids(FOXML);
        if (childrenUuids.size() > 0){
            for (String childUuid : childrenUuids){
                getDeeper(childUuid);
            }
        } else {
            scans++;
        }
    }

    public void start(Properties prop) {
        List<String> inputPids = fileIO.readFileLineByLine("IO/scans/input.txt");
        int pidNumber = 0;
        for (String pid:inputPids){
            System.out.println("Num of pids checked: " + pidNumber);
            System.out.println("Checking pid: " + pid);
            getDeeper(pid);
            pidNumber++;
        }
        System.out.println("All scans: " + scans);
    }
}
