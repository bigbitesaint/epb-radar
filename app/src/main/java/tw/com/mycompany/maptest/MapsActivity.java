package tw.com.mycompany.maptest;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.content.SharedPreferences;

import java.util.ArrayList;


public class MapsActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG="MyApp";
    private static final String USER_INDEX="mapsactivity_user_index";
    private static final String USER_NAME="mapsactivity_user_name";
    private static final String PREFS_NAME="mapsactivity_pref_name";
    private static final String PREFS_NAME_USERNAME="mapsactivity_pref_name_username";
    private static final String PREFS_NAME_USERPASSWD="mapsactivity_pref_name_userpasswd";
    private static final String PREFS_NAME_REMEMBERINFO="mapsactivity_pref_name_rememberinfo";
    protected static final int MY_PERMISSION_FINE_LOCATION = 13;
    private MyMap mapFragment;
    private int mUserId=-1;
    private String mUserName = null;

    private class validationProcess implements Runnable{
        private String mUserName;
        private String mPasswd;

        public validationProcess(String userName, String passwd){
            mUserName = userName;
            mPasswd = passwd;
        }

        @Override
        public void run()
        {
            Message message = mValidationProcessHandler.obtainMessage();
            String result = DBHelper.doValidate(mUserName, mPasswd);
            SystemClock.sleep(1000);
            if ( result != null && result.length() > 0 )
            {
                message.obj = result;
                message.sendToTarget();
            }else
            {
                // validation fail
                message.obj = "";
                message.sendToTarget();
            }
        }
    }

    private Handler mValidationProcessHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        // updates marker's details.  only run once per marker
        public void handleMessage(Message message) {
            String response = (String)message.obj;
            Log.i(TAG, response);
            finishValidation();
            if (response.length() != 0)
            {
                try {
                    String[] arr = response.split(",");
                    mUserId = Integer.parseInt(arr[0]);
                    mUserName = arr[1];
                } catch (NumberFormatException nfe)
                {
                    Log.e(TAG, "Number format error.");
                }

                // to map view
                toMapView();
            }else
                Toast.makeText(getBaseContext(), getString(R.string.invalid_login),Toast.LENGTH_SHORT).show();

        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button loginButton = (Button)findViewById(R.id.login_button);
        loginButton.setOnClickListener(this);



        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_FINE_LOCATION);
        }




        if (savedInstanceState != null)
        {
            mUserId = savedInstanceState.getInt(USER_INDEX,-1);
            mUserName = savedInstanceState.getString(USER_NAME,"");
            toMapView();
        }

        // ok if we are still at the login menu
        loadLoginInfo();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstance)
    {
        super.onSaveInstanceState(savedInstance);
        Log.i(TAG, "onSaveInstanceState");
        savedInstance.putInt(USER_INDEX,mUserId);
        savedInstance.putString(USER_NAME, mUserName);
    }

    @Override
    public void onClick(View view)
    {
        String account = ((EditText) findViewById(R.id.account_text)).getText().toString();
        String passwd = ((EditText) findViewById(R.id.passwd_text)).getText().toString();
        validate(account, passwd);
    }

    private void clearLoginInfo()
    {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }

    private void saveLoginInfo()
    {
        String username = ((EditText) findViewById(R.id.account_text)).getText().toString();
        String passwd = ((EditText) findViewById(R.id.passwd_text)).getText().toString();
        boolean rememberInfo = ((CheckBox) findViewById(R.id.remember_me)).isChecked();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_NAME_USERNAME, username);
        editor.putString(PREFS_NAME_USERPASSWD, passwd);
        editor.putBoolean(PREFS_NAME_REMEMBERINFO, rememberInfo);
        editor.commit();
    }

    private void loadLoginInfo()
    {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String username = settings.getString(PREFS_NAME_USERNAME,"");
        String passwd = settings.getString(PREFS_NAME_USERPASSWD,"");
        boolean rememberInfo = settings.getBoolean(PREFS_NAME_REMEMBERINFO, false);

        ((EditText) findViewById(R.id.account_text)).setText(username);
        ((EditText) findViewById(R.id.passwd_text)).setText(passwd);
        ((CheckBox) findViewById(R.id.remember_me)).setChecked(rememberInfo);
    }



    public void toMapView()
    {
        // it's mandatory to check whether mUserId is set
        if (mUserId != -1) {
            setContentView(R.layout.activity_maps);
            mapFragment = (MyMap) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(mapFragment);

        }
    }

    public void validate(String userName, String passwd)
    {
        startValidation();

        // store/clear the login info depending on checkbox
        if (((CheckBox) findViewById(R.id.remember_me)).isChecked())
            saveLoginInfo();
        else
            clearLoginInfo();

        Thread thread = new Thread(new validationProcess(userName, passwd));
        thread.start();
    }

    public void startValidation()
    {
        View loadingIcon = (View) findViewById(R.id.login_loading_panel);
        loadingIcon.setVisibility(View.VISIBLE);
        View form = (View) findViewById(R.id.login_form);
        form.setAlpha(0.1f);
        form.setClickable(false);
    }

    public void finishValidation()
    {
        View loadingIcon = (View) findViewById(R.id.login_loading_panel);
        loadingIcon.setVisibility(View.GONE);
        View form = (View) findViewById(R.id.login_form);
        form.setAlpha(1.0f);
        form.setClickable(false);
    }

    public void reset()
    {
        finish();
        startActivity(getIntent());
    }

    public int getUserId()
    {
        return mUserId;
    }

    public String getUserName() {return mUserName;}

    public MyMap getMap()
    {
        return mapFragment;
    }
}
