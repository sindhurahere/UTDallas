package com.utdallas.Models;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by sxk159231 on 1/27/2016.
 */
public class Loc {
    public String name;
    public LatLng latlng;

    public Loc(String name, LatLng latLng){
        this.name=name;
        this.latlng = latLng;
    }

    public String getName() {
        return name;
    }

    public LatLng getLatlng() {
        return latlng;
    }
    public String getLatlngString(){
        return latlng.latitude + "," + latlng.longitude;
    }
}
