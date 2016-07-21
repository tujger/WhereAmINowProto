package ru.wtg.whereaminow;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import ru.wtg.whereaminow.helpers.CurrentState;
import ru.wtg.whereaminow.helpers.FabButtons;
import ru.wtg.whereaminow.helpers.MarkerUpdater;
import ru.wtg.whereaminow.helpers.Position;
import ru.wtg.whereaminow.helpers.PositionSender;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, OnStreetViewPanoramaReadyCallback {

    public final static String BROADCAST_ACTION = "ru.wtg.whereaminow.whereaminowservice";
    public final static String ACTION = "action";
    public final static String TEXT = "text";
    public final static String BOOLEAN = "boolean";
    public final static int ACTION_UPDATE = 1;
    public final static int ACTION_STOP_TIMEOUT = 2;
    public final static int ACTION_TRACKING_STOPPED = 3;
    public final static int ACTION_TRACKING_STARTED = 4;
    public final static int ACTION_FRIEND_CONNECTED = 7;
    public static final int ACTION_ORIENTATION_CHANGED = 5;
    public static final int ACTION_SEND_TOKEN = 8;
    public static final int ACTION_TRACKING_ACTIVE = 9;
    public final static int REQUEST_PERMISSION_LOCATION = 6;


    private LocationManager locationManager = null;
    private CurrentState state;
    private FloatingActionMenu fabActions;
    private FloatingActionMenu fabFriend;
    private FabButtons buttons;
    private Snackbar snackbar;
    private Intent serviceIntent;
    private SupportMapFragment mapView;
    private PositionSender sender;
    private WhereAmINowService service;

    private Boolean flag = false;
    private GoogleMap map;
    private Toolbar toolbar;
    private CameraPosition.Builder cameraPositionBuilder;
    private LatLngBounds region;
    private ServiceConnection serviceConnection;
    private SupportStreetViewPanoramaFragment streetView;
    private StreetViewPanorama panorama;
    private boolean fromHardware;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_track);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        state = CurrentState.getInstance().setContext(getApplicationContext()).load();
        buttons = new FabButtons();

