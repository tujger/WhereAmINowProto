package ru.wtg.whereaminow.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.preference.PreferenceManager;

/**
 * Created by tujger on 7/14/16.
 */
public class CurrentState {

    public final static int ORIENTATION_NORTH = 0;
    public final static int ORIENTATION_DIRECTION = 1;
    public final static int ORIENTATION_PERSPECTIVE = 2;
    public final static int ORIENTATION_STAY = 3;
    public final static int ORIENTATION_USER = 10;

    public final static int SCREEN_MODE_MINE = 11;
    public final static int SCREEN_MODE_FRIEND = 12;
    public final static int SCREEN_SHOW_US = 13;
    public final static int SCREEN_SPLIT_FRIEND_ABOVE = 14;
    public final static int SCREEN_SPLIT_FRIEND_BELOW = 15;
    public final static int SCREEN_STREET_VIEW = 16;
    public final static int SCREEN_SPLIT_STREET_VIEW_FRIEND = 17;

    public final static String ID_MY = "my";
    public final static String ID_FRIEND = "friend";

    private final static String PREF_TRACKING_ACTIVE = "trackingActive";
    private final static String PREF_FRIEND_ACTIVE = "friendActive";
    private final static String PREF_SCREEN_MODE = "screenMode";
    private final static String PREF_MY_ZOOM = "myZoom";
    private final static String PREF_FRIEND_ZOOM = "friendZoom";
    private final static String PREF_MY_ORIENTATION = "myScreenOrientation";
    private final static String PREF_FRIEND_ORIENTATION = "friendScreenOrientation";
    private final static String PREF_MY_POSITION = "myPosition";
    private final static String PREF_FRIEND_POSITION = "friendPosition";

    private static volatile CurrentState instance = null;
    private Context context;

    private CurrentState() {

    }

    public User getUser(String id){
        if(ID_MY.equals(id)){
            return my;
        } else if(ID_FRIEND.equals(id)){
            return friend;
        } else {
            try {
                throw new Exception("User ID wrong or not defined: '"+id+"'.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public User getUserByMarkerId(String id){

        if(my.hasMarker() && id.equals(my.getMarker().getMarker().getId())){
            return my;
        } else if(friend.hasMarker() && id.equals(friend.getMarker().getMarker().getId())) {
            return friend;
        } else {
            try {
                throw new Exception("Unknown marker id: '"+id+"'.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    public static CurrentState getInstance() {
        if (instance == null) {
            synchronized (CurrentState.class){
                if (instance == null) {
                    instance = new CurrentState();
                }
            }
        }
        return instance ;
    }

    public User my = new User().setId(ID_MY);
    public User friend = new User().setId(ID_FRIEND);

    private String info;
    private boolean trackingActive;
    private boolean friendActive;
    private int screenMode;
    private String token;

    public CurrentState save(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREF_TRACKING_ACTIVE,isTrackingActive());
        editor.putBoolean(PREF_FRIEND_ACTIVE,isFriendActive());
        editor.putInt(PREF_SCREEN_MODE,getScreenMode());

        editor.putInt(PREF_MY_ORIENTATION,my.getOrientation());
        editor.putFloat(PREF_MY_ZOOM, my.getZoom());
        editor.putString(PREF_MY_POSITION, my.getPosition().toJsonString());

        editor.putInt(PREF_FRIEND_ORIENTATION,friend.getOrientation());
        editor.putFloat(PREF_FRIEND_ZOOM, friend.getZoom());
        editor.putString(PREF_FRIEND_POSITION, friend.getPosition().toJsonString());

        editor.apply();
        return this;
    }

    public CurrentState load(){

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        setTrackingActive(sp.getBoolean(PREF_TRACKING_ACTIVE,false));
        setFriendActive(sp.getBoolean(PREF_FRIEND_ACTIVE,false));
        setScreenMode(sp.getInt(PREF_SCREEN_MODE,SCREEN_MODE_MINE));
        my.setOrientation(sp.getInt(PREF_MY_ORIENTATION,ORIENTATION_NORTH));
        friend.setOrientation(sp.getInt(PREF_FRIEND_ORIENTATION,ORIENTATION_NORTH));
        my.setZoom(sp.getFloat(PREF_MY_ZOOM,17));
        friend.setZoom(sp.getFloat(PREF_FRIEND_ZOOM,17));

        if(my.getPosition() == null){
            my.setPosition(new Location("gps"));
            my.getPosition().fromJsonString(sp.getString(PREF_MY_POSITION,"{}"));
        }

        if(friend.getPosition() == null){
            friend.setPosition(new Location("gps"));
            friend.getPosition().fromJsonString(sp.getString(PREF_FRIEND_POSITION,"{}"));
        }
        return this;
    }

    public boolean isTrackingActive() {
        return trackingActive;
    }

    public CurrentState setTrackingActive(boolean trackingActive) {
        this.trackingActive = trackingActive;
        return this;
    }

    public boolean isFriendActive() {
        return friendActive;
    }

    public CurrentState setFriendActive(boolean friendActive) {
        this.friendActive = friendActive;
        return this;
    }

    public String getInfo() {
        return info;
    }

    public CurrentState setInfo(String info) {
        this.info = info;
        return this;
    }

    public Context getContext() {
        return context;
    }

    public CurrentState setContext(Context context) {
        this.context = context;
        return this;
    }


    public int getScreenMode() {
        return screenMode;
    }

    public CurrentState setScreenMode(int screenMode) {
        this.screenMode = screenMode;
        return this;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public class User {
        private Position position;
        private float zoom;
        private int orientation;
        private String id;
        private MarkerUpdater marker;

        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            if(position == null) {
                this.position = null;
            } else if(this.position == null) {
                this.position = position;
            } else {
                this.position.apply(position);
            }
            if(this.position != null) {
                switch (id) {
                    case ID_MY:
                        this.position.setId(ID_MY).setStrokeColor(Color.CYAN).setFillColor(0x2000ffff).setMarkerColor(0x880000FF);
                        break;
                    case ID_FRIEND:
                        this.position.setId(ID_FRIEND).setStrokeColor(Color.GREEN).setFillColor(0x0a00ff00).setMarkerColor(0x8800FF00);
                        break;
                }
            }
        }

        public void setPosition(Location location){
            setPosition(new Position(location));
        }

        public float getZoom() {
            return zoom;
        }

        public void setZoom(float zoom) {
            this.zoom = zoom;
        }

        public int getOrientation() {
            return orientation;
        }

        public void setOrientation(int orientation) {
            this.orientation = orientation;
        }

        public String getId() {
            return id;
        }

        public User setId(String id) {
            this.id = id;
            return this;
        }

        public MarkerUpdater getMarker() {
            return marker;
        }

        public int getMarkerWidth(){
            return position.getMarkerWidth();
        }

        public User setMarker(MarkerUpdater marker) {
            this.marker = marker;
            return this;
        }

        public boolean hasMarker(){
            return this.marker != null;
        }
    }

}