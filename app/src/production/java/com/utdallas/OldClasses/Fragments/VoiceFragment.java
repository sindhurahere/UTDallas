package com.utdallas.OldClasses.Fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonElement;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.utdallas.OldClasses.HelperClasses.HomeActivityHelper;
import com.utdallas.Models.Building;
import com.utdallas.R;
import com.utdallas.Utilities.LocationGetter;
import com.utdallas.OldClasses.Utilities.MapUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import ai.api.AIConfiguration;
import ai.api.model.AIError;
import ai.api.model.AIOutputContext;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import ai.api.ui.AIButton;
import cz.msebera.android.httpclient.Header;

/**
 * Created by sxk159231 on 1/25/2016.
 */
public class VoiceFragment extends ListFragment implements AIButton.AIButtonListener {

    TextView tvQuestion, tvAnswer;
    AIButton micButton;
    SupportMapFragment mapFragment;

    MapView mapView;
    Context context;
    HomeActivityHelper helperClass;
    private final String TAG = "VoiceFragment";
    TextToSpeech tts;
    private final String MAPS_DISTANCE = "maps.distance", MAPS_TRANSPORT = "maps.transport", MAPS_WAYFINDING = "maps.wayfinding", MAPS_TIME = "maps.time", MAPS_LOCATE = "maps.locate";
    private final int LOCATIONS_REQUEST_CODE = 444;
    boolean permissionsGranted = false;
    GoogleMap mMap;
    FragmentTransaction ft;

    public VoiceFragment() {
    }