//        System.out.println("STSTS:"+state.isTrackingActive());
//        state.setTrackingActive(false);
//        state.setFriendActive(false);
        //TODO detect that tracking is not active
        serviceIntent = new Intent(MainActivity.this, WhereAmINowService.class);

        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                System.out.println("MainActivity onServiceConnected");
                service = ((WhereAmINowService.ServiceBinder) binder).getService();
                onCreateReady();
            }
            public void onServiceDisconnected(ComponentName name) {
                System.out.println("MainActivity onServiceDisconnected");
                service = null;
            }
        };
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(service != null) unbindService(serviceConnection);
    }

    private void onCreateReady(){

        System.out.println("CONNECTED TO SERVICE, IS ACTIVE:"+service.isActive());
        if(!service.isActive()){
            state.setFriendActive(false).setTrackingActive(false).setScreenMode(CurrentState.SCREEN_MODE_MINE);
        }

        initFabActions();
        initFabFriend();
        initSnackbar();
        findViewById(R.id.button_start_tracking_and_send_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTracking();

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);

        registerReceiver(receiver,intentFilter);

        mapView = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        streetView = (SupportStreetViewPanoramaFragment) getSupportFragmentManager().findFragmentById(R.id.streetView);
        mapView.getMapAsync(this);

//        findViewById(R.id.zoomIn).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                map.animateCamera(CameraUpdateFactory.zoomIn());
//                zoom = map.getCameraPosition().zoom;
//            }
//        });
//        findViewById(R.id.zoomOut).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                map.animateCamera(CameraUpdateFactory.zoomOut());
//                zoom = map.getCameraPosition().zoom;
//            }
//        });
//        findViewById(R.id.findMe).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (current != null) current.remove();
//                current = null;
//                if (circle != null) circle.remove();
//                circle = null;
//                onMapReady(map);
//            }
//        });

    }

    @Override
    protected void onDestroy() {
        state.save();
        super.onDestroy();
        if(receiver != null) unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableLocationManager();
        findViewById(R.id.stickyInfoLayout).setVisibility(View.INVISIBLE);
        if(map != null) onMapReady(map);
    }

    @SuppressWarnings("MissingPermission")
    @Override
    protected void onPause() {
        super.onPause();
        if(locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
//        if(state.friend != null && state.friend.getMarker() != null) state.friend.getMarker().setMarker(null);
//        if(state.my != null && state.my.getMarker() != null) state.my.getMarker().setMarker(null);

        if(state.my != null && state.my.hasMarker()) state.my.setMarker(null);
        if(state.friend != null && state.friend.hasMarker()) state.friend.setMarker(null);
    }

    private void initSnackbar() {
        View fabLayout = findViewById(R.id.fab_layout);
        snackbar = Snackbar.make(fabLayout, "Starting...", Snackbar.LENGTH_INDEFINITE);
        snackbar.getView().setAlpha(.5f);
        snackbar.setAction("Action", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("SNACKBAR ACTION");
            }
        });
        snackbar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                initSnackbar();
            }
        });
        snackbar.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
                System.out.println("SNACKBAR CLICK");
            }
        });
    }

    private void initFabActions(){
        fabActions = (FloatingActionMenu) findViewById(R.id.fab_actions);

        buttons.startTrackingAndSend = (FloatingActionButton) fabActions.findViewById(R.id.fab_start_and_send);
        buttons.sendLink = (FloatingActionButton) fabActions.findViewById(R.id.fab_send_link);
        buttons.stopTracking = (FloatingActionButton) fabActions.findViewById(R.id.fab_stop_tracking);
        buttons.splitScreen = (FloatingActionButton) fabActions.findViewById(R.id.fab_split_screen);
        buttons.cancelTracking = (FloatingActionButton) fabActions.findViewById(R.id.fab_cancel_tracking);

        fabActions.setVisibility(View.GONE);
        fabActions.setClosedOnTouchOutside(true);

        fabActions.removeAllMenuButtons();
        fabActions.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabActions.removeAllMenuButtons();
                if (!fabActions.isOpened()) {
                    if(!state.isTrackingActive())
//                        fabActions.addMenuButton(buttons.startTrackingAndSend);
                    if(state.isTrackingActive() && !state.isFriendActive())
                        fabActions.addMenuButton(buttons.sendLink);
                    if(state.isTrackingActive() && state.isFriendActive())
                        fabActions.addMenuButton(buttons.stopTracking);
                    if(state.isTrackingActive() && !state.isFriendActive())
                        fabActions.addMenuButton(buttons.cancelTracking);
//                    if(state.isFriendActive() && !state.isScreenSplitted())
//                        fabActions.addMenuButton(buttons.splitScreen);
                }
                fabActions.toggle(true);
            }
        });
        fabActions.setOnMenuButtonLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return false;
            }
        });

        buttons.startTrackingAndSend.setOnClickListener(onFabActionsClickListener);
        buttons.sendLink.setOnClickListener(onFabActionsClickListener);
        buttons.stopTracking.setOnClickListener(onFabActionsClickListener);
        buttons.cancelTracking.setOnClickListener(onFabActionsClickListener);
        buttons.splitScreen.setOnClickListener(onFabActionsClickListener);

    }

    private void initFabFriend(){
        fabFriend = (FloatingActionMenu) findViewById(R.id.fab_friend);

        if(state.isFriendActive()) {
            fabFriend.setVisibility(View.VISIBLE);
        } else {
            fabFriend.setVisibility(View.GONE);
        }

        buttons.sendMessage = (FloatingActionButton) fabFriend.findViewById(R.id.fab_send_message);
        buttons.switchToFriend = (FloatingActionButton) fabFriend.findViewById(R.id.fab_switch_to_friend);
        buttons.switchToMe = (FloatingActionButton) fabFriend.findViewById(R.id.fab_switch_to_me);
        buttons.navigate = (FloatingActionButton) fabFriend.findViewById(R.id.fab_navigate);
        buttons.showUs = (FloatingActionButton) fabFriend.findViewById(R.id.fab_show_us);
        buttons.streetView = (FloatingActionButton) fabFriend.findViewById(R.id.fab_street_view);
        buttons.splitStreetViewFriend = (FloatingActionButton) fabFriend.findViewById(R.id.fab_split_street_view_friend);

        fabFriend.removeAllMenuButtons();
        fabFriend.getMenuIconView().setImageResource(R.drawable.ic_person_white_24dp);
        fabFriend.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabFriend.removeAllMenuButtons();
                if(!fabFriend.isOpened()){
                    fabFriend.addMenuButton(buttons.sendMessage);
                    switch(state.getScreenMode()){
                        case CurrentState.SCREEN_MODE_MINE:
                            fabFriend.addMenuButton(buttons.switchToFriend);
                            fabFriend.addMenuButton(buttons.showUs);
                            break;
                        case CurrentState.SCREEN_MODE_FRIEND:
                            fabFriend.addMenuButton(buttons.switchToMe);
                            fabFriend.addMenuButton(buttons.showUs);
                            fabFriend.addMenuButton(buttons.streetView);
                            break;
                        case CurrentState.SCREEN_SHOW_US:
                            fabFriend.addMenuButton(buttons.switchToMe);
                            fabFriend.addMenuButton(buttons.switchToFriend);
                            break;
                        case CurrentState.SCREEN_STREET_VIEW:
                            fabFriend.addMenuButton(buttons.switchToMe);
                            fabFriend.addMenuButton(buttons.switchToFriend);
                            fabFriend.addMenuButton(buttons.splitStreetViewFriend);
                            break;
                        case CurrentState.SCREEN_SPLIT_STREET_VIEW_FRIEND:
                            fabFriend.addMenuButton(buttons.switchToMe);
                            fabFriend.addMenuButton(buttons.switchToFriend);
                            fabFriend.addMenuButton(buttons.showUs);
                            break;
                    }
                    fabFriend.addMenuButton(buttons.navigate);
                    fabFriend.getMenuIconView().setImageResource(R.drawable.ic_add_white_24dp);
                } else {
                    fabFriend.getMenuIconView().setImageResource(R.drawable.ic_person_white_24dp);
                }
                fabFriend.toggle(true);
            }
        });

        buttons.sendMessage.setOnClickListener(onFabFriendClickListener);
        buttons.switchToFriend.setOnClickListener(onFabFriendClickListener);
        buttons.switchToMe.setOnClickListener(onFabFriendClickListener);
        buttons.navigate.setOnClickListener(onFabFriendClickListener);
        buttons.showUs.setOnClickListener(onFabFriendClickListener);
        buttons.streetView.setOnClickListener(onFabFriendClickListener);
        buttons.splitStreetViewFriend.setOnClickListener(onFabFriendClickListener);
    }

    private View.OnClickListener onFabActionsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            fabActions.toggle(true);
            switch (view.getId()){
                case R.id.fab_cancel_tracking:
                    fabActions.removeAllMenuButtons();
                    cancelTracking();
                    System.out.println("FAB STOP TRACKING");
                    break;
                case R.id.fab_split_screen:
                    fabActions.removeAllMenuButtons();
                    splitScreen();
                    System.out.println("FAB SPLIT SCREEN");
                    break;
                case R.id.fab_start_and_send:
                    fabActions.removeAllMenuButtons();
                    startTracking();
                    System.out.println("FAB START AND SEND");
                    break;
                case R.id.fab_send_link:
                    fabActions.removeAllMenuButtons();
                    sendInvite();
                    System.out.println("FAB START AND SEND");
                    break;
                case R.id.fab_stop_tracking:
                    fabActions.removeAllMenuButtons();
                    stopTracking();
                    System.out.println("FAB STOP TRACKING");
                    break;
            }
        }
    };

    private View.OnClickListener onFabFriendClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            fabFriend.toggle(true);
            switch (view.getId()){
                case R.id.fab_send_message:
                    fabFriend.removeAllMenuButtons();
                    System.out.println("FAB SEND MESSAGE");
                    break;
                case R.id.fab_switch_to_me:
                    fabFriend.removeAllMenuButtons();
                    System.out.println("FAB SWITCH TO ME");

                    switchOffStreetView();

                    state.setScreenMode(CurrentState.SCREEN_MODE_MINE);
                    updatePositionView(state.my);
                    break;
                case R.id.fab_switch_to_friend:
                    fabFriend.removeAllMenuButtons();
                    System.out.println("FAB SWITCH TO FRIEND");

                    switchOffStreetView();

                    state.setScreenMode(CurrentState.SCREEN_MODE_FRIEND);
                    updatePositionView(state.friend);
                    break;
                case R.id.fab_navigate:
                    fabFriend.removeAllMenuButtons();
                    System.out.println("FAB NAVIGATE");

                    Uri uri = Uri.parse("google.navigation:q=" + String.valueOf(state.friend.getPosition().getLatitude())
                            + "," + String.valueOf(state.friend.getPosition().getLongitude()));
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            uri);
                    try {
                        startActivity(intent);
                    } catch(ActivityNotFoundException ex) {
                        try {
                            Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(unrestrictedIntent);
                        } catch(ActivityNotFoundException innerEx) {
                            Toast.makeText(getApplicationContext(), "Please install a navigation application.", Toast.LENGTH_LONG).show();
                        }
                    }

