package com.utdallas.OldClasses.Activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;

import com.utdallas.OldClasses.HelperClasses.HomeActivityHelper;
import com.utdallas.R;
import com.utdallas.OldClasses.Utilities.BlurImage;

import java.util.HashMap;
import java.util.Locale;

import ai.api.AIConfiguration;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.ui.AIButton;

/**
 * Created by sxk159231 on 1/12/2016.
 */
public class HomeActivity extends Activity implements AIButton.AIButtonListener {

    AutoCompleteTextView tvQuestion;
    AIButton micButton;
    private final String TAG = "HomeActivity.class";
    HashMap<String, String> latlongMap;
    HomeActivityHelper helperClass;
    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home2);
        tvQuestion = (AutoCompleteTextView) findViewById(R.id.tvQuestion_home);
        micButton = (AIButton) findViewById(R.id.micButton);
        helperClass = new HomeActivityHelper(HomeActivity.this);
        helperClass.getBuildingsList();
        configureMic(micButton);
        tts = new TextToSpeech(HomeActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
        Bitmap bgimage= BitmapFactory.decodeResource(getResources(), R.drawable.rsz_1utd_bg);
        Bitmap blurredBitmap = BlurImage.blur(this, bgimage);
        LinearLayout ll_layout = (LinearLayout)findViewById(R.id.ll_home);
        ll_layout.setBackgroundDrawable( new BitmapDrawable( getResources(), blurredBitmap ) );
    }

    //Configuring the mic
    void configureMic(AIButton micButton) {
        final AIConfiguration config = new AIConfiguration(getApplicationContext().getResources().getString(R.string.ACCESS_KEY),
                getApplicationContext().getResources().getString(R.string.SUBSCRIPTION_KEY), AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));
        micButton.initialize(config);
        micButton.setResultsListener(this);
    }

    //Response contains user input; If it contains places in UTD, show them in the map
    @Override
    public void onResult(final AIResponse response) {
        Log.d(TAG, "Result : " + response.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helperClass.logResponse(response);
                helperClass.takeAction(response, tts);
                tvQuestion.setText(response.getResult().getResolvedQuery());
               // helperClass.showResponseDialog(response, tts);
            }
        });
    }

    //Pause mic when activity pauses
    @Override
    protected void onPause() {
        super.onPause();
        micButton.pause();
        /*if (tts != null) {
            tts.stop();
            tts.shutdown();
        }*/
    }

    //Resume mic when activity resumes
    @Override
    protected void onResume() {
        super.onResume();
        micButton.resume();
    }

    //In case of error, log it and let the user know
    @Override
    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "OnError");
                if (error != null) Log.e(TAG, "Error : " + error.toString());
            }
        });
    }

    //In case user cancels
    @Override
    public void onCancelled() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onCancelled");
            }
        });
    }

}
