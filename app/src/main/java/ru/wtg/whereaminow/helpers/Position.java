package ru.wtg.whereaminow.helpers;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tujger on 7/16/16.
 */
public class Position extends Location {

    public static final Creator<Location> CREATOR = null;

    private String id;
    private int strokeColor;
    private int fillColor;
    private int markerColor;
    private int markerWidth;

    public Position(Location l) {
        super(l);
    }

    public LatLng getLatLng(){
        return new LatLng(getLatitude(), getLongitude());
    }

    @Override
    public String toString() {
        return "Position from "+getProvider()+": alt="+getAltitude()+", lat="+getLatitude()+", lng="+getLongitude()+", bearing="+getBearing()+", accuracy="+getAccuracy()+", time="+getTime();
    }

    public Position apply(Position location){
        System.out.println("POSITION:"+location.getLatitude()+":"+location.getLongitude());
        setLatitude(location.getLatitude());
        setBearing(location.getBearing());
        setLongitude(location.getLongitude());
        setAccuracy(location.getAccuracy());
        setAltitude(location.getAltitude());
        setProvider(location.getProvider());
        setSpeed(location.getSpeed());
        setTime(location.getTime());
        return this;
    }

    public JSONObject toJson(){
        JSONObject o = new JSONObject();
        try {
            o.put("accuracy",getAccuracy());
            o.put("altitude",getAltitude());
            o.put("bearing",getBearing());
            o.put("latitude",getLatitude());
            o.put("longitude",getLongitude());
            o.put("provider",getProvider());
            o.put("speed",getSpeed());
            o.put("time",getTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return o;
    }

    public Position fromJsonString(String jsonString){
        try {
            JSONObject o = new JSONObject(jsonString);
            fromJson(o);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public Position fromJson(JSONObject json){
        try {
            setAccuracy((float)json.getDouble("accuracy"));
            setAltitude(json.getDouble("altitude"));
            setBearing((float)json.getDouble("bearing"));
            setLatitude(json.getDouble("latitude"));
            setLongitude(json.getDouble("longitude"));
            setProvider(json.getString("provider"));
            setSpeed((float)json.getDouble("speed"));
            setTime(json.getLong("time"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public String toJsonString(){
        return toJson().toString();
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public Position setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
        return this;
    }

    public int getFillColor() {
        return fillColor;
    }

    public Position setFillColor(int fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    public int getMarkerColor() {
        return markerColor;
    }

    public Position setMarkerColor(int markerColor) {
        this.markerColor = markerColor;
        return this;
    }

    public String getId() {
        return id;
    }

    public Position setId(String id) {
        this.id = id;
        return this;
    }

    public Position setMarkerWidth(int markerWidth) {
        this.markerWidth = markerWidth;
        return this;
    }

    public int getMarkerWidth() {
        return markerWidth;
    }
}
