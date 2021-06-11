package com.example.gallery_ai;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;

import static com.example.gallery_ai.UserLogin.userID;

import static android.net.sip.SipErrorCode.TIME_OUT;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        final int TIME_OUT = 2000;
        SharedPreferences readPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String user2 = readPref.getString("userID","");
        System.out.println(user2);


        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN ;
        decorView.setSystemUiVisibility(uiOptions);

        if(!user2.matches("")){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    userID = user2;
                    Intent i = new Intent(SplashActivity.this, GalleryGrid.class);
                    startActivity(i);
                }
            }, TIME_OUT);
        }
    else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(SplashActivity.this, UserLogin.class);
                    startActivity(i);
                    finish();
                }
            }, TIME_OUT);
        }

    }
}