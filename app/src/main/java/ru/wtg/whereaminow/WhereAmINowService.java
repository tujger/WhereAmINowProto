package ru.wtg.whereaminow;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.wtg.whereaminow.helpers.CurrentState;
import ru.wtg.whereaminow.helpers.Position;
import ru.wtg.whereaminow.helpers.PositionSender;

public class WhereAmINowService extends Service {

    public static final String ACTION = "action";
    public static final int START_TRACKING = 1;
    public static final int STOP_TRACKING = 2;
    public static final int CHECK_TRACKING = 3;

    private CurrentState state;
    final String LOG_TAG = "myLogs";
    ExecutorService es;
    private PositionSender mr;
    private LocationManager locationManager;
    private boolean active = false;
    private ServiceBinder binder = new ServiceBinder();

    public void onCreate() {
        super.onCreate();
        state = CurrentState.getInstance();

        es = Executors.newFixedThreadPool(1);
    }

    @SuppressWarnings("MissingPermission")
    public void onDestroy() {
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "MyService onStartCommand");
        int action = intent.getIntExtra(ACTION,1);

        switch(action){
            case START_TRACKING:
                startTracking();
                break;
            case STOP_TRACKING:
                stopTracking();
                stopSelf(startId);
                break;
            case CHECK_TRACKING:
                sendBroadcast(new Intent(MainActivity.BROADCAST_ACTION)
                        .putExtra(MainActivity.ACTION, MainActivity.ACTION_TRACKING_ACTIVE)
                        .putExtra(MainActivity.BOOLEAN, active));
                stopSelf(startId);
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressWarnings("MissingPermission")
    public void stopTracking() {
        if(!active) return;
        active = false;
        stopForeground(true);
        state.setInfo("Finished");
        state.setTrackingActive(false);
        state.setFriendActive(false);
        state.setToken(null);
        if(locationManager != null) locationManager.removeUpdates(locationListener);
        sendBroadcast(new Intent(MainActivity.BROADCAST_ACTION).putExtra(MainActivity.ACTION, MainActivity.ACTION_TRACKING_STOPPED));
        if(mr != null) mr.stop();
        mr = null;
    }

    @SuppressWarnings("MissingPermission")
    private void startTracking() {
        state.setInfo("Start tracking");

        active = true;
        sendBroadcast(new Intent(MainActivity.BROADCAST_ACTION).putExtra(MainActivity.ACTION, MainActivity.ACTION_TRACKING_STARTED));

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Doing some work...")
                .setContentIntent(pendingIntent).build();

        startForeground(1976, notification);

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 1, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                1000 * 1, 1, locationListener);

        mr = new PositionSender(this).setLocationManager(locationManager).setLocationListener(locationListener);
        es.execute(mr);


    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            Position position = new Position(loc);
            state.my.setPosition(position);
            System.out.println(position.toString());

            mr.update();
        }

        @Override
        public void onProviderDisabled(String provider) {
            System.out.println("GPS SIGNAL LOST");
        }

        @SuppressWarnings("MissingPermission")
        @Override
        public void onProviderEnabled(String provider) {
            state.my.setPosition(locationManager.getLastKnownLocation(provider));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            System.out.println("GPS CHANGED");
        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public boolean isActive() {
        return active;
    }

    class ServiceBinder extends Binder {
        WhereAmINowService getService() {
            return WhereAmINowService.this;
        }
    }
}