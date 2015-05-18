package se.kth.ict.id2203.sim;

import java.net.*;
import java.io.*;

public class Submit
{
    public static String submit(final String email, final String password, final String assignment) {
        String responseLine;
        try {
            final URL url = new URL("http://cloud7.sics.se:11700/submit");
            final HttpURLConnection con = (HttpURLConnection)url.openConnection();
            final String urlParameters = String.format("email=%s&password=%s&assignment=%s&abc=404", URLEncoder.encode(email, "UTF-8"), password, assignment);
            con.setDoOutput(true);
            final DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            final int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                responseLine = "Success";
            }
            else if (responseCode == 400) {
                responseLine = "Submission arguments error.";
            }
            else if (responseCode == 401) {
                responseLine = "Invalid email or password.";
            }
            else if (responseCode == 500) {
                responseLine = "Internal server error.";
            }
            else {
                responseLine = "error " + responseCode + ".";
            }
            con.disconnect();
        }
        catch (MalformedURLException e) {
            responseLine = "URL error.";
        }
        catch (ProtocolException e2) {
            responseLine = "Protocol exception.";
        }
        catch (IOException e3) {
            responseLine = "IO exception.";
        }
        return responseLine;
    }
}
