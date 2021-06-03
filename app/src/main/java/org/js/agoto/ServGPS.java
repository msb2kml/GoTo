package org.js.agoto;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class ServGPS extends Service {

    private final IBinder mBinder=new MyBinder();

    private Context context;
    private Handler mHandler=null;
    private Boolean listen=false;
    private LocationManager lm;

    public static boolean SERVICE_CONNECTED=false;
    public static int DONE=1000;
    public static int ERROR=666;
    public static int NWLOC=777;
    public static int OFF=100;
    public static int TEMP=200;
    public static int AVL=300;
    public static int ENBL=400;
    public static int DSBL=500;


    public class MyBinder extends Binder {
        ServGPS getService(){
            return ServGPS.this;
        }
    }

    @Override
    public void onCreate(){
        this.context=this;
        lm=(LocationManager)getSystemService(LOCATION_SERVICE);
        ServGPS.SERVICE_CONNECTED=true;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
        if (listen) {
            lm.removeUpdates(listener);

        }
        super.onDestroy();
    }

    public class GpsBinder extends Binder {
        public ServGPS getService() {
            return ServGPS.this;
        }
    }

    public void setHandler(Handler mHandler){
        this.mHandler=mHandler;
    }

    public void listenGps(){
        if (!listen){
            try {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,
                        0.0f,listener);
                listen=true;
            } catch (SecurityException e){
                if (mHandler!=null) mHandler.obtainMessage(ERROR,e).sendToTarget();
            }
        }
    }

    public void unListenGps(){
        if (listen){
            lm.removeUpdates(listener);
            listen=false;
        }
    }

    public void term(){
        if (mHandler!=null) mHandler.obtainMessage(DONE,null).sendToTarget();
    }

    void herePos(Location location){
        if (mHandler!=null) mHandler.obtainMessage(NWLOC,location).sendToTarget();
    }

    private LocationListener listener=new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            herePos(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (status== LocationProvider.OUT_OF_SERVICE){
                if (mHandler!=null) mHandler.obtainMessage(OFF).sendToTarget();
            } else if (status==LocationProvider.TEMPORARILY_UNAVAILABLE){
                if (mHandler!=null) mHandler.obtainMessage(TEMP).sendToTarget();
            } else if (status==LocationProvider.AVAILABLE){
                GpsStatus gstat;
                try {
                    gstat = lm.getGpsStatus(null);
                } catch (SecurityException e) {gstat=null;}
                if (mHandler!=null) mHandler.obtainMessage(AVL,gstat).sendToTarget();
            }

        }

        @Override
        public void onProviderEnabled(String provider) {
            if (mHandler!=null) mHandler.obtainMessage(ENBL).sendToTarget();
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (mHandler!=null) mHandler.obtainMessage(DSBL).sendToTarget();
        }
    };




}
