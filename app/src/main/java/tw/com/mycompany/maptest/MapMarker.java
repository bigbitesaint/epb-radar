package tw.com.mycompany.maptest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Administrator on 2016/9/12.
 */
public class MyMarker {
    private Marker mMarker;
    private MyMap mMyMap;
    private int mId;
    private String mReporter;
    private String mReason;
    private String mCellphone;
    private boolean mNeedCompany;
    private boolean mNeedReply;
    private boolean mExists = true;
    private boolean mIsSet = false;
    private Timestamp mTimestamp;
    private TYPE mType;
    public static AtomicBoolean markerSynced;

    public enum TYPE {
        CAR,
        UNATTENDED,
        ATTENDING,
        ATTENDED;

        //  align TYPE's ordinal values with MySql's type field definitions
        public int val() {
            return ordinal() + 1;
        }
    }

    ;

    static {
        markerSynced = new AtomicBoolean(true);
    }

    ;


    public static BitmapDescriptor getMarkerBitmap(String selector) {
        if (selector.compareTo("CAR_BLUE") == 0) {
            return BitmapDescriptorFactory.fromResource(R.drawable.car_blue);
        } else if (selector.compareTo("CAR_RED") == 0) {
            return BitmapDescriptorFactory.fromResource(R.drawable.car_red);
        } else if (selector.compareTo("MARKER_RED") == 0) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        } else if (selector.compareTo("MARKER_BLUE") == 0) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA);
        } else if (selector.compareTo("MARKER_GREEN") == 0) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        } else {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        }
    }

    public class MissionUpdateRunnable implements Runnable {
        int mId;
        int mType;
        int mUserId;

        public MissionUpdateRunnable(int id, int type, int userId) {
            mId = id;
            mType = type;
            mUserId = userId;
        }

        public void run() {
            String result = DBHelper.updateMission(mId, mType, mUserId);
            Log.i(MapsActivity.TAG, result);
        }
    }

    ;


    public MyMarker(int id, TYPE type, Marker marker, MyMap myMap) {
        mId = id;
        mType = type;
        mMarker = marker;
        mMyMap = myMap;
    }

    public int getId() {
        return mId;
    }


    public MyMarker setReporter(String reporter) {
        mReporter = reporter;
        return this;
    }

    public String getReporter() {
        return mReporter;
    }

    public MyMarker setReason(String reason) {
        mReason = reason;
        return this;
    }

    public String getReason() {
        return mReason;
    }

    public MyMarker setTimestamp(Timestamp timestamp) {
        mTimestamp = timestamp;
        return this;
    }

    public Timestamp getTimestamp() {
        return mTimestamp;
    }

    public MyMarker setType(TYPE type) {
        mType = type;
        return this;
    }

    public TYPE getType() {
        return mType;
    }

    public LatLng getPosition() {
        return mMarker.getPosition();
    }

    public void setPosition(LatLng position) {
        mMarker.setPosition(position);
    }

    public MyMarker setIcon(BitmapDescriptor bitmapDescriptor) {
        mMarker.setIcon(bitmapDescriptor);
        return this;
    }

    public Marker getMarker() {
        return mMarker;
    }

    public MyMarker setCellphone(String cellphone) {
        mCellphone = cellphone;
        return this;
    }

    public MyMarker setTitle(String title) {
        mMarker.setTitle(title);
        return this;
    }

    public MyMarker setNeedCompany(boolean needCompany) {
        mNeedCompany = needCompany;
        return this;
    }

    public MyMarker setNeedReply(boolean needReply) {
        mNeedReply = needReply;
        return this;
    }


    public MyMarker setExist(boolean exist)
    {
        this.mExists = exist;
        return this;
    }

    public boolean getExist()
    {
        return this.mExists;
    }

    public MyMarker showInfoWindow()
    {
        mMarker.showInfoWindow();
        return this;
    }

    public boolean getNeedCompany()
    {
        return mNeedCompany;
    }

    public boolean getNeedReply()
    {
        return mNeedReply;
    }

    public void remove()
    {
        mMarker.remove();
    }

    public String getCellphone()
    {
        return mCellphone;
    }

    public MyMarker setHasData()
    {
        mIsSet = true;
        return this;
    }

    public boolean hasData()
    {
        return mIsSet;
    }

    public void stopMission()
    {
        setType(MyMarker.TYPE.UNATTENDED);
        markerSynced.set(false);
        Thread thread = new Thread(new MissionUpdateRunnable(mId, MyMarker.TYPE.UNATTENDED.val(), 0));
        thread.start();
    }

    public void startMission(int userId)
    {
        setType(MyMarker.TYPE.ATTENDING);
        markerSynced.set(false);
        Thread thread = new Thread(new MissionUpdateRunnable(mId, MyMarker.TYPE.ATTENDING.val(), userId));
        thread.start();
    }

    public void finishMission()
    {
        setType(MyMarker.TYPE.ATTENDED);
        markerSynced.set(false);
        // remove marker from
        if (!mMyMap.popMyMarker(mId))
            Log.i(MapsActivity.TAG, "Marker deletion failed.");
        else
            Log.i(MapsActivity.TAG, "Marker deletion successed.");
        remove();
        Thread thread = new Thread(new MissionUpdateRunnable(mId, TYPE.ATTENDED.val(), 0));
        thread.start();
    }

}
