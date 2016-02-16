package com.utdallas.OldClasses.HelperClasses;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.maps.android.SphericalUtil;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.utdallas.Models.Building;
import com.utdallas.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;
import cz.msebera.android.httpclient.Header;

/**
 * Created by sxk159231 on 1/13/2016.
 */

/**
 * This helper class is used in HomeActivity only.
 */

public class HomeActivityHelper {

    Context context;
    private final String TAG = "HomeActivityHelper";
    public HashMap<String, Building> buildingsMap;
    private final String DISTANCE_TO = "buildings", DISTANCE_FROM = "buildings_1";



    public HomeActivityHelper(Context context) {
        this.context = context;
        getBuildingsList();
    }

    //Get the latitude longitude of the buildings
    public void getBuildingsList() {
        AsyncHttpClient client = new AsyncHttpClient();
        final List<Building> buildings = new ArrayList<Building>();
        buildingsMap = new HashMap<>();
        HashMap<String, String> param = new HashMap<>();
        param.put("authtoken", context.getResources().getString(R.string.authtoken_buildingsList));
        RequestParams paramss = new RequestParams(param);
        String url = context.getResources().getString(R.string.url_buildingsList) + context.getResources().getString(R.string.buildingsList);
        client.get(url, paramss, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                Log.d(TAG, "Buildings list request statusCode : " + i);
                String jsonBuildingsList = new String(bytes);
                jsonBuildingsList = jsonBuildingsList.substring(jsonBuildingsList.indexOf("=") + 1);
                Log.d(TAG, "BuildingsList JSON string : " + jsonBuildingsList);
                try {
                    JSONObject object = new JSONObject(jsonBuildingsList);
                    JSONArray buildingsArray = object.getJSONArray("Buildings");
                    String name, id, latitude, longitude;
                    Building building;
                    for (int k = 0; k < buildingsArray.length(); k++) {
                        JSONObject obj = buildingsArray.getJSONObject(k);
                        name = obj.getString("Name");
                        id = obj.getString("ID");
                        latitude = obj.getString("Latitude");
                        longitude = obj.getString("Longitude");
                        building = new Building(name, id, latitude, longitude);
                        building.print();
                        // buildingsMap.put("\"" + name + "\"", building);
                         buildingsMap.put(name, building);
                        buildings.add(building);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Number of buildings = " + buildings.size());
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                Log.d(TAG, "Buildings list error code : " + i);
                Log.e(TAG, "Error string = ");
            }
        });
    }

