package tw.com.mycompany.maptest;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.Timestamp;

/**
 * Created by Administrator on 2016/9/12.
 */
public class MyMarker{
    private Marker mMarker;
    private int mId;
    private String mReporter;
    private String mReason;
    private String mCellphone;
    private boolean mNeedCompany;
    private boolean mNeedReply;
    private boolean mIsSet=false;
    private Timestamp mTimestamp;
    private TYPE mType;
    public static BitmapDescriptor CAR_BLUE;
    public static BitmapDescriptor CAR_RED;
    public static BitmapDescriptor MARKER_RED;
    public static BitmapDescriptor MARKER_BLUE;
    public static BitmapDescriptor MARKER_GREEN;
    public enum TYPE {
        CAR,
        UNATTENDED{
            @Override
            public TYPE prev()
            {
                return this;
            }
        },
        ATTENDING,
        ATTENDED{
            @Override
            public TYPE next()
            {
                return this;
            }
        };
        public TYPE next()
        {
            return values()[ordinal()+1];
        }

        public TYPE prev()
        {
            return values()[ordinal()-1];
        }
    };


    static{
        MyMarker.CAR_BLUE = BitmapDescriptorFactory.fromResource(R.drawable.car_blue);
        MyMarker.CAR_RED = BitmapDescriptorFactory.fromResource(R.drawable.car_red);
        MyMarker.MARKER_RED = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        MyMarker.MARKER_BLUE = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
        MyMarker.MARKER_GREEN = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
    }

    public MyMarker(int id, TYPE type, Marker marker)
    {
        mId=id;
        mType=type;
        mMarker=marker;
    }

    public int getId()
    {
        return mId;
    }


    public MyMarker setReporter(String reporter)
    {
        mReporter = reporter;
        return this;
    }

    public String getReporter()
    {
        return mReporter;
    }

    public MyMarker setReason(String reason)
    {
        mReason = reason;
        return this;
    }

    public String getReason()
    {
        return mReason;
    }

    public MyMarker setTimestamp(Timestamp timestamp)
    {
        mTimestamp = timestamp;
        return this;
    }

    public Timestamp getTimestamp()
    {
        return mTimestamp;
    }

    public MyMarker setType(TYPE type)
    {
        mType = type;
        return this;
    }

    public TYPE getType()
    {
        return mType;
    }

    public void setPosition(LatLng position)
    {
        mMarker.setPosition(position);
    }

    public MyMarker setIcon(BitmapDescriptor bitmapDescriptor)
    {
        mMarker.setIcon(bitmapDescriptor);
        return this;
    }

    public Marker getMarker()
    {
        return mMarker;
    }

    public MyMarker setCellphone(String cellphone)
    {
        mCellphone = cellphone;
        return this;
    }

    public MyMarker setTitle(String title)
    {
        mMarker.setTitle(title);
        return this;
    }

    public MyMarker setNeedCompany(boolean needCompany)
    {
        mNeedCompany = needCompany;
        return this;
    }

    public MyMarker setNeedReply(boolean needReply)
    {
        mNeedReply = needReply;
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

    public MyMarker setHasNoData()
    {
        mIsSet = false;
        return this;
    }

    public boolean hasData()
    {
        return mIsSet;
    }
}
