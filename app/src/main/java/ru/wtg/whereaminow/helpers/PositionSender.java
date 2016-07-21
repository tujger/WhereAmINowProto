package ru.wtg.whereaminow.helpers;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.concurrent.TimeUnit;

import ru.wtg.whereaminow.MainActivity;

/**
 * Created by tujger on 7/14/16.
 */
public class PositionSender implements Runnable {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private CurrentState state;
    private Service context;
    private Object flag = new Object();

    int time;
    private boolean cancelled;
    private String token;

    public PositionSender(Service context) {
        this.context = context;
        state = CurrentState.getInstance();

            System.out.println("MyRun#" + " create");
    }

    @SuppressWarnings("MissingPermission")
    public void run() {

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(cancelled || !state.isTrackingActive()) return;

        token = String.valueOf(Math.random());
        Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
        intent.putExtra(MainActivity.ACTION, MainActivity.ACTION_SEND_TOKEN);
        intent.putExtra(MainActivity.TEXT, "http://www.whereaminow.com/session/" + token);
        context.sendBroadcast(intent);

        int time = 0;
        while(true) {
            time++;
//            System.out.println("MyRun#"  + " start, time = " + time);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            synchronized (state) {
//                try {
//                    state.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }

            if(!state.isFriendActive() && time>5) {
                Position friendPosition = new Position(state.my.getPosition());
                double lat = friendPosition.getLatitude();
                double lng = friendPosition.getLongitude();
                float br = friendPosition.getBearing();
                friendPosition.setLatitude(lat);
                friendPosition.setLongitude(lng);
                friendPosition.setBearing(br);
                state.friend.setPosition(null);
                state.friend.setPosition(friendPosition);
                state.setFriendActive(true);

                intent = new Intent(MainActivity.BROADCAST_ACTION);
                intent.putExtra(MainActivity.ACTION, MainActivity.ACTION_FRIEND_CONNECTED);
                context.sendBroadcast(intent);
            } else if(state.isFriendActive()) {
                double lat = state.friend.getPosition().getLatitude() + .00001;
                double lng = state.friend.getPosition().getLongitude() + Math.random() * .0002;
                float br = state.friend.getPosition().getBearing() + 1;
                state.friend.getPosition().setLatitude(lat);
                state.friend.getPosition().setLongitude(lng);
                state.friend.getPosition().setBearing(br);
//                System.out.println("INSERVICE:"+state.friend.getPosition().toString());

                intent = new Intent(MainActivity.BROADCAST_ACTION);
                intent.putExtra(MainActivity.ACTION, MainActivity.ACTION_UPDATE);
                context.sendBroadcast(intent);
            }

            if(cancelled){
                System.out.println("CANCELLED#");
                state.setFriendActive(false);
                break;
            }
        }
    }

    public void stop() {
        cancelled = true;
    }


    public PositionSender setLocationManager(LocationManager locationManager) {
        this.locationManager = locationManager;
        return this;
    }

    public PositionSender setLocationListener(LocationListener locationListener) {
        this.locationListener = locationListener;
        return this;
    }

    public void update(){
        synchronized(state){
            state.notify();
        }
    }

}
