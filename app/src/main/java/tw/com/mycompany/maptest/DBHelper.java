package tw.com.mycompany.maptest;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

public class DBHelper{
    private static final String baseUrl = "http://172.23.2.230";

    private static String doQuery(String qry)
    {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(qry);
            Log.i(MapsActivity.TAG, url.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15 * 1000);
            connection.connect();
        } catch (IOException ioe)
        {
            Log.i(MapsActivity.TAG, ioe.getMessage());
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException e)
        {
            Log.i(MapsActivity.TAG, e.getMessage());
            return "";
        }
        String buffer=null;
        try {
            buffer = reader.readLine();
        } catch (IOException ioe)
        {
            Log.i(MapsActivity.TAG, ioe.getMessage());
        }
        return buffer;

    }

    public static String getMarkerDetail(int markerId)
    {
        return doQuery(baseUrl+"/app_webapi.php?action=getdetail&id="+markerId);
    }

    public static String executeUpdate(int id, double lat, double lng){
        return doQuery(baseUrl+"/app_webapi.php?action=updatecoor&id="+id+"&lat="+lat+"&lng="+lng);
    }

    public static String executeQuery(int id){
        return doQuery(baseUrl+"/app_webapi.php?action=getmarkers&id="+id);
    }

    public static String updateMission(int markerId, int disp)
    {
        return doQuery(baseUrl+"/app_webapi.php?action=updatemarker&id="+markerId+"&disp="+disp);
    }
}


