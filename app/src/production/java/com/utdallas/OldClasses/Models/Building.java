package com.utdallas.OldClasses.Models;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by sxk159231 on 1/12/2016.
 */
public class Building {
    String Name = "";
    String ID="";
    String Latitude="";
    String Longitude="";
    LatLng latlng;
    String LatLong;

    public Building(String name, String id, String latitude, String longitude){
        this.Name = name;
        this.ID = id;
        this.Latitude = latitude;
        this.Longitude = longitude;
        this.LatLong = latitude+","+longitude;
    }
    public void print(){
        Log.d("Building", "Name : " + Name + "; ID : " + ID + "; LatLong : " + LatLong);//"; Latitude : " + Latitude + " ; Longitude : " + Longitude + "\n");
    }

    public String getLatLongString() {
        return LatLong;
    }

    public LatLng getLatLong(){
        Log.d("Building", "Latitude : " + Latitude + "; Longitude : " + Longitude);
        return new LatLng(Double.valueOf(Latitude), Double.valueOf(Longitude));
    }

    public String getName(){
        return this.Name;
    }


}
