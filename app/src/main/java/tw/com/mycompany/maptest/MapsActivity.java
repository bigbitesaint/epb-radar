package tw.com.mycompany.maptest;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;


public class MapsActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG="MyApp";
    public static final String USER_INDEX="user_index";
    protected static final int MY_PERMISSION_FINE_LOCATION = 13;
    private MyMap mapFragment;
    private int mUserId=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button loginButton = (Button)findViewById(R.id.login_button);
        loginButton.setOnClickListener(this);


        ArrayList<String> permissionNeeded = new ArrayList<String>();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (permissionNeeded.size() > 0)
            ActivityCompat.requestPermissions(this, (String[])permissionNeeded.toArray(), MY_PERMISSION_FINE_LOCATION);

        if (savedInstanceState != null)
        {
            mUserId = savedInstanceState.getInt(USER_INDEX,-1);
            toMapView();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstance)
    {
        super.onSaveInstanceState(savedInstance);
        Log.i(TAG, "onSaveInstanceState");
        savedInstance.putInt(USER_INDEX,mUserId);
    }

    @Override
    public void onClick(View view)
    {
        String account = ((EditText) findViewById(R.id.account_text)).getText().toString();
        String passwd = ((EditText) findViewById(R.id.passwd_text)).getText().toString();
        int userId;
        if ( (userId=validate(account, passwd)) == 0)
        {
            Toast.makeText(getBaseContext(), getString(R.string.invalid_login),Toast.LENGTH_SHORT).show();
        }else
        {
            mUserId = userId;
            toMapView();
        }

    }

    public void toMapView()
    {
        if (mUserId != -1) {
            setContentView(R.layout.activity_maps);
            mapFragment = (MyMap) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(mapFragment);
        }
    }

    public int validate(String account, String passwd)
    {
        if (account.compareTo("user1") == 0)
        {
            return 1;
        }else if (account.compareTo("user2") == 0)
        {
            return 4;
        }
        return 0;
    }

    public int getUserId()
    {
        return mUserId;
    }

    public MyMap getMap()
    {
        return mapFragment;
    }
}