//                    state.setScreenMode(CurrentState.SCREEN_MODE_MINE);
//                    updatePositionView(state.my);
                    break;
                case R.id.fab_show_us:
                    fabFriend.removeAllMenuButtons();
                    System.out.println("FAB SHOW US");
                    switchOffStreetView();
                    state.setScreenMode(CurrentState.SCREEN_SHOW_US);
                    updatePositionView(state.my);
                    break;
                case R.id.fab_street_view:
                    fabFriend.removeAllMenuButtons();
                    System.out.println("FAB STREET VIEW");
                    state.setScreenMode(CurrentState.SCREEN_STREET_VIEW);
                    findViewById(R.id.mapViewLayout).setVisibility(View.GONE);
                    findViewById(R.id.placeholderLayout).setVisibility(View.VISIBLE);
                    streetView.getStreetViewPanoramaAsync(MainActivity.this);
                    break;
                case R.id.fab_split_street_view_friend:
                    fabFriend.removeAllMenuButtons();
                    System.out.println("FAB SPLIT STREET VIEW FRIEND");
                    state.setScreenMode(CurrentState.SCREEN_SPLIT_STREET_VIEW_FRIEND);
                    findViewById(R.id.mapViewLayout).setVisibility(View.GONE);
                    findViewById(R.id.placeholderLayout).setVisibility(View.VISIBLE);
                    streetView.getStreetViewPanoramaAsync(MainActivity.this);
