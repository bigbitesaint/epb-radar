package tw.com.mycompany.maptest;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tw.com.mycompany.maptest.R;


public class MyMap extends SupportMapFragment implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowCloseListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    private GoogleMap mMap;
    private MyMarker myMarker;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private boolean missionStarted = false;
    private static final int GPS_INTERVAL = 5000;
    private static final int MARKER_INTERVAL = 3000;
    private Map<Integer, MyMarker> markerArray = new ConcurrentHashMap<Integer, MyMarker>();
    private MyMarker mAttendingMarker;




    /************************************************************************************************
      *Handlers for processing
     **************************************************************************************************/
    private Handler mHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        // routinely updates markers
        public void handleMessage(Message message)
        {
            Log.i(MapsActivity.TAG, (String)message.obj);
            String[] chunks = ((String)message.obj).split(";");
            for (int it=0; it<chunks.length; ++it)
            {
                // assign fields
                MyMarker currMarker = null;
                String[] fields = chunks[it].split(",");
                int id = Integer.parseInt(fields[0]);
                int type = Integer.parseInt(fields[1]);
                double lat = Double.parseDouble(fields[2]);
                double lng = Double.parseDouble(fields[3]);

                Log.i(MapsActivity.TAG, "Checking id:"+id);
                if ( (currMarker=markerArray.get(id)) == null)
                {
                    Log.i(MapsActivity.TAG, id+" does't exists.");
                    switch(type)
                    {
                        case 1:
                            markerArray.put(id,
                                    new MyMarker(id,
                                                MyMarker.TYPE.CAR ,
                                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat,lng)).icon(MyMarker.CAR_BLUE).title(getString(R.string.examine_car)))));
                            break;
                        case 2:
                            markerArray.put(id,
                                    new MyMarker(id,
                                                MyMarker.TYPE.UNATTENDED,
                                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat,lng)).icon(MyMarker.MARKER_RED).title(getString(R.string.unattended_mission)))));
                            break;
                        case 3:
                            markerArray.put(id,
                                    new MyMarker(id,
                                                MyMarker.TYPE.ATTENDING,
                                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat,lng)).icon(MyMarker.MARKER_BLUE).title(getString(R.string.attended_mission)))));
                            break;

                    }
                }else
                {
                    currMarker.setPosition(new LatLng(lat,lng));
                    switch (type)
                    {
                        case 1:
                            currMarker.setIcon(MyMarker.CAR_BLUE);
                            break;
                        case 2:
                            currMarker.setIcon(MyMarker.MARKER_RED).setTitle(getString(R.string.unattended_mission));
                            break;
                        case 3:
                            currMarker.setIcon(MyMarker.MARKER_BLUE).setTitle(getString(R.string.attended_mission));
                            break;
                        case 4://not supposed to show, clear
                            currMarker.remove();
                            break;

                    }

                }
            }

        }
    };
    private Handler mMarkerHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        // updates marker's details.  only run once per marker
        public void handleMessage(Message message) {
            String[] fields = ((String)message.obj).split(",");
            int markerId = Integer.parseInt(fields[0]);
            String reporter = fields[1];
            String reason = fields[2];
            String cellphone = fields[3];
            boolean needCompany = "1".equals(fields[4]);
            boolean needReply = "1".equals(fields[5]);
            markerArray.get(markerId)
                    .setReporter(reporter)
                    .setReason(reason)
                    .setCellphone(cellphone)
                    .setNeedCompany(needCompany)
                    .setNeedReply(needReply)
                    .setHasData();
            Log.i(MapsActivity.TAG,"Data set!");
        }
    };


    private Runnable httpProcess = new Runnable(){
        public void run()
        {
        // while it is not detached
        while (getContext() != null) {
            Message message = mHandler.obtainMessage();
            String response = DBHelper.executeQuery(((MapsActivity) getContext()).getUserId());
            if (response.length() > 0)
            {
                message.obj = response;
                message.sendToTarget();
            }else
            {
                Log.i(MapsActivity.TAG,"No response from server.");
            }
            SystemClock.sleep(MARKER_INTERVAL);
        }

        }
    };

    /**************************************************************************************************
     * prototypes for threads
     ****************************************************************************************************/

    private Runnable httpUpdateProcess = new Runnable(){
        public void run()
        {
            // if it's not detached
            if (getContext() != null) {
                String result = DBHelper.executeUpdate(((MapsActivity) getContext()).getUserId(), mLastLocation.getLatitude(), mLastLocation.getLongitude());
                Log.i(MapsActivity.TAG, result);
            }
        }
    };

    private Runnable markerUpdateProcess = new Runnable(){
        public void run()
        {
            while (true)
            {
                for (Iterator it = markerArray.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    if (!((MyMarker) entry.getValue()).hasData()) {
                        Message message = mMarkerHandler.obtainMessage();
                        String response = DBHelper.getMarkerDetail((int) entry.getKey());
                        if (response != null && response.length() > 0) {
                            message.obj = response;
                            message.sendToTarget();
                        } else {
                            Log.i(MapsActivity.TAG, "No response from server.");
                        }
                    }

                }
                SystemClock.sleep(MARKER_INTERVAL);
            }
        }
    };

    /************************************************************
     * override methods
     *************************************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        enableGps();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // A car's default location is at hcepb
        LatLng hcepb = new LatLng(24.8291459, 121.0113723);

        // hook listener
        mMap.setOnMarkerClickListener(this);

        // add user's car on map
        myMarker = new MyMarker(0, MyMarker.TYPE.CAR,mMap.addMarker(new MarkerOptions().position(hcepb).icon(MyMarker.CAR_RED).title(getString(R.string.examine_car))));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hcepb,15));

        // add current car marker to marker array
        markerArray.put(0, myMarker);

        // start two background threads
        Thread thread = new Thread(httpProcess);
        thread.start();

        Thread threadMarker = new Thread(markerUpdateProcess);
        threadMarker.start();
    }

    @Override
    public void onInfoWindowClose(Marker marker)
    {
        missionDeselected();
    }


    @Override
    public boolean onMarkerClick(Marker marker)
    {
        MyMarker myMarker = queryMyMarker(marker);

        // if the marker is a car, do nothing
        if (myMarker.getType ()== MyMarker.TYPE.CAR)
            return true;

        MyDialog dialog = new MyDialog().setMarker(myMarker);
        dialog.show(getFragmentManager(),"Notice");

        return false;
    }




    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_maps, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        switch(menuItem.getItemId())
        {
            case R.id.menu_item_map_1:

                if (!missionStarted) {
                    startMission();
                    menuItem.setTitle(getString(R.string.stop_mission));
                }
                else {
                    stopMission();
                    menuItem.setTitle(getString(R.string.start_mission));
                }
                missionStarted = !missionStarted;
                break;
            case R.id.menu_item_map_2:
                break;
        }
        return false;
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case MapsActivity.MY_PERMISSION_FINE_LOCATION:
                if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED)
                    Toast.makeText(getContext(), R.string.permission_denied, Toast.LENGTH_LONG).show();
                break;

        }

    }

    @Override
    public void onConnectionSuspended(int cause)
    {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result)
    {
        Toast.makeText(getContext(),"Connection failed!",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        mLastLocation =location;
        setCarPosition(new LatLng(location.getLatitude(),location.getLongitude()));
        Thread thread = new Thread(httpUpdateProcess);
        thread.start();
        Log.i(MapsActivity.TAG,"("+location.getLatitude()+","+location.getLongitude()+")");
    }

    @Override
    public void onStart()
    {
        mGoogleApiClient.connect();
        super.onStart();

    }


    @Override
    public void onStop()
    {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /***************************************************************************
     *  Custom methods
     *****************************************************************************/

    public boolean enableGps()
    {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build();
        }
        return true;
    }



    public MyMarker queryMyMarker(Marker marker)
    {
        // iterate through all pairs of hashmap entries
        for (Iterator it = markerArray.entrySet().iterator(); it.hasNext();)
        {
            MyMarker val = ((MyMarker)(( (Map.Entry) it.next()).getValue()));
            if ( val.getMarker().equals(marker))
                return val;
        }
        return null;
    }

    public void setCarPosition(LatLng latlng)
    {
        myMarker.setPosition(latlng);
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,15));
    }

    public void setAttendingMarker(MyMarker marker)
    {
        mAttendingMarker = marker;
    }

    public MyMarker getAttendingMarker()
    {
        return mAttendingMarker;
    }

    public void missionSelected(MyMarker myMarker)
    {
        View footer = (View)getActivity().findViewById(R.id.info_footer);
        footer.setVisibility(View.VISIBLE);
    }

    public void missionDeselected()
    {
        View footer = (View)getActivity().findViewById(R.id.info_footer);
        footer.setVisibility(View.GONE);
    }

    public void startMission()
    {
        LocationManager lm = (LocationManager) getActivity().getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled, networkEnabled;
        networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        if (networkEnabled && gpsEnabled) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                setCarPosition(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            }

            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(GPS_INTERVAL);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }else
            Toast.makeText(getContext(),"GPS or NETWORK unavailable!", Toast.LENGTH_SHORT).show();
        myMarker.setIcon(MyMarker.CAR_BLUE);
    }

    public void stopMission()
    {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        myMarker.setIcon(MyMarker.CAR_RED);
    }
}
