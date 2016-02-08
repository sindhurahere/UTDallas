package com.utdallas.Utilities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.utdallas.Models.Loc;
import com.utdallas.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sxk159231 on 1/27/2016.
 */
public class MyMapFragment extends SupportMapFragment {

    List<LatLng> locations;
    GoogleMapOptions options;
    String originName;
    String destinationName;
    Context context;
    GoogleMap mMap;
    private final Integer LOCATION_REQUEST_CODE = 444;

    public MyMapFragment(){
        super();
    }

    public static MyMapFragment newInstance(List<LatLng> locations, GoogleMapOptions options, Context context, String originName, String destinationName)
    {
        MyMapFragment fragment = new MyMapFragment();
        fragment.locations = locations;
        fragment.options = options;
        fragment.context = context;
        fragment.originName = originName;
        fragment.destinationName = destinationName;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        initMap();
        return view;
    }

    private void initMap(){
        UiSettings settings = getMap().getUiSettings();
        settings.setAllGesturesEnabled(true);
        settings.setMyLocationButtonEnabled(true);
        settings.setMapToolbarEnabled(true);
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                mMap = googleMap;
                if (locations.size() > 1) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LatLng origin = locations.get(0), destination = locations.get(locations.size()-1);
                            googleMap.addMarker(new MarkerOptions().position(origin).title(originName));
                            googleMap.addMarker(new MarkerOptions().position(destination).title(destinationName));
                            LatLngBounds.Builder builder = LatLngBounds.builder().include(origin);
                            for(LatLng point : locations){
                                builder.include(point);
                            }
                            LatLngBounds bounds = builder.build();
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 150);
                            PolylineOptions options = new PolylineOptions().addAll(locations).color(getResources().getColor(R.color.blue));
                            googleMap.addPolyline(options);
                            googleMap.animateCamera(cu);
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                String[] permissions = new String[2];
                                permissions[0] = Manifest.permission.ACCESS_FINE_LOCATION;
                                permissions[1] = Manifest.permission.ACCESS_COARSE_LOCATION;
                                ActivityCompat.requestPermissions(getActivity(), permissions, LOCATION_REQUEST_CODE);
                            }
                        }
                    }, 2000);
                }
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) return;
            }
            if (mMap != null) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);
            }
        }
    }
}