//                    updatePositionView(state.friend);
                    break;
            }
        }
    };

    private void switchOffStreetView() {
        if(state.getScreenMode() == CurrentState.SCREEN_STREET_VIEW || state.getScreenMode() == CurrentState.SCREEN_SPLIT_STREET_VIEW_FRIEND){
            findViewById(R.id.streetViewLayout).setVisibility(View.GONE);
            findViewById(R.id.mapViewLayout).setVisibility(View.VISIBLE);
        }
    }

    private void cancelTracking() {
        snackbar.dismiss();
        state.setTrackingActive(false);
        stopService(serviceIntent);
    }

    private void stopTracking() {
        snackbar.dismiss();
        state.setTrackingActive(false);
        if(service != null) service.stopTracking();
        stopService(serviceIntent);
        resetPositionView(state.my);
    }

    private void splitScreen() {
        state.setScreenMode(CurrentState.SCREEN_SPLIT_FRIEND_ABOVE);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        System.out.println("onRequestPermissionsResult="+requestCode);
        switch(requestCode){
            case REQUEST_PERMISSION_LOCATION:
                onMapReadyPermitted();
                break;
        }
    }

    @SuppressWarnings("MissingPermission")
    private void startTracking(){
        System.out.println("MyRun#" + " create");

        state.setTrackingActive(true);

        snackbar.setText("Starting tracking...").setAction("Cancel", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                state.getSnackbar().dismiss();
                state.setTrackingActive(false);
                if(fabActions.isOpened()) fabActions.toggle(true);
                cancelTracking();
            }
        }).show();

        startService(serviceIntent.putExtra(WhereAmINowService.ACTION, WhereAmINowService.START_TRACKING));

    }

    private void adjustButtonPositions() {
        ViewGroup v1 = (ViewGroup) mapView.getView();
        ViewGroup v2 = (ViewGroup) v1.getChildAt(0);
        ViewGroup v3 = (ViewGroup) v2.getChildAt(2);
        View myLocationButton = v3.getChildAt(0);

        myLocationButton.setVisibility(View.VISIBLE);
        myLocationButton.setEnabled(true);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(state.getScreenMode()){
                    case CurrentState.SCREEN_MODE_MINE:
                        resetPositionView(state.my);
                        break;
                    case CurrentState.SCREEN_MODE_FRIEND:
                        resetPositionView(state.friend);
                        break;
                    case CurrentState.SCREEN_SHOW_US:
                        resetPositionView(state.my);
                        break;
                }

            }
        });

        View zoomButtons =  v3.getChildAt(2);
        int positionWidth = zoomButtons.getLayoutParams().width;
        int positionHeight = zoomButtons.getLayoutParams().height;

        RelativeLayout.LayoutParams zoomParams = new RelativeLayout.LayoutParams(positionWidth,positionHeight);
        int margin = positionWidth/5;
        zoomParams.setMargins(margin, 0, 0, margin);
        zoomParams.addRule(RelativeLayout.BELOW, myLocationButton.getId());
        zoomParams.addRule(RelativeLayout.ALIGN_LEFT, myLocationButton.getId());
        zoomButtons.setLayoutParams(zoomParams);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /*   @Override
       public boolean onCreateOptionsMenu(Menu menu) {
           // Inflate the menu; this adds items to the action bar if it is present.
           getMenuInflater().inflate(R.menu.my_track, menu);
           return true;
       }
   */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_type) {
            if (map.getMapType() == GoogleMap.MAP_TYPE_SATELLITE) {
                map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            } else if (map.getMapType() == GoogleMap.MAP_TYPE_TERRAIN) {
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            } else if (map.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            } else if (map.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            }
        } else if (id == R.id.nav_start_and_share) {
        } else if (id == R.id.nav_follow_me) {

        } else if (id == R.id.nav_settings) {
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        System.out.println("ONMAPREADY");
        this.map = map;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSION_LOCATION);
            System.out.println("FIRST CHECK");
        } else {
            onMapReadyPermitted();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void onMapReadyPermitted(){
        System.out.println("ONMAPREADYPERMITTED");
        adjustButtonPositions();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            findViewById(R.id.stickyInfoLayout).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.stickyInfo)).setText("GPS disabled");
            findViewById(R.id.placeholderLayout).setVisibility(View.GONE);
            snackbar.setText("GPS disabled. Click here to enable.").setAction("Enable GPS",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    }).show();
            return;
        }

        enableLocationManager();

