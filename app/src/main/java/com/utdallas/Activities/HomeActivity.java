package com.utdallas.Activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonElement;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.utdallas.Models.Building;
import com.utdallas.Models.Loc;
import com.utdallas.R;
import com.utdallas.Utilities.HomeActivityHelper;
import com.utdallas.Utilities.LocationGetter;
import com.utdallas.Utilities.MyMapFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ai.api.AIConfiguration;
import ai.api.model.AIError;
import ai.api.model.AIOutputContext;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import ai.api.ui.AIButton;
import cz.msebera.android.httpclient.Header;

/**
 * Created by sxk159231 on 1/27/2016.
 */


public class HomeActivity extends FragmentActivity implements AIButton.AIButtonListener, LocationGetter.CurrentLocationGetter {

    private final String TAG = "HomeActivity";
    private final String MAPS_DISTANCE = "maps.distance", MAPS_TRANSPORT = "maps.transport", MAPS_TIME = "maps.time", MAPS_LOCATE = "maps.locate", MAPS_WAYFINDING = "maps.wayfinding";
    private int FRAME_ID = 756565;

    HomeActivityHelper helperClass;
    TextToSpeech tts;

    LinearLayout ll_home, ll_main;
    ScrollView sv_home;
    AIButton micButton;
    LocationGetter locGetter;
    LatLng currentLocation;
    Loc origin = null, destination = null;
    String mode = null;


    private final int AUDIO_REQUEST_CODE = 111;
    private final int COARSE_LOCATION_CODE = 222;
    private final int FINE_LOCATION_CODE = 333;

    private boolean requestedLocation = false, requestedMicrophone = true;
    private enum SpeechType {
        QUESTION, ANSWER
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ll_home = (LinearLayout) findViewById(R.id.llHome);
        ll_main = (LinearLayout) findViewById(R.id.llMainHome);
        sv_home = (ScrollView) findViewById(R.id.SVHome);
        micButton = (AIButton) findViewById(R.id.micHome);
        requestMicrophonePermission();
        configureMic(micButton);

        tts = new TextToSpeech(HomeActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
        helperClass = new HomeActivityHelper(HomeActivity.this);
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
        micButton.setResultsListener(HomeActivity.this);
    }

