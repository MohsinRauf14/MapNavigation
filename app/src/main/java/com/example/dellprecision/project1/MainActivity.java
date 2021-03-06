package com.example.dellprecision.project1;

import android.location.Location;
//import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMapClickListener;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.Mapbox.getAccessToken;

public class MainActivity extends AppCompatActivity
       implements
       OnMapReadyCallback,LocationEngineListener,
       PermissionsListener,
       OnMapClickListener
        {

            private MapView mapView;
            private MapboxMap map;
            private Button startButton;
            private PermissionsManager permissionsManager;
            private LocationEngine locationEngine;
            private LocationLayerPlugin locationLayerPlugin;
            private Location originlocation;
            private Point originPosition;
            private Point destinationPosition;
            public Marker destinationMarker;
            private NavigationMapRoute navigationMapRoute;
            private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1Ijoic3Ryb20iLCJhIjoiY2pxa3ptYmExMGllYzQzczF3cDVpN2djMCJ9.vMOA084r6aw_Ud3DUPvPIQ");
        setContentView(R.layout.activity_main);
       // Mapbox.getInstance(this,getString(R.string.AccessToken));
        mapView =  findViewById(R.id.mapView);
        startButton = findViewById(R.id.startButton);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

       startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .origin(originPosition)
                        .destination(destinationPosition)
                        .shouldSimulateRoute(true)
                        .build();

                NavigationLauncher.startNavigation(MainActivity.this,options );
            }
        });

    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map.addOnMapClickListener(this);
        enableLocation();
    }

    private void enableLocation(){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
            initializeLocationlayer();
        }else{
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine(){
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation =  locationEngine.getLastLocation();
        if(lastLocation != null){
            originlocation = lastLocation;
            setCameraPosition(lastLocation);
        }else{
            locationEngine.addLocationEngineListener(this);
        }
    }
    @SuppressWarnings("MissingPermission")
    private void initializeLocationlayer(){
        locationLayerPlugin = new LocationLayerPlugin(mapView,map,locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
    }

    private void setCameraPosition(Location location){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()),13.0));
    }


    public void onMapClick(@NonNull LatLng point) {
        destinationMarker = map.addMarker(new MarkerOptions().position(point));

        destinationPosition = Point.fromLngLat(point.getLongitude(),point.getLatitude());
        originPosition   = Point.fromLngLat(originlocation.getLongitude(),originlocation.getLatitude());
        getRoute(originPosition,destinationPosition);

        startButton.setEnabled(false);
        startButton.setBackgroundResource(R.color.mapbox_blue);
    }

    private void getRoute(Point origin,Point destination){
        assert getAccessToken() != null;
        NavigationRoute.builder()
                .accessToken(getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
                        if (response.body()== null){
                            Log.e(TAG ,"No Routes Found, Check right user and access token");
                            return;
                        }else if(response.body().routes().size()==0 ){
                            Log.e(TAG,"No Routes Found");
                            return;
                        }

                        DirectionsRoute currentRoute = response.body().routes().get(0);

                        if (navigationMapRoute!=null){
                            navigationMapRoute.removeRoute();
                        }else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, map);
                        }

                        navigationMapRoute.addRoute(currentRoute);
                    }


                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e(TAG,"Error:" +t.getMessage());
                    }
                });
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null){
            originlocation = location;
            setCameraPosition(location);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //present dialog
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            enableLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @Override
    @SuppressWarnings("MissingPermission")
    protected void onStart() {
        super.onStart();
        if (locationEngine != null){
            locationEngine.requestLocationUpdates();
        }
        if(locationLayerPlugin != null){
            locationLayerPlugin.onStart();
        }
        mapView.onStart();


    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null){
            locationEngine.removeLocationUpdates();
        }
        if (locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationEngine != null){
            locationEngine.deactivate();
        }
        mapView.onDestroy();
    }

}
