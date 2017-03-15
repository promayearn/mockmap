package com.example.pro.mockmap;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pro.mockmap.Modules.DirectionFinder;
import com.example.pro.mockmap.Modules.DirectionFinderListener;
import com.example.pro.mockmap.Modules.Route;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.location.LocationListener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, DirectionFinderListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "MapsActivity";
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final CharSequence[] MAP_TYPE_ITEMS =
            {"Normal", "Satellite"};

    private GoogleMap mMap;
    protected UiSettings mUiSettings;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private boolean TRAFFIC_MODE = false;
    private LatLng defaultLocation = new LatLng(13.7123814, 100.6072075);
    private String mSearchKey;
    private String travelMode = "driving";

    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;

    private FloatingActionButton mFloatButton;
    private boolean[] filter = new boolean[24];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mFloatButton = (FloatingActionButton) findViewById(R.id.fab);

        mFloatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filterMarkerTypeDialog();
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mUiSettings = mMap.getUiSettings();

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        setDefaultFilter();
        addMarker();

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                marker.setDraggable(false);
                marker.hideInfoWindow();
            }
        });

        if (!checkLocationPermission()) {
            addMarkerToGoogleMap(defaultLocation, "Marker in Default", 25);

            // Permission denied, Disable the functionality that depends on this permission.
            //zoom to default position:
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(defaultLocation).zoom(16).build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            Toast.makeText(this, "Permission denied set start at default location", Toast.LENGTH_LONG).show();
        }

        mMap.setOnInfoWindowLongClickListener(new GoogleMap.OnInfoWindowLongClickListener() {
            @Override
            public void onInfoWindowLongClick(Marker marker) {
                if (checkLocationPermission()) {
                    LatLng originLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    getDirections(originLocation, marker.getPosition());
                } else {
                    getDirections(defaultLocation, marker.getPosition());

                }
            }
        });

        //show building
        mMap.setBuildingsEnabled(true);
        //compass
        mUiSettings.setCompassEnabled(true);
        //navigation option
        mUiSettings.setMapToolbarEnabled(false);
    }

    private void addMarker() {

        if (!checkLocationPermission()) {
            addMarkerToGoogleMap(defaultLocation, "Marker in Default", 25);
        }
        LatLng bigC = new LatLng(13.7094186, 100.6015668);
        LatLng lotus = new LatLng(13.7051772, 100.6009256);
        LatLng hospital = new LatLng(13.7161837, 100.7105331);
        LatLng cafe = new LatLng(13.7131158, 100.6307685);
        LatLng airport = new LatLng(13.6917497, 100.7477188);
        LatLng restaurant = new LatLng(13.7091808, 100.6347194);
        LatLng carparking = new LatLng(13.713762, 100.6323993);
        LatLng atm = new LatLng(13.7091808, 100.6347194);
        LatLng school = new LatLng(13.7094935, 100.6285073);
        LatLng bts = new LatLng(13.7085982, 100.5997431);

        addMarkerToGoogleMap(bigC, "Big C", 14);
        addMarkerToGoogleMap(lotus, "Lotus", 14);
        addMarkerToGoogleMap(hospital, "Hospital", 12);
        addMarkerToGoogleMap(cafe, "cafe", 6);
        addMarkerToGoogleMap(airport, "Airport", 1);
        addMarkerToGoogleMap(restaurant, "Restaurant", 18);
        addMarkerToGoogleMap(carparking, "Car parking", 4);
        addMarkerToGoogleMap(atm, "ATM", 2);
        addMarkerToGoogleMap(school, "School", 19);
        addMarkerToGoogleMap(bts, "BTS", 23);
    }

    private void setDefaultFilter() {
        for (int i = 0; i < 24; i++) {
            filter[i] = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQuery(mSearchKey, false);//
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Query text submitted: " + query);
                mSearchKey = query;
                onSearch();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Query text changing: " + newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchView.setQuery(mSearchKey, false);
            }
        });
        return true;
    }

    @Override
    public void onBackPressed() {
    }

    public void onSearch() {

        List<Address> addressList = null;

        if (mSearchKey != null || !mSearchKey.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(mSearchKey, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng).zoom(16).build();

            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_map_type:
                showMapTypeSelectorDialog();
                return true;
            case R.id.menu_traffic:
                setTraffic();
                return true;
            case R.id.menu_travel:
                if (travelMode.equals("driving")) {
                    travelMode = "walking";
                    Toast.makeText(this, "Travel Mode: Walking", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (travelMode.equals("walking")) {
                    travelMode = "bicycling";
                    Toast.makeText(this, "Travel Mode: Bicycling", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (travelMode.equals("bicycling")) {
                    travelMode = "transit";
                    Toast.makeText(this, "Travel Mode: Transit", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    travelMode = "driving";
                    Toast.makeText(this, "Travel Mode: Driving", Toast.LENGTH_SHORT).show();
                    return true;
                }
        }
        return true;
    }

    private void setTraffic() {
        if (TRAFFIC_MODE) {
            mMap.setTrafficEnabled(false);
            TRAFFIC_MODE = false;
        } else {
            mMap.setTrafficEnabled(true);
            TRAFFIC_MODE = true;
        }
    }

    private void showMapTypeSelectorDialog() {
        // Prepare the dialog by setting up a Builder.
        final String fDialogTitle = "Select Map Type";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(fDialogTitle);

        // Find the current map type to pre-check the item representing the current state.
        int checkItem = mMap.getMapType() - 1;

        // Add an OnClickListener to the dialog, so that the selection will be handled.
        builder.setSingleChoiceItems(
                MAP_TYPE_ITEMS,
                checkItem,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {
                        // Locally create a finalised object.

                        // Perform an action depending on which item was selected.
                        switch (item) {
                            case 1:
                                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                break;
                            default:
                                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }
                        dialog.dismiss();
                    }
                }
        );
        // Build the dialog and show it.
        AlertDialog fMapTypeDialog = builder.create();
        fMapTypeDialog.setCanceledOnTouchOutside(true);
        fMapTypeDialog.show();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    public void addMarkerToGoogleMap(LatLng position, String title, int type) {

        MarkerOptions options = new MarkerOptions();
        options.position(new LatLng(position.latitude, position.longitude));
        options.title(title);
        options.draggable(false).visible(true);

        if (type == 1 && !filter[0]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_airplane));
            mMap.addMarker(options);
        } else if (type == 2 && !filter[1]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bank));
            mMap.addMarker(options);
        } else if (type == 3 && !filter[2]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_car));
            mMap.addMarker(options);
        } else if (type == 4 && !filter[3]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_car_park));
            mMap.addMarker(options);
        } else if (type == 5 && !filter[4]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_cinema));
            mMap.addMarker(options);
        } else if (type == 6 && !filter[5]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_coffee));
            mMap.addMarker(options);
        } else if (type == 7 && !filter[6]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_dessert));
            mMap.addMarker(options);
        } else if (type == 8 && !filter[7]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_fitness));
            mMap.addMarker(options);
        } else if (type == 9 && !filter[8]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_gas_station));
            mMap.addMarker(options);
        } else if (type == 10 && !filter[9]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_government));
            mMap.addMarker(options);
        } else if (type == 11 && !filter[10]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_home));
            mMap.addMarker(options);
        } else if (type == 12 && !filter[11]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_hospital));
            mMap.addMarker(options);
        } else if (type == 13 && !filter[12]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_hotel));
            mMap.addMarker(options);
        } else if (type == 14 && !filter[13]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_mall));
            mMap.addMarker(options);
        } else if (type == 15 && !filter[14]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_mountain));
            mMap.addMarker(options);
        } else if (type == 16 && !filter[15]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_other));
            mMap.addMarker(options);
        } else if (type == 17 && !filter[16]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_park));
            mMap.addMarker(options);
        } else if (type == 18 && !filter[17]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_restaurant));
            mMap.addMarker(options);
        } else if (type == 19 && !filter[18]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_school));
            mMap.addMarker(options);
        } else if (type == 20 && !filter[19]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_sea));
            mMap.addMarker(options);
        } else if (type == 21 && !filter[20]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_ship));
            mMap.addMarker(options);
        } else if (type == 22 && !filter[21]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_temple));
            mMap.addMarker(options);
        } else if (type == 23 && !filter[22]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_train));
            mMap.addMarker(options);
        } else if (type == 24 && !filter[23]) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_work));
            mMap.addMarker(options);
        } else if (type == 25) {
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
            mMap.addMarker(options);
        }
    }

    public void getDirections(LatLng sourcePosition, LatLng destPosition) {
        Double latSource = sourcePosition.latitude;
        Double lngSource = sourcePosition.longitude;
        Double latDes = destPosition.latitude;
        Double lngDes = destPosition.longitude;
        String origin = latSource.toString() + "," + lngSource.toString();
        String destination = latDes.toString() + "," + lngDes.toString();

        if (origin.isEmpty()) {
            Toast.makeText(this, "Please enter origin address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destination.isEmpty()) {
            Toast.makeText(this, "Please enter destination address!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            new DirectionFinder(this, origin, destination, travelMode).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline : polylinePaths) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 15));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.CYAN).width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            //You can add here other case statements according to your requirement.
        }
    }

    private void filterMarkerTypeDialog() {

        CheckBox checkBox1, checkBox2, checkBox3, checkBox4, checkBox5, checkBox6, checkBox7, checkBox8,
                checkBox9, checkBox10, checkBox11, checkBox12, checkBox13, checkBox14, checkBox15, checkBox16,
                checkBox17, checkBox18, checkBox19, checkBox20, checkBox21, checkBox22, checkBox23, checkBox24;
        TextView okTextView;

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.marker_filter_dialog);
        dialog.setTitle(R.string.select_filter_please);

        okTextView = (TextView) dialog.findViewById(R.id.text_ok);
        okTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                mMap.clear();
                addMarker();
            }
        });

        checkBox1 = (CheckBox) dialog.findViewById(R.id.checkbox_1);
        if (filter[0]) {
            checkBox1.setChecked(filter[0]);
        }
        checkBox1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[0] = !filter[0];
            }
        });

        checkBox2 = (CheckBox) dialog.findViewById(R.id.checkbox_2);
        if (filter[1]) {
            checkBox2.setChecked(filter[1]);
        }
        checkBox2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[1] = !filter[1];
            }
        });

        checkBox3 = (CheckBox) dialog.findViewById(R.id.checkbox_3);
        if (filter[2]) {
            checkBox3.setChecked(filter[2]);
        }
        checkBox3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[2] = !filter[2];
            }
        });

        checkBox4 = (CheckBox) dialog.findViewById(R.id.checkbox_4);
        if (filter[3]) {
            checkBox4.setChecked(filter[3]);
        }
        checkBox4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[3] = !filter[3];
            }
        });

        checkBox5 = (CheckBox) dialog.findViewById(R.id.checkbox_5);
        if (filter[4]) {
            checkBox5.setChecked(filter[4]);
        }
        checkBox5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[4] = !filter[4];
            }
        });

        checkBox6 = (CheckBox) dialog.findViewById(R.id.checkbox_6);
        if (filter[5]) {
            checkBox6.setChecked(filter[5]);
        }
        checkBox6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[5] = !filter[5];
            }
        });

        checkBox7 = (CheckBox) dialog.findViewById(R.id.checkbox_7);
        if (filter[6]) {
            checkBox7.setChecked(filter[6]);
        }
        checkBox7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[6] = !filter[6];
            }
        });

        checkBox8 = (CheckBox) dialog.findViewById(R.id.checkbox_8);
        if (filter[7]) {
            checkBox8.setChecked(filter[7]);
        }
        checkBox8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[7] = !filter[7];
            }
        });

        checkBox9 = (CheckBox) dialog.findViewById(R.id.checkbox_9);
        if (filter[8]) {
            checkBox9.setChecked(filter[8]);
        }
        checkBox9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[8] = !filter[8];
            }
        });

        checkBox10 = (CheckBox) dialog.findViewById(R.id.checkbox_10);
        if (filter[9]) {
            checkBox10.setChecked(filter[9]);
        }
        checkBox10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[9] = !filter[9];
            }
        });

        checkBox11 = (CheckBox) dialog.findViewById(R.id.checkbox_11);
        if (filter[10]) {
            checkBox11.setChecked(filter[10]);
        }
        checkBox11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[10] = !filter[10];
            }
        });

        checkBox12 = (CheckBox) dialog.findViewById(R.id.checkbox_12);
        if (filter[11]) {
            checkBox12.setChecked(filter[11]);
        }
        checkBox12.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[11] = !filter[11];
            }
        });

        checkBox13 = (CheckBox) dialog.findViewById(R.id.checkbox_13);
        if (filter[12]) {
            checkBox13.setChecked(filter[12]);
        }
        checkBox13.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[12] = !filter[12];
            }
        });

        checkBox14 = (CheckBox) dialog.findViewById(R.id.checkbox_14);
        if (filter[13]) {
            checkBox14.setChecked(filter[13]);
        }
        checkBox14.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[13] = !filter[13];
            }
        });

        checkBox15 = (CheckBox) dialog.findViewById(R.id.checkbox_15);
        if (filter[14]) {
            checkBox15.setChecked(filter[14]);
        }
        checkBox15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[14] = !filter[14];
            }
        });

        checkBox16 = (CheckBox) dialog.findViewById(R.id.checkbox_16);
        if (filter[15]) {
            checkBox16.setChecked(filter[15]);
        }
        checkBox16.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[15] = !filter[15];
            }
        });

        checkBox17 = (CheckBox) dialog.findViewById(R.id.checkbox_17);
        if (filter[16]) {
            checkBox17.setChecked(filter[16]);
        }
        checkBox17.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[16] = !filter[16];
            }
        });

        checkBox18 = (CheckBox) dialog.findViewById(R.id.checkbox_18);
        if (filter[17]) {
            checkBox18.setChecked(filter[17]);
        }
        checkBox18.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[17] = !filter[17];
            }
        });

        checkBox19 = (CheckBox) dialog.findViewById(R.id.checkbox_19);
        if (filter[18]) {
            checkBox19.setChecked(filter[18]);
        }
        checkBox19.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[18] = !filter[18];
            }
        });

        checkBox20 = (CheckBox) dialog.findViewById(R.id.checkbox_20);
        if (filter[19]) {
            checkBox20.setChecked(filter[19]);
        }
        checkBox20.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[19] = !filter[19];
            }
        });

        checkBox21 = (CheckBox) dialog.findViewById(R.id.checkbox_21);
        if (filter[20]) {
            checkBox21.setChecked(filter[20]);
        }
        checkBox21.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[20] = !filter[20];
            }
        });

        checkBox22 = (CheckBox) dialog.findViewById(R.id.checkbox_22);
        if (filter[21]) {
            checkBox22.setChecked(filter[21]);
        }
        checkBox22.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[21] = !filter[21];
            }
        });

        checkBox23 = (CheckBox) dialog.findViewById(R.id.checkbox_23);
        if (filter[22]) {
            checkBox23.setChecked(filter[22]);
        }
        checkBox23.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[22] = !filter[22];
            }
        });

        checkBox24 = (CheckBox) dialog.findViewById(R.id.checkbox_24);
        if (filter[23]) {
            checkBox24.setChecked(filter[23]);
        }
        checkBox24.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter[23] = !filter[23];
            }
        });
        dialog.show();
    }
}