    void openMaps(ArrayList<String> places) {
            Uri gmmIntentUri = null;
            switch (places.size()) {
                case 2:
                    gmmIntentUri = Uri.parse("http://maps.google.com/maps?saddr=" + buildingsMap.get(places.get(0)).getLatLongString() + "&daddr=" + buildingsMap.get(places.get(1)).getLatLongString());
                    break;
                case 1:
                    gmmIntentUri = Uri.parse("google.navigation:q=" + buildingsMap.get(places.get(0)).getLatLongString());// + "("+places.get(0)+")");
                    //  gmmIntentUri = Uri.parse("google.navigation:q="+places.get(0)+",Richardson,TX" + "("+places.get(0)+")");
                    break;
                case 0:
                    gmmIntentUri = Uri.parse("google.navigation:q=" + buildingsMap.get(places.get(0)).getLatLongString());// + "("+places.get(0)+")");
            }
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            }
            Log.e(TAG, "Map activity opened");
    }

    public void takeAction(AIResponse response, TextToSpeech tts) {
//        if (response.getResult().getAction().contains("map")) {
//            showResponseDialog(response, tts);
//        }
        String action = response.getResult().getAction();
        if(action.equalsIgnoreCase("maps.distance")){
            HashMap<String, JsonElement> para = response.getResult().getParameters();
            Log.d(TAG, "Parameters in maps.distance : " + para.toString());
            if(para.containsKey("buildings") && para.containsKey("buildings_1")) {
                double x= SphericalUtil.computeDistanceBetween(buildingsMap.get(para.get("buildings").getAsString()).getLatLong(), buildingsMap.get(para.get("buildings_1").getAsString()).getLatLong());
                Log.d(TAG, "Distance is  : " + formatNumber(x));
                Toast.makeText(context,"Distance is : " + formatNumber(x), Toast.LENGTH_LONG);
            }
            else{
                Log.e(TAG, "Distance with no parameters ");
            }
        }
    }

    private String formatNumber(double distance) {
        String unit = "m";
        if (distance < 1) {
            distance *= 1000;
            unit = "mm";
        } else if (distance > 1000) {
            distance /= 1000;
            unit = "km";
        }
        return String.format("%4.3f%s", distance, unit);
    }

    public void showResponseDialog(AIResponse response, TextToSpeech tts) {
        ResponseDialog responseDialog = new ResponseDialog(context, response);
        String action = response.getResult().getAction();
            if (action.equalsIgnoreCase("maps.distance") || action.equalsIgnoreCase("maps.locate")) {
            responseDialog.openResponseDialog(tts);
            if (responseDialog.dialog != null && !responseDialog.dialog.isShowing()) {
                responseDialog.dialog.show();
            }
        }
        else if(action.equalsIgnoreCase("maps.places") || action.contains("maps.navigation")){
            openMapsActivity(response);
        }
    }

    public void openMapsActivity(AIResponse response){
        Intent mapsIntent = new Intent(context, MapsActivity.class);
        mapsIntent.putExtra("action", response.getResult().getAction());
        mapsIntent.putExtra("parameters", response.getResult().getStringParameter("to", null));
    }

    public class ResponseDialog {
        Dialog dialog;
        TextView tvYes, tvNo, tvResponse;
        Context context;
        AIResponse response;

        public ResponseDialog(Context context, AIResponse response) {
            dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_response);
            dialog.setCancelable(false);
            tvResponse = (TextView) dialog.findViewById(R.id.tvResponse_homeDialog);
            tvYes = (TextView) dialog.findViewById(R.id.tvYes_homeDialog);
            tvNo = (TextView) dialog.findViewById(R.id.tvNo_homeDialog);
            this.context = context;
            this.response = response;
        }

        public void openResponseDialog(TextToSpeech tts) {
            String speech = response.getResult().getFulfillment().getSpeech();
            tvResponse.setText(speech);
            tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
            tvYes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getId() == R.id.tvYes_homeDialog) {
                        final HashMap<String, JsonElement> params = response.getResult().getParameters();
                        if (params != null && !params.isEmpty()) {
                            ArrayList<String> places = new ArrayList<String>();
                            for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                                places.add(entry.getValue().toString());
                            }
                            openMaps(places);
                        }
                    }
                }
            });
            tvNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getId() == R.id.tvNo_homeDialog) {
                        if (dialog != null && dialog.isShowing())
                            dialog.dismiss();
                    }
                }
            });
        }
    }

    //Logging the response for user voice input
    public void logResponse(AIResponse response) {
        final Status status = response.getStatus();
        Log.i(TAG, "Status code: " + status.getCode());
        Log.i(TAG, "Status type: " + status.getErrorType());
        final Result res = response.getResult();
        Log.i(TAG, "Resolved query: " + res.getResolvedQuery());
        Log.i(TAG, "Action: " + res.getAction());
        Log.i(TAG, "Speech: " + res.getFulfillment().getSpeech());
        final Metadata metadata = res.getMetadata();
        if (metadata != null) {
            Log.i(TAG, "Intent id: " + metadata.getIntentId());
            Log.i(TAG, "Intent name: " + metadata.getIntentName());
        }
        final HashMap<String, JsonElement> params = res.getParameters();
        if (params != null && !params.isEmpty()) {
            Log.i(TAG, "Parameters: ");
            for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
            }
        }
    }

  /*  //Setting the latitude and longitude for different buildings into a Hashmap that we can use
    void initializeHashMap() {
        latlongMap = new HashMap<>();
        latlongMap.put("\"Student Union\"", "32.986889,-96.749198");
        latlongMap.put("\"Visitor Center and University Bookstore\"", "32.984532,-96.749499");
        latlongMap.put("\"Research and Operations Center\"", "32.986294,-96.757091");
        latlongMap.put("\"Naveen Jindal School Of Management\"", "32.984768,-96.757091");
        latlongMap.put("\"Administration\"", "32.989608,-96.748666");
        latlongMap.put("\"Eugene McDermott Library\"", "32.986913,-96.747769");

    }*/
    // String buildingsUrl = "https://creator.zoho.com/api/json/utdallasnext/view/Buildings_Report?authtoken=891e0da5d48bc1217fcd00a942569a60";

}