//        map.setMyLocationEnabled(true);
        map.setBuildingsEnabled(true);
        map.setIndoorEnabled(true);
        map.setOnCameraChangeListener(onCameraChangeListener);
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                System.out.println("MAP CLICK");
            }
        });
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick ( Marker marker ) {

                Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
                intent.putExtra(MainActivity.ACTION, MainActivity.ACTION_ORIENTATION_CHANGED);
                intent.putExtra(MainActivity.TEXT, marker.getId());
                getApplicationContext().sendBroadcast(intent);

                return true;
            }
        });
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setAllGesturesEnabled(true);
        map.getUiSettings().setIndoorLevelPickerEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
//        map.getUiSettings().setMapToolbarEnabled(true);

        Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(lastKnown == null) lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(lastKnown == null) lastKnown = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if(lastKnown != null) {
            state.my.setPosition(new Position(lastKnown));
            new Handler().post(new UpdateAddress());
            if(!state.isTrackingActive()) {
                resetPositionView(state.my);
            }
        } else {
            new Handler().post(locateMeThread);
        }

        updatePositionView(state.my);
        findViewById(R.id.placeholderLayout).setVisibility(View.GONE);

    }

    @SuppressWarnings("MissingPermission")
    private void enableLocationManager() {
        if(locationManager != null && locationListener != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 1, 1, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 1, 1, locationListener);
        }
    }

    GoogleMap.OnCameraChangeListener onCameraChangeListener = new GoogleMap.OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            if(cameraPositionBuilder == null) return;
            CurrentState.User user = null;
            switch(state.getScreenMode()) {
                case CurrentState.SCREEN_MODE_MINE:
                    user = state.my;
                    break;
                case CurrentState.SCREEN_MODE_FRIEND:
                    user = state.friend;
                    break;
            }
            System.out.println("C");
            if(fromHardware) {
                fromHardware = false;
            }else{
                if(user != null && user.getZoom() != cameraPosition.zoom){
                    user.setZoom(cameraPosition.zoom);
                    cameraPositionBuilder.zoom(cameraPosition.zoom);

                    System.out.println("onCameraChange zoom="+cameraPosition.zoom);
                } else if(user != null) {
                    user.setOrientation(CurrentState.ORIENTATION_STAY);

                    user.setOrientation(CurrentState.ORIENTATION_STAY);
                    System.out.println("SET ORIENTATION STAY");
                } else {
                    cameraPositionBuilder.zoom(cameraPosition.zoom);
                }
            }
            cameraPositionBuilder.target(cameraPosition.target);
        }
    };

    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        return gpsStatus;
    }

    private void resetPositionView(final CurrentState.User user){
        switchOffStreetView();
        if(!state.isFriendActive())state.setScreenMode(CurrentState.SCREEN_MODE_MINE);
        user.setOrientation(CurrentState.ORIENTATION_NORTH);
        if(cameraPositionBuilder != null){
            cameraPositionBuilder.zoom(15);
        }
        user.setZoom(15);
        updatePositionView(user);
    }

    private void updatePositionView(final CurrentState.User user){
        CameraUpdate camera;

        System.out.println("UPDATE CAMERA, MODE "+state.getScreenMode()+", ORIENTATION "+user.getOrientation()+", zoom "+user.getZoom());

        new Handler().post(new UpdateAddress());

        if (cameraPositionBuilder == null) {
            cameraPositionBuilder = new CameraPosition.Builder()
                    .target(user.getPosition().getLatLng())
                    .bearing(0)
                    .zoom(user.getZoom())
                    .tilt(0);
            camera = CameraUpdateFactory.newCameraPosition(cameraPositionBuilder.build());
        } else {
            region = null;
            if (state.getScreenMode() == CurrentState.SCREEN_SHOW_US) {
// Create a LatLngBounds that includes Australia.
                double lat1 = state.my.getPosition().getLatitude();
                double lat2 = state.friend.getPosition().getLatitude();
                double lng1 = state.my.getPosition().getLongitude();
                double lng2 = state.friend.getPosition().getLongitude();
                LatLng latLngLB = new LatLng(Math.min(lat1, lat2), Math.min(lng1, lng2));
                LatLng latLngRT = new LatLng(Math.max(lat1, lat2), Math.max(lng1, lng2));

                region = new LatLngBounds(latLngLB, latLngRT);
                camera = CameraUpdateFactory.newLatLngBounds(region,state.my.getMarkerWidth());
            } else {
                switch (user.getOrientation()) {
                    case CurrentState.ORIENTATION_NORTH:
                        cameraPositionBuilder.target(user.getPosition().getLatLng()).bearing(0).tilt(0);
                        break;
                    case CurrentState.ORIENTATION_DIRECTION:
                        cameraPositionBuilder.target(user.getPosition().getLatLng()).bearing(user.getPosition().getBearing()).tilt(0);
                        break;
                    case CurrentState.ORIENTATION_PERSPECTIVE:
                        cameraPositionBuilder.target(user.getPosition().getLatLng()).bearing(user.getPosition().getBearing()).tilt(60);
                        break;
                    case CurrentState.ORIENTATION_STAY:
                        break;
                    case CurrentState.ORIENTATION_USER:
                        cameraPositionBuilder.target(user.getPosition().getLatLng());
                        break;
                }
                camera = CameraUpdateFactory.newCameraPosition(cameraPositionBuilder.build());
            }
        }

        fromHardware = true;
        System.out.println("A");
        map.animateCamera(camera, 1500,
                new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
//                        System.out.println("ANIMATION FINICHED");
                    }

                    @Override
                    public void onCancel() {
//                        System.out.println("ANIMATION CNCELLED");
                    }
                });

        if(state.isFriendActive()){
            findViewById(R.id.stickyInfoLayout).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.stickyInfo))
                    .setText("Between us "+String.valueOf((int)state.my.getPosition().distanceTo(state.friend.getPosition()))+" feet(s)");
        }

    }

    private Thread locateMeThread = new Thread() {
        @SuppressWarnings("MissingPermission")
        public void run() {

            flag = displayGpsStatus();
            if (flag) {

                toolbar.setSubtitle("Move yourself to detect the motion.");

//                map.setMyLocationEnabled(true);
//                map.getUiSettings().setZoomControlsEnabled(true);


            } else {
                toolbar.setSubtitle("Your GPS is off.");
            }
            return;
        }
    };

    private class UpdateAddress extends Thread {
        CurrentState.User user;
        @Override
        public void run() {
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            if(state.getScreenMode() == CurrentState.SCREEN_MODE_FRIEND && state.friend != null){
                user = state.friend;
            } else {
                user = state.my;
            }

            try {
                List<Address> addresses = gcd.getFromLocation(user.getPosition().getLatitude(), user.getPosition().getLongitude(), 1);
                if (addresses.size() > 0) {
                    String locationDescription = addresses.get(0).getAddressLine(0) + ", " + addresses.get(0).getAddressLine(1);
                    toolbar.setSubtitle(locationDescription);
                }
            } catch (IOException | IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getIntExtra(ACTION,0)){
                case ACTION_UPDATE:
                    System.out.println("RECEIVE ACTION_UPDATE");
                    updateFromService();
                    break;
                case ACTION_FRIEND_CONNECTED:
                    System.out.println("RECEIVE ACTION_FRIEND_CONNECTED");
                    if(fabActions.isOpened()){
                        fabActions.removeAllMenuButtons();
                        fabActions.toggle(true);
                    }
                    findViewById(R.id.stickyInfoLayout).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.stickyInfo)).setText("Your friend is on touch!");
                    updateFromService();
                    break;
                case ACTION_STOP_TIMEOUT:
                    System.out.println("RECEIVE ACTION_STOP_TIMEOUT");
                    break;
                case ACTION_SEND_TOKEN:
                    state.setToken(intent.getStringExtra(TEXT));
                    if(fabActions.isOpened()) fabActions.toggle(true);
                    sendInvite();
                    System.out.println("RECEIVE ACTION_SEND_TOKEN");
                    break;
                case ACTION_TRACKING_STARTED:
                    System.out.println("RECEIVE ACTION_TRACKING_STARTED");
                    findViewById(R.id.stickyInfoLayout).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.stickyInfo)).setText("Tracking started");

                    findViewById(R.id.button_start_tracking_and_send_link).setVisibility(View.GONE);
                    fabActions.setVisibility(View.VISIBLE);

                    break;
                case ACTION_TRACKING_STOPPED:
                    System.out.println("RECEIVE ACTION_TRACKING_STOPPED");
                    findViewById(R.id.stickyInfoLayout).setVisibility(View.GONE);
                    findViewById(R.id.button_start_tracking_and_send_link).setVisibility(View.VISIBLE);
                    fabActions.setVisibility(View.GONE);

                    break;
                case ACTION_ORIENTATION_CHANGED:
                    System.out.println("RECEIVE ACTION_ORIENTATION_CHANGED");

                    String id = intent.getStringExtra(TEXT);
                    CurrentState.User user = state.getUserByMarkerId(id);
                    if(user.hasMarker()) {
                        if(state.getScreenMode() != CurrentState.SCREEN_SHOW_US) {
                            switchOrientation(user);
                        }
                        if(state.getScreenMode() != CurrentState.SCREEN_SPLIT_STREET_VIEW_FRIEND) {
                            switch (user.getId()) {
                                case CurrentState.ID_MY:
                                    state.setScreenMode(CurrentState.SCREEN_MODE_MINE);
                                    break;
                                case CurrentState.ID_FRIEND:
                                    state.setScreenMode(CurrentState.SCREEN_MODE_FRIEND);
                                    break;
                            }
                        }
                        updatePositionView(user);
                        Toast.makeText(getApplicationContext(), (new String[]{
                                "North on top.","Easy navigation.","Easy navigation 3D.","User defined location.",null,null,null,null,null,"Show us both.","Userable."
                        })[user.getOrientation()], Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private void switchOrientation(CurrentState.User user) {
        int orientation = user.getOrientation();
        int previousOrientation = 0;
        if(orientation <= 3) previousOrientation = orientation;
        orientation ++;
        if(orientation > 9) orientation = previousOrientation;
        else if(orientation > 3) orientation = 0;
        user.setOrientation(orientation);
    }

    private void sendInvite() {
        snackbar.dismiss();

        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        share.putExtra(Intent.EXTRA_SUBJECT, "Link to the track");
        share.putExtra(Intent.EXTRA_TEXT, state.getToken());

        startActivity(Intent.createChooser(share, "Send link to friend"));
    }

    private void updateFromService() {
        if(state.isTrackingActive() && state.isFriendActive()){
            fabFriend.setVisibility(View.VISIBLE);
        } else {
            fabFriend.setVisibility(View.GONE);
            state.friend.getMarker().remove();
            state.friend.setMarker(null);
        }

        if(state.isFriendActive()){
            if(!state.friend.hasMarker()){
                state.friend.setMarker(new MarkerUpdater(getApplicationContext(),map)
                        .setLocation(state.friend.getPosition()));
            }
            state.friend.getMarker().update();

            switch(state.getScreenMode()){
                case CurrentState.SCREEN_MODE_MINE:
                    updatePositionView(state.my);
                    break;
                case CurrentState.SCREEN_MODE_FRIEND:
                    updatePositionView(state.friend);
                    break;
                case CurrentState.SCREEN_SHOW_US:
                    updatePositionView(state.my);
                    break;
                case CurrentState.SCREEN_STREET_VIEW:
                    if(panorama != null) {
                        new Handler().post(new UpdateAddress());
                        panorama.setPosition(state.friend.getPosition().getLatLng());
                    }
                    break;
                case CurrentState.SCREEN_SPLIT_STREET_VIEW_FRIEND:
                    updatePositionView(state.friend);
                    if(panorama != null) {
                        panorama.setPosition(state.friend.getPosition().getLatLng());
                    }
                    break;
            }
        } else {
            updatePositionView(state.my);
        }
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            state.my.setPosition(location);
            if(state.my.getMarker() == null){
//                fabActions.setVisibility(View.VISIBLE);
                state.my.setMarker(new MarkerUpdater(getApplicationContext(),map)
                        .setLocation(state.my.getPosition()));
            }
            state.my.getMarker().update();

            state.save();
            switch(state.getScreenMode()){
                case CurrentState.SCREEN_MODE_MINE:
                    updatePositionView(state.my);
                    break;
                case CurrentState.SCREEN_MODE_FRIEND:
                    updatePositionView(state.friend);
                    break;
                case CurrentState.SCREEN_SHOW_US:
                    updatePositionView(state.my);
                    break;
            }
//            System.out.println("MAIN"+location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            new Handler().post(locateMeThread);
        }

        @SuppressWarnings("MissingPermission")
        public void onProviderEnabled(String provider) {
            state.my.setPosition(new Position(locationManager.getLastKnownLocation(provider)));
            new Handler().post(new UpdateAddress());
//            new Handler().post(locateMeThread);
            resetPositionView(state.my);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        findViewById(R.id.placeholderLayout).setVisibility(View.GONE);

        findViewById(R.id.streetViewLayout).setVisibility(View.VISIBLE);
        this.panorama = panorama;

        if(state.getScreenMode() == CurrentState.SCREEN_SPLIT_STREET_VIEW_FRIEND){
            findViewById(R.id.mapViewLayout).setVisibility(View.VISIBLE);
        }

        panorama.setPosition(state.friend.getPosition().getLatLng());
    }

}
