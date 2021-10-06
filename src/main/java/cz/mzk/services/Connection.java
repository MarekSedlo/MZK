package cz.mzk.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Connection {

    // making connection with URL of document
    public HttpURLConnection getConnection(String url, boolean DEBUG){
        try {
            URL urlObject = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            int responseCode = connection.getResponseCode();
            if (DEBUG)
                System.out.println("Response Code : " + responseCode);
            return connection;
        }
        catch (Exception responseException) {
            responseException.printStackTrace();
        }
        return null;
    }

    // reading response from URL connection and closing connection
    public StringBuilder read(HttpURLConnection connection){
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
        catch (Exception readingException){
            System.err.println("ERROR failed to read data");
            readingException.printStackTrace();
        }
        return null;
    }
}