    //Response contains user input; If it contains places in UTD, show them in the map
    @Override
    public void onResult(final AIResponse response) {
        Log.d(TAG, "Result : " + response.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helperClass.logResponse(response);
                addTextView(response.getResult().getResolvedQuery(), SpeechType.QUESTION);
                String speechResponse = response.getResult().getFulfillment().getSpeech();
                if (!speechResponse.equals(""))
                    addTextView(speechResponse, SpeechType.ANSWER);
                takeAction(response);
            }
        });
    }

    private void addTextView(String speech, SpeechType type) {
        TextView tv = new TextView(HomeActivity.this);
        if (type == SpeechType.QUESTION) {
            speech = speech.toUpperCase();
            tv.setTextAppearance(this, R.style.tvQuestion);
        } else if (type == SpeechType.ANSWER) {
            tv.setTextAppearance(this, R.style.tvAnswer);
            tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
        }
        tv.setMinHeight(50);
        tv.setText(speech);
        ll_home.addView(tv);
        sv_home.post(new Runnable() {
            @Override
            public void run() {
                sv_home.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void takeAction(AIResponse response) {
        String action = response.getResult().getAction();
        if (action.equalsIgnoreCase(MAPS_DISTANCE) || action.equalsIgnoreCase(MAPS_TRANSPORT) || action.equalsIgnoreCase(MAPS_TIME) || action.equalsIgnoreCase(MAPS_LOCATE) || action.equalsIgnoreCase(MAPS_WAYFINDING)) {
            makeURL(response.getResult());
        }
    }

    private static String action = "";

    private void makeURL(Result result) {
        Log.d(TAG, "makeURL");
        if (result.getParameters().containsKey("transportation")) {
            Log.d(TAG, "Transportation detail received");
            List<AIOutputContext> contexts = result.getContexts();
            for (AIOutputContext con : contexts) {
                final Map<String, JsonElement> params = con.getParameters();
                if (params.containsKey("transportation")) {
                    Log.d(TAG, "CONTEXT NAME : " + con.getParameters().get("transportation").getAsString());
                    String transportParam = params.get("transportation").getAsString();
                    if (transportParam.equals("Walk")) mode = "walking";
                    else mode = "driving";
                    if ((params.containsKey("buildings") && params.containsKey("buildings_1") && params.get("buildings_1").getAsString().equals("")) || (params.containsKey("buildings") && !params.containsKey("buildings_1"))) {
                        locGetter = new LocationGetter(this, this);
                        requestedLocation = true;
                        Log.d(TAG, "Directions from current location");
                        Building building = helperClass.buildingsMap.get(params.get("buildings").getAsString());
                        destination = new Loc(building.getName(), building.getLatLong());
                    } else if (params.containsKey("buildings") && params.containsKey("buildings_1") && !params.get("buildings_1").getAsString().equalsIgnoreCase("")) {
                        Building building1 = helperClass.buildingsMap.get(params.get("buildings").getAsString());
                        Building building2 = helperClass.buildingsMap.get(params.get("buildings_1").getAsString());
                        origin = new Loc(building1.getName(), building1.getLatLong());
                        destination = new Loc(building2.getName(), building2.getLatLong());
                        buildUrl(origin, destination, mode);
                    }
                }
            }
        } else if (result.getParameters().containsKey("buildings")) {
            action = result.getAction();
            micButton.startListening();
        }
    }

    private void getOriginFromCurrentLocation(LatLng currentLoc) {
        buildUrl(new Loc("Your location", currentLoc), destination, mode);
        if(locGetter!=null) locGetter.stopLocationUpdates();
    }

    private void buildUrl(Loc origin, Loc destination, String mode) {
        if (origin != null && destination != null) {
            String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin.getLatlngString()
                    + "&destination=" + destination.getLatlngString() +
                    "&units=imperial&mode=" + mode;
            requestServer_maps(url, origin, destination);
            Log.d(TAG, "Url = " + url);
        }
    }

    private void requestServer_maps(String url, final Loc origin, final Loc destination) {
        AsyncHttpClient client = new AsyncHttpClient();
        final List<LatLng> waypoints = new ArrayList<>();
        waypoints.add(origin.getLatlng());
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                String response = new String(bytes);
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray routesArray = obj.getJSONArray("routes");
                    JSONObject route = routesArray.getJSONObject(0);
                    JSONArray legsArray = route.getJSONArray("legs");
                    JSONObject leg = legsArray.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    String dist = distance.getString("text");
                    JSONObject duration = leg.getJSONObject("duration");
                    String dur = duration.getString("text");
                    JSONArray stepsArray = leg.getJSONArray("steps");
                    for (int k = 0; k < stepsArray.length(); k++) {
                        JSONObject step = stepsArray.getJSONObject(k);
                        JSONObject endLocation = step.getJSONObject("end_location");
                        LatLng point = new LatLng(Double.parseDouble(endLocation.getString("lat")), Double.parseDouble(endLocation.getString("lng")));
                        waypoints.add(point);
                    }
                    //Create a FrameLayout to hold the Map Fragment
                    final FrameLayout frame = new FrameLayout(HomeActivity.this);
                    FRAME_ID++;
                    frame.setId(FRAME_ID);
                    ll_home.addView(frame, ViewGroup.LayoutParams.MATCH_PARENT, 700);
                    sv_home.post(new Runnable() {
                        @Override
                        public void run() {
                            sv_home.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                    GoogleMapOptions options = new GoogleMapOptions();
                    //  options.scrollGesturesEnabled(true).rotateGesturesEnabled(true).zoomGesturesEnabled(true).zoomControlsEnabled(true);
                    MyMapFragment mapFragment = MyMapFragment.newInstance(waypoints, options, HomeActivity.this, origin.getName(), destination.getName());
                    getSupportFragmentManager().beginTransaction().add(frame.getId(), mapFragment).commit();
                    String speech = "";
                    if (action.equalsIgnoreCase(MAPS_DISTANCE))
                        speech = "The distance between the two places is " + dist;
                    else if (action.equalsIgnoreCase(MAPS_WAYFINDING))
                        speech = "This is how you can get to your destination.";
                    else if (action.equalsIgnoreCase(MAPS_TIME))
                        speech = "It takes " + dur + " to get to the destination";
                    addTextView(speech, SpeechType.ANSWER);
                } catch (Exception e) {
                    Log.d(TAG, "Exception parsing directions response");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                Log.d(TAG, "Failed to retrieve directions");
            }
        });
    }


    //Pause mic when activity pauses
    @Override
    protected void onPause() {
        super.onPause();
        micButton.pause();
//        if (locGetter != null && locGetter.mGoogleApiClient.isConnected()) locGetter.stopLocationUpdates();
    }

    //Resume mic when activity resumes. Action to be performed after coming back from Settings screen-after GPS is enabled.
    @Override
    protected void onResume() {
        super.onResume();
        micButton.resume();
        if (locGetter != null && locGetter.mGoogleApiClient != null && locGetter.mGoogleApiClient.isConnected() && !locGetter.mRequestingLocationUpdates) {
            locGetter.startLocationUpdates();
        }
    }

    //In case of error, log it and let the user know
    @Override
    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Mic - OnError");
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
                Log.d(TAG, "Mic - onCancelled");
            }
        });
    }

    //Callback after request for permissions is made
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(TAG, "Request permissions call back");
        switch (requestCode) {
            case AUDIO_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Microphone permission granted");
                } else {
                    Toast.makeText(this, "Please allow microphone in Settings", Toast.LENGTH_SHORT).show();
                    if (requestedMicrophone) {
                        requestMicrophonePermission();
                        requestedMicrophone = false;
                    }
                }
                return;
            }
            case COARSE_LOCATION_CODE: {
                Log.d(TAG, "Request callback - location");
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Coarse location Permission granted");
                } else {
                    Toast.makeText(this, "Please enable location in Settings", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case FINE_LOCATION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Coarse location Permission granted");
                    if(locGetter.mGoogleApiClient.isConnected()) locGetter.startLocationUpdates();
                    else locGetter.mGoogleApiClient.connect();
                } else {
                    Toast.makeText(this, "Please enable location in Settings", Toast.LENGTH_SHORT).show();
                }
        }
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onStop() {
        if (locGetter != null && locGetter.mGoogleApiClient != null)
            locGetter.mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        AUDIO_REQUEST_CODE);
            }
        }
    }


    private final int LOCATION_INTENT_CODE = 444;

    @Override
    public void getLatLong(LatLng currentLoc) {
        currentLocation = currentLoc;
        Log.d(TAG, "Current location set");
        if(requestedLocation){
            getOriginFromCurrentLocation(currentLocation);
            requestedLocation = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_INTENT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                locGetter.mGoogleApiClient.connect();
                Log.d(TAG, "GPS result ok");
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "GPS result cancelled");
                if (((LocationManager) getSystemService(Context.LOCATION_SERVICE))
                        .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locGetter.mGoogleApiClient.connect();
                }
            }
        }
    }
}
