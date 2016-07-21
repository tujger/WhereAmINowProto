package ru.wtg.whereaminow.helpers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import ru.wtg.whereaminow.MainActivity;
import ru.wtg.whereaminow.R;

/**
 * Created by tujger on 7/16/16.
 */
public class MarkerUpdater {

    private final Context context;
    private GoogleMap map;
    private Position location;
    private Marker marker;
    private Circle circle;
//    private int orientation;
    private int previousOrientation;


    public MarkerUpdater(Context context, GoogleMap map){
        this.context = context;
        this.map = map;
    }

    public MarkerUpdater update(){
        System.out.println("MARKER:"+location.toString());

        if(marker == null){
//            myMarker.remove();
//            Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
//                    R.drawable.ic_navigation_white_24dp);

            Drawable drawable = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                drawable = context.getResources().getDrawable(R.drawable.navigation_marker,context.getTheme());
            } else {
                drawable = context.getResources().getDrawable(R.drawable.navigation_marker);
            }
            drawable.setColorFilter(location.getMarkerColor(),PorterDuff.Mode.MULTIPLY);


            Canvas canvas = new Canvas();
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            location.setMarkerWidth(bitmap.getWidth());

            marker = map.addMarker(new MarkerOptions()
                    .position(location.getLatLng())
                    .rotation(location.getBearing())
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));


             CircleOptions circleOptions = new CircleOptions()
                    .center(location.getLatLng()).radius(location.getAccuracy())
                    .fillColor(location.getFillColor()).strokeColor(location.getStrokeColor()).strokeWidth(2f);
//            circle = map.addCircle(circleOptions);
        } else {
//            myMarker.setPosition(state.getMyPosition().getLatLng());
            marker.setRotation(location.getBearing());
//            myCircle.setCenter(state.getMyPosition().getLatLng());
//            circle.setRadius(location.getAccuracy());

            final LatLng startPosition = marker.getPosition();
            final LatLng finalPosition = location.getLatLng();
//            final double startRadius = circle.getRadius();
//            final double finalRadius = (double) location.getAccuracy();
            final float startRotation = marker.getRotation();
            final float finalRotation = location.getBearing();
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final Interpolator interpolator = new AccelerateDecelerateInterpolator();
            final float durationInMs = 1000;

            handler.post(new Runnable() {
                long elapsed;
                float t;
                float v;

                @Override
                public void run() {
                    elapsed = SystemClock.uptimeMillis() - start;
                    t = elapsed / durationInMs;
                    v = interpolator.getInterpolation(t);

                    LatLng currentPosition = new LatLng(
                            startPosition.latitude*(1-t)+finalPosition.latitude*t,
                            startPosition.longitude*(1-t)+finalPosition.longitude*t);
//                    double currentRadius = startRadius*(1-t)+finalRadius*t;

//                    float currentRotation;
//                    if(startRotation < finalRotation && finalRotation-startRotation<180){
//                        currentRotation = finalRotation *(1-t)+startRotation*t;
//                        marker.setRotation(currentRotation);
//                    } else if(startRotation > finalRotation){
//                        currentRotation = startRotation *(1-t)+finalRotation*t;
//                        marker.setRotation(currentRotation);
//                    }

                    marker.setPosition(currentPosition);
//                    circle.setCenter(currentPosition);
//                    circle.setRadius(finalRadius);

                    // Repeat till progress is complete.
                    if (t < 1) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    }
                }
            });


        }
        return this;
    }


    public MarkerUpdater setLocation(Position location) {
        this.location = location;
        return this;
    }

//    public int getOrientation() {
//        return orientation;
//    }
//
//    public MarkerUpdater setOrientation(int orientation) {
//        this.orientation = orientation;
//        return this;
//    }
    public void remove(){
        if(marker != null) marker.remove();
        if(circle != null) circle.remove();
    }

    public Marker getMarker(){
        return marker;
    }

    public MarkerUpdater setMarker(Marker marker){
        this.marker = marker;
        return this;
    }

}
