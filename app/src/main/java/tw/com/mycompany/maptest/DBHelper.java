package tw.com.mycompany.maptest;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class DBHelper{
    private static final String baseUrl = "http://172.23.2.230";
    //private static final String baseUrl = "http://www.hcepb.gov.tw/app";

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

    public static String updateMission(int markerId, int type, int userId)
    {
        return doQuery(baseUrl+"/app_webapi.php?action=updatemarker&id="+markerId+"&type="+type+"&attendee="+userId);
    }

    public static String doValidate(String userName, String passwd){
        return doQuery(baseUrl+"/app_webapi.php?action=validate&user_name="+userName+"&passwd="+passwd);
    }
}


