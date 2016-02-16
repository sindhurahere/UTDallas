/*
package com.utdallas.OldClasses.Activities;

*/
/**
 * Created by sxk159231 on 1/25/2016.
 *//*

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.utdallas.R;

public class SplashActivity extends Activity {

    */
/** Duration of wait **//*

    private final int SPLASH_DISPLAY_LENGTH = 3000;

    */
/** Called when the activity is first created. *//*

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_splash);

        */
/* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*//*

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                */
/* Create an Intent that will start the Menu-Activity. *//*

                Intent mainIntent = new Intent(SplashActivity.this,ViewPagerActivity.class);
                startActivity(mainIntent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}*/
