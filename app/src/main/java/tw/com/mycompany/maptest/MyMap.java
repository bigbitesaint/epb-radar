package tw.com.mycompany.maptest;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tw.com.mycompany.maptest.R;

import static android.support.v7.appcompat.R.styleable.AlertDialog;


public class MyMap extends SupportMapFragment implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    private GoogleMap mMap;
    private MyMarker carMarker;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private boolean missionStarted = false;
    private static final int GPS_INTERVAL = 5000;
    private static final int MARKER_INTERVAL = 3000;
    private Map<Integer, MyMarker> markerArray = new ConcurrentHashMap<Integer, MyMarker>();
    private MyMarker mAttendingMarker;
    private MyDialog mDialog;
    private boolean footerExpanded = false;
    private Polyline mAttendingLine = null;



    /************************************************************************************************
      *Handlers for processing
     **************************************************************************************************/
    private Handler mMarkerTrackUpdateProcessHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        // routinely updates markers
        public void handleMessage(Message message)
        {
            if (getActivity() == null)
                return;

            Log.i(MapsActivity.TAG, "[POLL]"+(String)message.obj);
            boolean isAttending = false;
            String[] chunks = ((String)message.obj).split(";");
            for (int it=0; it<chunks.length; ++it)
            {
                // assign fields
                MyMarker currMarker = null;
                String[] fields = chunks[it].split(",");
                int id;
                int type;
                int attendee;
                double lat;
                double lng;
                try {
                    id = Integer.parseInt(fields[0]);
                    type = Integer.parseInt(fields[1]);
                    attendee = Integer.parseInt(fields[2]);
                    lat = Double.parseDouble(fields[3]);
                    lng = Double.parseDouble(fields[4]);
                } catch (NumberFormatException nfe)
                {
                    Log.e(MapsActivity.TAG, "Number format error!");
                    continue;
                }
                String ts = fields[5];
                String driver = null;
                if (fields.length > 6)
                 driver = fields[6];

                // if this marker hasn't been recorded
                if ( (currMarker=markerArray.get(id)) == null)
                {
                    switch(type)
                    {
                        case 1:
                            markerArray.put(id,
                                    new MyMarker(id,
                                                MyMarker.TYPE.CAR ,
                                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat,lng)).icon(MyMarker.getMarkerBitmap("CAR_BLUE"))),
                                                MyMap.this
                                    ).setTitle(driver));
                            break;
                        case 2:
                            markerArray.put(id,
                                    new MyMarker(id,
                                                MyMarker.TYPE.UNATTENDED,
                                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat,lng)).icon(MyMarker.getMarkerBitmap("MARKER_RED"))),
                                                MyMap.this
                                    ));
                            break;
                        case 3:
                            markerArray.put(id,
                                    new MyMarker(id,
                                                MyMarker.TYPE.ATTENDING,
                                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat,lng)).icon(MyMarker.getMarkerBitmap("MARKER_BLUE"))),
                                                MyMap.this
                                    ));
                            break;

                    }
                }else // if this marker is already on map
                {
                    currMarker.setPosition(new LatLng(lat, lng));
                    currMarker.setExist(true);
                    switch (type) {
                        case 1:
                            currMarker.setIcon(MyMarker.getMarkerBitmap("CAR_BLUE")).setType(MyMarker.TYPE.CAR).setTitle(driver);
                            break;
                        case 2:
                            currMarker.setIcon(MyMarker.getMarkerBitmap("MARKER_RED")).setType(MyMarker.TYPE.UNATTENDED);
                            break;
                        case 3:
                            currMarker.setIcon(MyMarker.getMarkerBitmap("MARKER_BLUE")).setType(MyMarker.TYPE.ATTENDING);
                            break;
                        case 4://not supposed to show, clear
                            currMarker.remove();
                            break;
                    }
                }

                // check if I am attending this marker
                if (attendee == ((MapsActivity)getActivity()).getUserId())
                {
                    if (!hasMission()) {
                        markerArray.get(id).startMission(attendee);
                    }
                    missionSelected(markerArray.get(id));
                    isAttending = true;
                }
            }

            // delete unused marker & unset exist flag
            for (Iterator it = markerArray.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                MyMarker myMarker;
                if (entry.getValue() instanceof MyMarker)
                    myMarker =  (MyMarker)entry.getValue();
                else
                    continue;

                if (myMarker.getExist() == false) {
                    markerArray.remove(entry.getKey());
                    myMarker.remove();
                    Log.i(MapsActivity.TAG, "Removed!");
                }else if (myMarker.getId() != 0) // the car representing user himself should be persistent
                    myMarker.setExist(false);

            }

            if (!isAttending)
            {
                missionDeselected();
            }
            MyMarker.markerSynced.set(true);
        }
    };
    private Handler mMarkerDetailUpdateProcessHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        // updates marker's details.  only run once per marker
        public void handleMessage(Message message) {
            String[] fields = ((String)message.obj).split(",");
            int markerId=-1;
            try {
                markerId = Integer.parseInt(fields[0]);
            }catch (NumberFormatException nfe)
            {
                Log.e(MapsActivity.TAG, "Number format error!");
            }
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



    /**************************************************************************************************
     * prototypes for threads
     ****************************************************************************************************/

    private Runnable markerTrackUpdatePoller = new Runnable(){
        public void run()
        {
        // while it is not detached
        while (getContext() != null) {
            Message message = mMarkerTrackUpdateProcessHandler.obtainMessage();
            String response = DBHelper.executeQuery(((MapsActivity) getContext()).getUserId());
            if (response == null)
            {
                message.obj = "";
                message.sendToTarget();
            }
            else if (response.length() > 0)
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


    private Runnable markerTrackUpdateProcess = new Runnable(){
        public void run()
        {
            // if it's not detached
            if (getContext() != null) {
                String result = DBHelper.executeUpdate(((MapsActivity) getContext()).getUserId(), mLastLocation.getLatitude(), mLastLocation.getLongitude());
                Log.i(MapsActivity.TAG, result);
            }
        }
    };

    private Runnable markerDetailUpdateProcess = new Runnable(){
        public void run()
        {
            while (true)
            {
                for (Iterator it = markerArray.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    if (!((MyMarker) entry.getValue()).hasData()) {
                        Message message = mMarkerDetailUpdateProcessHandler.obtainMessage();
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
    public void onPause(){
        super.onPause();
        if (mDialog != null)
            mDialog.dismiss();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        enableGps();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // disable rotate function
        googleMap.getUiSettings().setRotateGesturesEnabled(false);

        // A car's default location is at hcepb
        LatLng hcepb = new LatLng(24.8280508,121.0132852);

        // hook listener
        mMap.setOnMarkerClickListener(this);



        // add user's car on map
        carMarker = new MyMarker(0, MyMarker.TYPE.CAR,mMap.addMarker(new MarkerOptions().position(hcepb).icon(MyMarker.getMarkerBitmap("CAR_RED")).title(getString(R.string.examine_car))), this);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hcepb,15));

        // add current car marker to marker array
        markerArray.put(0, carMarker);

    }


    @Override
    public boolean onMarkerClick(Marker marker)
    {
        MyMarker carMarker = queryMyMarker(marker);

        // if the marker is a car, do nothing
        if (carMarker == null || carMarker.getType ()== MyMarker.TYPE.CAR)
            return false;

        mDialog = new MyDialog().setMarker(carMarker);
        mDialog.show(getFragmentManager(),"Notice");
        return false;
    }




    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        if (!missionStarted)
            menu.add(0, R.id.menu_item_map_1, 0, R.string.gps_start);
        else
            menu.add(0, R.id.menu_item_map_1, 0, R.string.gps_stop);
        menu.add(0, R.id.menu_item_map_2, 0, getString(R.string.log_out)+((MapsActivity)getActivity()).getUserName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        switch(menuItem.getItemId())
        {
            case R.id.menu_item_map_1:
                if (!missionStarted) {
                    // a user does not necessarily need a destination marker to start mission.
                    startCarTrack();
                }
                else {
                    if (hasMission())
                    {
                        Toast.makeText(getContext(),R.string.has_mission_cannot_stop,Toast.LENGTH_LONG).show();
                    }else
                        stopCarTrack();
                }
                break;
            case R.id.menu_item_map_2:
                ((MapsActivity)getActivity()).reset();
                break;
        }
        return false;
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        // start two background threads
        // it's necessary to start these two threads in this callback, otherwise the handler may be involked while map is not connected
        Thread thread = new Thread(markerTrackUpdatePoller);
        thread.start();

        Thread threadMarker = new Thread(markerDetailUpdateProcess);
        threadMarker.start();
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
        Thread thread = new Thread(markerTrackUpdateProcess);
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

    public boolean popMyMarker(int id)
    {
        return (markerArray.remove(id) != null);
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
        carMarker.setPosition(latlng);
    }

    public void updateAttendingLine()
    {
        // show connecting line
        unsetAttendingLine();
        mAttendingLine = mMap.addPolyline(new PolylineOptions().add(carMarker.getPosition(), mAttendingMarker.getPosition()).width(20.0f).color(Color.parseColor("#80FF0000")));
    }

    public void unsetAttendingLine()
    {
        if (mAttendingLine != null) {
            mAttendingLine.remove();
            mAttendingLine = null;
        }
    }

    public MyMap setAttendingMarker(MyMarker marker)
    {
        mAttendingMarker = marker;
        updateAttendingLine();
        return this;
    }

    public MyMap unsetAttendingMarker()
    {
        mAttendingMarker = null;
        unsetAttendingLine();
        return this;
    }

    public MyMarker getAttendingMarker()
    {
        return mAttendingMarker;
    }

    public GoogleMap getGMap()
    {
        return mMap;
    }

    public boolean hasMission()
    {
        return (mAttendingMarker != null);
    }

    public void missionSelected(MyMarker marker)
    {
        if (marker != null)
            setAttendingMarker(marker);


        // check mMap to know whether google map is ready
        if (mMap != null)
            startCarTrack();
    /*
       Fill footer view
       */
        View footer = (View)getActivity().findViewById(R.id.info_footer);
        TextView header = (TextView)getActivity().findViewById(R.id.footer_header);
        TextView reporter = (TextView)getActivity().findViewById(R.id.footer_reporter);
        TextView reason = (TextView)getActivity().findViewById(R.id.footer_reason);
        TextView cellphone = (TextView)getActivity().findViewById(R.id.footer_cellphone);
        CheckBox need_company = (CheckBox)getActivity().findViewById(R.id.footer_need_company_checkbox);
        CheckBox need_reply = (CheckBox)getActivity().findViewById(R.id.footer_need_reply_checkbox);
        if (!reason.hasOnClickListeners()) {
            reason.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    footerExpanded = !footerExpanded;
                    missionSelected(null);
                }
            });
        }
        if (!header.hasOnClickListeners()) {
            header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(mAttendingMarker.getPosition()));
                }
            });
        }
        if (mAttendingMarker.hasData()) {
            // show footer only when data is available
            footer.setVisibility(View.VISIBLE);

            reporter.setText(mAttendingMarker.getReporter());
            cellphone.setText(mAttendingMarker.getCellphone());
            need_company.setChecked(mAttendingMarker.getNeedCompany());
            need_reply.setChecked(mAttendingMarker.getNeedReply());
            String reasonText = mAttendingMarker.getReason();
            if (!footerExpanded && reasonText.length() > 8)
                reasonText = reasonText.substring(0, 8).concat("...");
            reason.setText(reasonText);
        }

    }

    public void missionDeselected()
    {
        unsetAttendingMarker();
        // hide footer
        View footer = (View)getActivity().findViewById(R.id.info_footer);
        footer.setVisibility(View.GONE);
    }

    public void stopCarTrack()
    {
        if (!missionStarted)
            return;
        missionStarted = false;
        // mark car as stopped
        carMarker.setIcon(MyMarker.getMarkerBitmap("CAR_RED"));
        // stop GPS tracker
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    public void startCarTrack(){
        if (missionStarted)
            return;

        // start GPS tracker
        LocationManager lm = (LocationManager) getActivity().getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled, networkEnabled;
        networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        if (networkEnabled && gpsEnabled) {
            missionStarted = true;
            // mark car as started
            carMarker.setIcon(MyMarker.getMarkerBitmap("CAR_BLUE"));
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                setCarPosition(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            }

            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(GPS_INTERVAL);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }else{
            //Toast.makeText(getContext(),"GPS or NETWORK unavailable!", Toast.LENGTH_SHORT).show();
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setMessage(getString(R.string.need_gps));
            alertDialog.setTitle(getString(R.string.error));
            alertDialog.setButton(android.app.AlertDialog.BUTTON_NEUTRAL, getString(R.string.retry), new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int which)
                {
                    dialogInterface.dismiss();
                    startCarTrack();
                }
            });
            alertDialog.show();
        }


    }

}
