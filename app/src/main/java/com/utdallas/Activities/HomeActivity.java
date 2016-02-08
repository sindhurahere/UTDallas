package com.utdallas.Activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
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
import com.utdallas.Models.Loc;
import com.utdallas.OldClasses.Models.Building;
import com.utdallas.OldClasses.Utilities.HomeActivityHelper;
import com.utdallas.OldClasses.Utilities.LocationGetter;
import com.utdallas.R;
import com.utdallas.Utilities.BlurImage;
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


public class HomeActivity extends FragmentActivity implements AIButton.AIButtonListener {

    //Test push..

    private final String TAG = "HomeActivity";
    private final String MAPS_DISTANCE = "maps.distance", MAPS_TRANSPORT = "maps.transport", MAPS_TIME = "maps.time", MAPS_LOCATE = "maps.locate", MAPS_WAYFINDING = "maps.wayfinding";
    private int FRAME_ID = 756565;

    HomeActivityHelper helperClass;
    TextToSpeech tts;

    LinearLayout ll_home, ll_main;
    ScrollView sv_home;
    AIButton micButton;

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
        //blurBackground();
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
            speech.toUpperCase();
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

    private void makeURL(Result result) {
        if (result.getParameters().containsKey("buildings")) {
            micButton.startListening();
        } else if (result.getParameters().containsKey("transportation")) {
            Log.d(TAG, "Transportation detail received");
            List<AIOutputContext> contexts = result.getContexts();
            for (AIOutputContext con : contexts) {
                Map<String, JsonElement> params = con.getParameters();
                if (params.containsKey("transportation")) {
                    Loc origin = null, destination = null;
                    String mode = "", url = "";
                    Log.d(TAG, "CONTEXT NAME : " + con.getParameters().get("transportation").getAsString());
                    String transportParam = params.get("transportation").getAsString();
                    if (transportParam.equals("Walk")) mode = "walking";
                    else mode = "driving";
                    if (params.containsKey("buildings") && params.containsKey("buildings_1") && params.get("buildings_1").getAsString().equals("")) {
                        LocationGetter locationGetter = new LocationGetter(this);
                        origin = new Loc("Your location", new LatLng(locationGetter.getLatitude(), locationGetter.getLongitude()));
                        Building building = helperClass.buildingsMap.get(params.get("buildings").getAsString());
                        destination = new Loc(building.getName(), building.getLatLong());
                    } else if (params.containsKey("buildings") && params.containsKey("buildings_1") && !params.get("buildings_1").getAsString().equalsIgnoreCase("")) {
                        Building building1 = helperClass.buildingsMap.get(params.get("buildings").getAsString());
                        Building building2 = helperClass.buildingsMap.get(params.get("buildings_1").getAsString());
                        origin = new Loc(building1.getName(), building1.getLatLong());
                        destination = new Loc(building2.getName(), building2.getLatLong());
                    }
                    url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin.getLatlngString()
                            + "&destination=" + destination.getLatlngString() +
                            "&units=imperial&mode=" + mode;
                    requestServer_maps(url, origin, destination, result.getAction());
                    Log.d(TAG, "Url = " + url);
                }
            }

        }
    }

    private void requestServer_maps(String url, final Loc origin, final Loc destination, final String action){
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
                    if(action.equalsIgnoreCase(MAPS_DISTANCE)) addTextView("The distance is " + dist, SpeechType.ANSWER);
                    else if(action.equalsIgnoreCase(MAPS_TIME)) addTextView("It could take " + dur + "to get to the destination", SpeechType.ANSWER);
                    JSONArray stepsArray = leg.getJSONArray("steps");
                    for (int k = 0; k < stepsArray.length(); k++) {
                        JSONObject step = stepsArray.getJSONObject(k);
                        JSONObject endLocation = step.getJSONObject("end_location");
                        LatLng point = new LatLng(Double.parseDouble(endLocation.getString("lat")),Double.parseDouble(endLocation.getString("lng")));
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
                    String speech = "The distance between the two places is " + dist;
                    tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
                    Toast.makeText(HomeActivity.this, "Distance is : " + dist, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.d(TAG, "Exception parsing directions response");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

            }
        });
    }

   /* private void requestServer_maps(String url, final Loc origin, final Loc destination) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                String response = new String(bytes);
                try {
                    final JSONObject obj = new JSONObject(response);
                    JSONArray rowArray = obj.getJSONArray("rows");
                    JSONObject rowObject = rowArray.getJSONObject(0);
                    JSONArray elementsArray = rowObject.getJSONArray("elements");
                    JSONObject elementsObj = elementsArray.getJSONObject(0);
                    final String distance = elementsObj.getJSONObject("distance").getString("text");
                    Log.d(TAG, "Distance is : " + distance);
                    addTextView("The distance is " + distance, SpeechType.ANSWER);

                    final ArrayList<Loc> locations = new ArrayList<Loc>();
                    locations.add(origin);
                    locations.add(destination);

                    //Create a FrameLayout to hold the Map Fragment
                    final FrameLayout frame = new FrameLayout(HomeActivity.this);
                    frame.setId(675765);
                    ll_home.addView(frame, ViewGroup.LayoutParams.MATCH_PARENT, 700);
                    GoogleMapOptions options = new GoogleMapOptions();
                  //  options.scrollGesturesEnabled(true).rotateGesturesEnabled(true).zoomGesturesEnabled(true).zoomControlsEnabled(true);
                    MyMapFragment mapFragment = MyMapFragment.newInstance(locations, options, HomeActivity.this);
                    getSupportFragmentManager().beginTransaction().add(frame.getId(), mapFragment).commit();
                    String speech = "The distance between the two places is " + distance;
                    tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
                    Toast.makeText(HomeActivity.this, "Distance is : " + distance, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

            }
        });
    }*/


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

    private void blurBackground() {
        Bitmap bgimage = BitmapFactory.decodeResource(getResources(), R.drawable.utdchessboard);
        Bitmap blurredBitmap = BlurImage.blur(this, bgimage);
        ll_main.setBackgroundDrawable(new BitmapDrawable(getResources(), blurredBitmap));
    }
}