    public VoiceFragment(Context context) {
        this.context = context;
        helperClass = new HomeActivityHelper(context);
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_voice, null);
        tvQuestion = (TextView) v.findViewById(R.id.tvQuestion_fragmentVoice);
        tvAnswer = (TextView) v.findViewById(R.id.tvAnswer_fragmentVoice);
        micButton = (AIButton) v.findViewById(R.id.micButton);
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.f_map);
        tvQuestion.setVisibility(View.GONE);
        tvAnswer.setVisibility(View.GONE);
        configureMic(micButton);
        ft = getFragmentManager().beginTransaction();
        ft.hide(mapFragment).commit();
        return v;
    }

    //Configuring the mic
    void configureMic(AIButton micButton) {
        final AIConfiguration config = new AIConfiguration(getResources().getString(R.string.ACCESS_KEY),
                getResources().getString(R.string.SUBSCRIPTION_KEY), AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));
        micButton.initialize(config);
        micButton.setResultsListener(this);
    }


    @Override
    public void onPause() {
        super.onPause();
        micButton.pause();
    }

    //Resume mic when activity resumes
    @Override
    public void onResume() {
        super.onResume();
        micButton.resume();
    }

    //In case of error, log it and let the user know
    @Override
    public void onError(final AIError error) {
        getActivity().runOnUiThread(new Runnable() {
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onCancelled");
            }
        });
    }

    //Response contains user input; If it contains places in UTD, show them in the map
    @Override
    public void onResult(final AIResponse response) {
        Log.d(TAG, "Result : " + response.toString());
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helperClass.logResponse(response);
                takeAction(response, tts);
                tvQuestion.setVisibility(View.VISIBLE);
                tvAnswer.setVisibility(View.VISIBLE);
                tvQuestion.setText(response.getResult().getResolvedQuery());
                tvAnswer.setText(response.getResult().getFulfillment().getSpeech());
                // helperClass.showResponseDialog(response, tts);
            }
        });
    }


    private void takeAction(AIResponse response, TextToSpeech tts) {
        Result result = response.getResult();
        String action = result.getAction();
        HashMap<String, JsonElement> parameters = result.getParameters();
        if (action.equalsIgnoreCase(MAPS_DISTANCE)) {
            calculateDistance(result, result.getFulfillment().getSpeech());
        } else if (action.equalsIgnoreCase(MAPS_TRANSPORT)) {
            calculateDistance(result, result.getFulfillment().getSpeech());
        }
    }


    private void calculateDistance(Result result, String speech) {
        if (result.getParameters().containsKey("buildings")) {
            tvAnswer.setVisibility(View.VISIBLE);
            tvAnswer.setText(speech);
            tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
            micButton.startListening();
        } else if (result.getParameters().containsKey("transportation")) {
            List<AIOutputContext> contexts = result.getContexts();
            for (AIOutputContext con : contexts) {
                Map<String, JsonElement> params = con.getParameters();
                if (params.containsKey("transportation")) {
                    LatLng origin=null, destination=null;
                    String mode="",url="", latlong_origin="", latlong_destination="", originName="", destinationName="";

                    Log.d(TAG, "CONTEXT NAME : " + con.getParameters().get("transportation").getAsString());

                    String transportParam = params.get("transportation").getAsString();

                    if(transportParam.equals("Walk")) mode="walking";
                    else mode="driving";

                    if (params.containsKey("buildings") && params.containsKey("buildings_1") && params.get("buildings_1").getAsString().equals("")) {
                        LocationGetter locationGetter = new LocationGetter(context);
                        latlong_origin = locationGetter.getLatLong();
                        originName = "Your location";
                        origin = new LatLng(locationGetter.getLatitude(), locationGetter.getLongitude());
                        Building building = helperClass.buildingsMap.get(params.get("buildings").getAsString());
                        latlong_destination = building.getLatLongString();
                        destinationName = building.getName();
                        destination = building.getLatLong();
                    } else if (params.containsKey("buildings") && params.containsKey("buildings_1") && !params.get("buildings_1").getAsString().equalsIgnoreCase("")) {
                        Building building1 = helperClass.buildingsMap.get(params.get("buildings").getAsString());
                        Building building2 = helperClass.buildingsMap.get(params.get("buildings_1").getAsString());
                        latlong_origin = building1.getLatLongString();
                        latlong_destination = building2.getLatLongString();
                        originName = building1.getName();
                        destinationName = building2.getName();
                        origin = building1.getLatLong();
                        destination = building2.getLatLong();
                    }
                    url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + latlong_origin
                            + "&destinations=" + latlong_destination +
                            "&units=imperial&mode="+mode;
                    getDistance(url, origin, destination, originName, destinationName, mode);
                    Log.d(TAG, "Url = " + url);
                }
            }

        }
    }

    private void getDistance(String url, final LatLng origin, final LatLng destination, final String originName, final String destinationName, final String mode) {
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
                    getDocument(origin, destination, mode);
                    getFragmentManager().beginTransaction().show(mapFragment).commit();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mapFragment.getMapAsync(new OnMapReadyCallback() {
                                @Override
                                public void onMapReady(GoogleMap googleMap) {
                                    mMap = googleMap;
                                    GoogleMapOptions options = new GoogleMapOptions().liteMode(true);
                                    googleMap.addMarker(new MarkerOptions().position(origin).title(originName));
                                    googleMap.addMarker(new MarkerOptions().position(destination).title(destinationName));
                                    LatLngBounds bounds = new LatLngBounds.Builder().include(origin).include(destination).build();
                                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 300);
                                    googleMap.animateCamera(cu);
                                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        permissionsGranted = false;
                                        String[] permissions = new String[2];
                                        permissions[0] = Manifest.permission.ACCESS_FINE_LOCATION;
                                        permissions[1] = Manifest.permission.ACCESS_COARSE_LOCATION;
                                        ActivityCompat.requestPermissions(getActivity(), permissions, LOCATIONS_REQUEST_CODE);
                                        return;
                                    }
                                }
                            });
                        }
                    }, 1000);
                    String speech = "The distance between the two places is " + distance;
                    tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
                    tvAnswer.setVisibility(View.VISIBLE);
                    tvAnswer.setText(speech);
                    Toast.makeText(context, "Distance is : " + distance, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == LOCATIONS_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) return;
            }
            permissionsGranted = true;
            if (mMap != null) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    public void getDocument(LatLng start, LatLng end, String mode) {
        String url = "http://maps.googleapis.com/maps/api/directions/json?"
                + "origin=" + start.latitude + "," + start.longitude
                + "&destination=" + end.latitude + "," + end.longitude
                + "&units=metric&mode="+mode;
        Log.d("url", url);
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(url, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int i, Header[] headers, byte[] bytes) {
                    String response = new String(bytes);
                    Log.d(TAG, "Response in getDocument : " + response);
                    DocumentBuilder builder = null;
                    try {
                        builder = DocumentBuilderFactory.newInstance()
                                .newDocumentBuilder();
                        Document doc = builder.parse(response);
                        ArrayList<LatLng> directionPoint = new MapUtils().getDirection(doc);
                        PolylineOptions rectLine = new PolylineOptions().width(3).color(
                                Color.RED);
                        for (int j = 0; j < directionPoint.size(); j++) {
                            rectLine.add(directionPoint.get(j));
                        }
                        if(mMap!=null) {
                            Polyline polyline = mMap.addPolyline(rectLine);
                        }
                        Log.d(TAG, doc.toString());
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    } catch (SAXException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                    Log.d(TAG, "Failed");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void parseXML(String string) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(string));
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_DOCUMENT) {
                Log.d(TAG, "Start document");
            } else if(eventType == XmlPullParser.END_DOCUMENT) {
               Log.d(TAG, "End document");
            } else if(eventType == XmlPullParser.START_TAG) {
                Log.d(TAG,"Start tag "+xpp.getName());
            } else if(eventType == XmlPullParser.END_TAG) {
                Log.d(TAG,"End tag "+xpp.getName());
            } else if(eventType == XmlPullParser.TEXT) {
                Log.d(TAG, "Text "+xpp.getText());
            }
            eventType = xpp.next();
        }
    }

}
