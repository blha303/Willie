package com.drtshock.willie.github;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GistHelper {

    private static final String GITHUB_API_URL = "https://api.github.com/";
    private static final String GIST_API_LOCATION = "gists";
    private static final String GIST_URL = GITHUB_API_URL + GIST_API_LOCATION;

    private static final String DESCRIPTION = "Willie pasted this on ";

    private static final Logger LOG = Logger.getLogger(GistHelper.class.getName());

    /**
     * Paste a String to Gist then returns the link to the gist
     *
     * @param toGist the String to paste
     * @return the link to the paste
     */
    public static String gist(String toGist) {
        LOG.fine("Started to Gist something...");
        OutputStream out = null;
        InputStream in = null;
        try {
            URL url = new URL(GIST_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("content-type", "application/json; charset=utf-8");

            LOG.fine("Request built, now creating JSON object to send...");

            JsonObject res = new JsonObject();
            res.addProperty("description", DESCRIPTION + date());
            res.addProperty("public", true);
            JsonObject fileList = new JsonObject();
            JsonObject file = new JsonObject();
            file.addProperty("content", toGist);
            fileList.add("WilliePaste-" + date().replace(' ', '-') + ".txt", file);
            res.add("files", fileList);
            String jsonString = res.toString();

            LOG.fine("Json object created: " + jsonString);

            connection.setRequestProperty("Content-Length", Integer.toString(jsonString.length()));

            connection.connect();

            LOG.fine("Sending...");
            out = connection.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write(jsonString);
            writer.flush();
            writer.close();

            LOG.fine("Reading response...");
            in = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(in));
            String line, response = "";
            while ((line = rd.readLine()) != null) {
                response = response + line;
            }
            rd.close(); //close the reader

            LOG.fine("Response received: " + response);

            JsonObject responseJson = new JsonParser().parse(response).getAsJsonObject();

            String link = responseJson.get("html_url").getAsString();

            LOG.fine("Gist successful! Link: " + link);
            return link;
        } catch (IOException e) {
            LOG.severe("Failed to Gist, error follows:");
            LOG.log(Level.SEVERE, e.getMessage(), e);
            LOG.severe("This is what I was trying to Gist:");
            LOG.severe("\n##########\n" + toGist + "\n##########");
            LOG.severe("Failed to Gist, error above.");
            return "Error. Limit exceeded?";
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOP
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOP
                }
            }
        }
    }

    private static String date() {
        return new SimpleDateFormat("EEEE dd MMMM YYYY").format(new Date());
    }
}
