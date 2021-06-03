package org.js.agoto;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    Context context;
    Intent intentMap=null;
    IntentFilter filter=new IntentFilter("org.js.ACK");
    boolean noMap=true;
    boolean waitMap=false;
    boolean runningMap=false;
    boolean startLine=true;
    Double zoom=17.0;
    Map<String,Location> centerPos=new HashMap<>();
    String targetName=null;
    String filePath=null;
    String refDirectory=null;
    String Directory=null;
    Boolean singleLoc=false;
    Location target=null;
    boolean granted=false;
    boolean enabled=false;
    boolean tempGps=false;
    boolean binded=false;
    boolean waitOK=false;
    LocationManager lm;
    LocationProvider gps;
    String exPath= Environment.getExternalStorageDirectory().getAbsolutePath();
    String refPath=null;
    Map<String,Location> Wpt=new HashMap<>();
    String[] items;
    Location currentHere=null;
    Location prevHere=null;
    ArrayList<Location> trajectory=new ArrayList<>();
    Integer[] lineColor={ Color.rgb(0x00,0x00,0xFF),
                          Color.rgb(0x00,0x63,0xF3),
                          Color.rgb(0x00,0x92,0xDE),
                          Color.rgb(0x00,0xB7,0xC2),
                          Color.rgb(0x00,0xD6,0xA0),
                          Color.rgb(0x54,0xDD,0x74),
                          Color.rgb(0x85,0xE0,0x46),
                          Color.rgb(0xAD,0xE1,0x00),
                          Color.rgb(0xD9,0xC6,0x00),
                          Color.rgb(0xFF,0xA5,0x00),
                          Color.rgb(0xFF,0x78,0x00),
                          Color.rgb(0xFF,0x00,0x00)};
    int nColor=lineColor.length;
    Location arrowOrg;



    Button bOther;
    TextView tTrgName;
    TextView tLatTrg;
    TextView tLonTrg;
    TextView tAltTrg;
    TextView tLeft;
    TextView tRight;
    TextView tDist;
    TextView tAzim;
    TextView tBear;
    TextView tHeight;
    TextView tSpeed;
    TextView tLatHr;
    TextView tLonHr;
    TextView tAltHr;
    TextView tStatusHr;
    Button bStop;
    Button bRef;
    Button bMap;

    String formLat="Lat. %.6f";
    String formLon="Lon. %.6f";
    String formAlt="Alt. %.2f";
    final int codeSelFile=10;
    final int codeSelRef=12;
    final int REQUEST_READ_STORAGE=20;
    final int REQUEST_GPS_ACCESS=30;
    MyHandler mHandler;
    VHandler vHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=getApplicationContext();
        mHandler=new MyHandler(this);
        PackageManager Pm=getPackageManager();
        List<PackageInfo> allPack=Pm.getInstalledPackages(0);
        for (PackageInfo AI :allPack) {
            String zz=AI.packageName;
            if (zz.matches("org.js.Msb2Map")){
                intentMap=Pm.getLaunchIntentForPackage(zz);
                break;
            }
        }
        if (intentMap==null){
            Toast.makeText(context,"Missing Msb2Map application: no map.",Toast.LENGTH_LONG)
                    .show();
        }
        else noMap=false;
        getFields();
        fetchPref();
        Intent intent=getIntent();
        if (intent!=null) {
            target=(Location)intent.getParcelableExtra("Target");
            if (target==null) {
                Uri uri = intent.getData();
                if (uri != null) {
                    String sc = uri.getScheme();
                    if (sc.contentEquals("file")) {
                        filePath = uri.getPath();
                    } else if (sc.contentEquals("geo")) {
                        String geo = uri.getSchemeSpecificPart();
                        singleLoc = validGeo(geo);
                        if (!singleLoc) {
                            Toast.makeText(context, "Unknown " + geo, Toast.LENGTH_LONG);
                        }
                    }
                }
            } else {
                targetName=target.getExtras().getString("name",null);
                singleLoc=true;
            }
        }
        lm=(LocationManager)getSystemService(LOCATION_SERVICE);
        if (singleLoc) setTarget();
        else {
            boolean hasPermission=(ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED);
            if (!hasPermission){
                Toast.makeText(context,"This application need to read in "+
                        exPath+".",Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                         REQUEST_READ_STORAGE);
            } else if (filePath==null) selectFile();
            else selectWpt();
        }

    }

    void fetchPref(){
        SharedPreferences pref=context.getSharedPreferences(
                context.getString(R.string.PrefName),0);
        Directory=pref.getString("Directory",null);
        if (Directory!=null){
            File d=new File(Directory);
            if (!d.exists()) Directory=null;
        }
    }

    void putPref(){
        if (Directory!=null) {
            SharedPreferences pref = context.getSharedPreferences(
                    context.getString(R.string.PrefName), 0);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString("Directory", Directory);
            edit.apply();
        }
    }

    void getFields(){
        bOther=(Button) findViewById(R.id.other);
        tTrgName=(TextView) findViewById(R.id.TargetName);
        tLatTrg=(TextView) findViewById(R.id.latTarget);
        tLonTrg=(TextView) findViewById(R.id.lonTarget);
        tAltTrg=(TextView) findViewById(R.id.altTarget);
        tLeft=(TextView) findViewById(R.id.left);
        tRight=(TextView) findViewById(R.id.right);
        tLeft.setBackgroundColor(Color.GRAY);
        tRight.setBackgroundColor(Color.GRAY);
        tDist=(TextView) findViewById(R.id.dist);
        tAzim=(TextView) findViewById(R.id.azim);
        tBear=(TextView) findViewById(R.id.bearing);
        tHeight=(TextView) findViewById(R.id.height);
        tSpeed=(TextView) findViewById(R.id.speed);
        tLatHr=(TextView) findViewById(R.id.latHere);
        tLonHr=(TextView) findViewById(R.id.lonHere);
        tAltHr=(TextView) findViewById(R.id.altHere);
        tStatusHr=(TextView) findViewById(R.id.hereStatus);
        bStop=(Button) findViewById(R.id.bStop);
        bRef=(Button) findViewById(R.id.bRef);
        bMap=(Button) findViewById(R.id.bMap);
        bStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpsService.term();
//                stop();
            }
        });
        bOther.setEnabled(false);
        bOther.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                other();
            }
        });
        bRef.setEnabled(false);
        bMap.setEnabled(false);
        bMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (currentHere!=null){
                    launchMap(target);
//                }
            }
        });
        bRef.setText("-none");
        bRef.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRef();
            }
        });
    }

    void stop(){
        closeServ();
        if (tempGps) StopGps();
        putPref();
        finish();
    }

    void selectFile(){
        Intent intent=new Intent(MainActivity.this, Selector.class);
        if (Directory!=null) intent.putExtra("CurrentDir",Directory);
        intent.putExtra("WithDir",false);
        intent.putExtra("Mask","(?i).+\\.gpx");
        intent.putExtra("Title","Look for target in GPX file?  ");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent,codeSelFile);
    }

    void selectWpt(){
        Integer sz;
        readWpt rW=new readWpt(filePath);
        Wpt=rW.readW();
        if (Wpt.isEmpty()){
            Toast.makeText(context,"No Waypoint in this file!",Toast.LENGTH_LONG).show();
            selectFile();
        } else {
            File f=new File(filePath);
            Directory=f.getParent();
            SortedSet<String> keys=new TreeSet<>();
            keys.addAll(Wpt.keySet());
            sz=keys.size();
            items=new String[sz];
            String[] desc=new String[sz];
            final Iterator<String> itr=((TreeSet<String>) keys).descendingIterator();
            String here;
            for (int i=0;i<sz;i++){
                here=itr.next();
                items[i]=here;
                desc[i]=here+": ";
                desc[i]+=String.format(Locale.ENGLISH,"lat=%.6f ",
                        Wpt.get(here).getLatitude());
                desc[i]+=String.format(Locale.ENGLISH,"lon=%.6f ",
                        Wpt.get(here).getLongitude());
                desc[i]+=String.format(Locale.ENGLISH,"alt=%.1f",
                        Wpt.get(here).getAltitude());
            }
            AlertDialog.Builder build=new AlertDialog.Builder(this);
            build.setTitle("Select a target location");
            build.setItems(desc, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String here=items[which];
                    targetName=here;
                    target=Wpt.get(here);
                    Bundle bundle=new Bundle();
                    target.setExtras(bundle);
                    target.getExtras().putString("name",targetName);
                    setTarget();
                }
            })
                    .setNegativeButton("Other file", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectFile();
                        }
                    });
            build.show();
        }

    }

    void selectRef(){
        Intent intent=new Intent(MainActivity.this,Selector.class);
        if (refDirectory==null) refDirectory=Directory;
        if (refDirectory!=null) intent.putExtra("CurrentDir",refDirectory);
        intent.putExtra("WithDir",false);
        intent.putExtra("Mask","(?i).+\\.gpx");
        intent.putExtra("Title","Reference GPX?   ");
        if (refPath!=null) intent.putExtra("Previous",refPath);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent,codeSelRef);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case codeSelFile:
                if (resultCode==RESULT_OK){
                    filePath=data.getStringExtra("Path");
                    selectWpt();
                } else finish();
                break;
            case codeSelRef:
                if (resultCode==RESULT_OK){
                    refPath=data.getStringExtra("Path");
                    if (refPath==null || refPath.isEmpty()) refPath=null;
                } else refPath=null;
                if (refPath==null) bRef.setText("-none-");
                else {
                    File f=new File(refPath);
                    refDirectory=f.getParent();
                    bRef.setText(f.getName());
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode==REQUEST_READ_STORAGE){
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                if (filePath==null) selectFile();
                else selectWpt();
            } else finish();
        } else if (requestCode==REQUEST_GPS_ACCESS){
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                checkPerm();
            } else notAv();
        }
    }

    void setTarget(){
        if (targetName==null) tTrgName.setText("Target");
        else tTrgName.setText(targetName);
        tLatTrg.setText(String.format(Locale.ENGLISH,formLat,target.getLatitude()));
        tLonTrg.setText(String.format(Locale.ENGLISH,formLon,target.getLongitude()));
        if (target.hasAltitude()) tAltTrg.setText(String.format(
                Locale.ENGLISH,formAlt,target.getAltitude()));
        else tAltTrg.setText("Alt. x");
        bOther.setEnabled(true);
        checkPerm();
    }

    void other(){
        if (target==null) return;
        String geo=String.format(Locale.ENGLISH,"geo:0,0?q=%.6f,%.6f",
                target.getLatitude(),target.getLongitude());
        if (targetName!=null) geo+="("+targetName+")";
        Uri uriGeo=Uri.parse(geo);
        Intent mapIntent=new Intent(Intent.ACTION_VIEW,uriGeo);
        startActivity(mapIntent);
        closeServ();
        putPref();
        finish();
    }

    Boolean validGeo(String geo){
        String patrnLatLon="(-?[0-9.]+)";
        String patrnAndro="q=(-?[0-9.]+),(-?[0-9.]+)\\(([^(]+)\\)";
        Pattern pLatLon=Pattern.compile(patrnLatLon);
        Pattern pAndro=Pattern.compile(patrnAndro);
        Matcher m;
        Location where=null;
        String name=null;
        String[] part=geo.split("\\?");
        String LatLon;
        if (part.length<1) LatLon=geo;
        else LatLon=part[0];
        if (!LatLon.contentEquals("0,0")){
            where=new Location("");
            name=LatLon;
            String[] coord=LatLon.split(",");
            if (coord.length<2) return false;
            m=pLatLon.matcher(coord[0]);
            if (!m.find()) return false;
            try {
                where.setLatitude(Double.parseDouble(m.group(1)));
            } catch (NumberFormatException e){
                return false;
            }
            m=pLatLon.matcher(coord[1]);
            if (!m.find()) return false;
            try {
                where.setLongitude(Double.parseDouble(m.group(1)));
            } catch (NumberFormatException e){
                return false;
            }
            m=pLatLon.matcher(coord[1]);
            if (!m.find()) return false;
            try {
                where.setLongitude(Double.parseDouble(m.group(1)));
            } catch (NumberFormatException e){
                return false;
            }
            if (coord.length>2){
                m=pLatLon.matcher(coord[2]);
                if (!m.find()) return false;
                try {
                    where.setAltitude(Double.parseDouble(m.group(1)));
                } catch (NumberFormatException e){
                    return false;
                }
            }
        }
        if (part.length>1){
            m=pAndro.matcher(part[1]);
            if (m.find()){
                int n=m.groupCount();
                if (n==3){
                    name=m.group(3);
                } else if (name==null) name=part[1];
                Double lat=null;
                Double lon=null;
                String s=m.group(1);
                try {
                    lat=Double.parseDouble(s);
                } catch (NumberFormatException e){
                    lat=null;
                }
                s=m.group(2);
                try {
                    lon=Double.parseDouble(s);
                } catch (NumberFormatException e){
                    lon=null;
                }
                if (lat!=null && lon!=null){
                    if (where==null) where=new Location("");
                    where.setLatitude(lat);
                    where.setLongitude(lon);
                }
            }
        }
        if (where==null) return false;
        Bundle bundle=new Bundle();
        where.setExtras(bundle);
        where.getExtras().putString("name",name);
        centerPos.put(name,where);
        targetName=name;
        target=where;
        return true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (waitOK) checkPerm();
        if (runningMap) fromMap();
    }

    void checkPerm() {
        granted = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            gps=lm.getProvider(LocationManager.GPS_PROVIDER);
            if (gps.equals(null)){
                tStatusHr.setText("No GPS!");
                Toast.makeText(context,"GPS is not available!",Toast.LENGTH_LONG).show();
                notAv();
            }
            enabled=lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } else {
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setTitle("GPS permission for "+getString(R.string.app_name));
            builder.setMessage("Please allow GPS access")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            notAv();
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        REQUEST_GPS_ACCESS);
                        }
                    });
            builder.show();
            return;
        }
        if (enabled) {
            waitOK=false;
            tStatusHr.setText("GPS location enabled; waiting for fix");
            follow2();
        }
        else {
            tStatusHr.setText("GPS location not enabled");
            waitOK=true;
            tempGps=true;
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setTitle("GPS for "+getString(R.string.app_name))
                    .setMessage("Please enable GPS location")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent;
                            intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                notAv();
                            }
                        });
            builder.show();
        }
    }

    void StopGps(){
        Intent intent=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
        tempGps=false;
    }

    void follow2(){
        startService(ServGPS.class,gpsConnection,null);
        if (!noMap) {
            bMap.setEnabled(true);
            bRef.setEnabled(true);
        }
        waitOK=false;
    }

    void notAv(){
        closeServ();
        Toast.makeText(context,"Unusable without access to GPS",Toast.LENGTH_LONG).show();
        finish();
    }

    Float DifAngl(Float from, Float to){
        Float dif=to-from;
        dif=(dif+180.0f)%360.0f-180.0f;
        return dif;
    }

    void herePos(Location location){
        Float dist=null;
        Float prec=null;
        Float azim=null;
        Float bear=null;
        Integer nSat= location.getExtras().getInt("satellites",-1);
        location.getExtras().putString("name","Here");
        if (location.hasAccuracy()){
            prec=location.getAccuracy();
            tStatusHr.setText(String.format(Locale.ENGLISH,"Error %.1f",prec));
        } else if (nSat>0){
            tStatusHr.setText("nb. sat.  "+nSat.toString());
        } else tStatusHr.setText("-");
        if (prevHere!=null){
            dist=location.distanceTo(prevHere);
            if (dist<5.0) return;
        }
        currentHere=location;
        trajectory.add(currentHere);
        tLatHr.setText(String.format(Locale.ENGLISH,formLat, location.getLatitude()));
        tLonHr.setText(String.format(Locale.ENGLISH,formLon,location.getLongitude()));
        if (location.hasAltitude())
            tAltHr.setText(String.format(Locale.ENGLISH,formAlt,location.getAltitude()));
        else
            tAltHr.setText(" - ");
        if (prec==null) prec=10.0f;
        Float distTrg=currentHere.distanceTo(target);
        tDist.setText(String.format(Locale.ENGLISH,"Distance %.1f",distTrg));
        if (distTrg<prec) tDist.setTextColor(Color.RED);
        else tDist.setTextColor(Color.BLACK);
        if (target.hasAltitude() && currentHere.hasAltitude()){
            Double height=target.getAltitude()-currentHere.getAltitude();
            tHeight.setText(String.format(Locale.ENGLISH,"Height %.1f",height));
        } else tHeight.setText("-");
        azim=currentHere.bearingTo(target);
        tAzim.setText("Azimuth "+DegCompass(azim));
        if (prevHere!=null){
            Float distPrev=prevHere.distanceTo(target);
            Long timeCurr=currentHere.getTime()-prevHere.getTime();
            Float min=timeCurr.floatValue()/60000f;
            Float speed=(distPrev-distTrg)/min;
            tSpeed.setText(String.format(Locale.ENGLISH,"Closing rate %.1f m/min",speed));
            if (speed>0.0) tSpeed.setTextColor(Color.BLACK);
            else tSpeed.setTextColor(Color.RED);
            bear=prevHere.bearingTo(currentHere);
            tBear.setText("Bearing "+DegCompass(bear));
            Float difBear=DifAngl(bear,azim);
            if (Math.abs(difBear)<12.5){
                tRight.setBackgroundColor(Color.GRAY);
                tLeft.setBackgroundColor(Color.GRAY);
            } else if (distTrg>prec){
                if (difBear>0.0){
                    tRight.setBackgroundColor(Color.RED);
                    tLeft.setBackgroundColor(Color.BLUE);
                } else {
                    tRight.setBackgroundColor(Color.BLUE);
                    tLeft.setBackgroundColor(Color.RED);
                }
            } else {
                tRight.setBackgroundColor(Color.RED);
                tLeft.setBackgroundColor(Color.RED);
            }
        } else {
            tSpeed.setText("-");
            tBear.setText("-");
            tRight.setBackgroundColor(Color.GRAY);
            tLeft.setBackgroundColor(Color.GRAY);
        }

        prevHere=currentHere;
    }

    void stateGps(int what, Object detail){
        if (what==ServGPS.OFF){
            tStatusHr.setText("GPS out of service");
        } else if (what==ServGPS.TEMP){
            tStatusHr.setText("GPS temporary unavailable");
        } else if (what==ServGPS.AVL){
            tStatusHr.setText("GPS available");
        } else if (what==ServGPS.ENBL){
            tStatusHr.setText("GPS enabled");
        } else if (what==ServGPS.DSBL){
            tStatusHr.setText("GPS disabled");
        }
    }

    String DegCompass(Float azim){
        String[] directions={ "N","NNE","NE","ENE","E","ESE","SE","SSE",
                              "S","SSW","SW","WSW","W","WNW","NW","NNW","N"};
        if (azim<0f && azim>-180f) azim=360.0f+azim;
        if (azim>360.0f || azim<-180.0f) return "?";
        int ind=(int) (Math.floor((azim+11.25f)%360.0f)/22.5f);
        return directions[ind];
    }

    int colorz(Float val){
        Float valBlue=200.0f;
        Float valRed=0.0f;
        if (val==null) return Color.BLACK;
        float norm=(val-valBlue)/(valRed-valBlue);
        int v=(int)Math.round(norm*nColor);
        v=Math.max(1,Math.min(nColor,v))-1;
        return lineColor[v];
    }

    void launchMap(Location centerLoc){
        Intent nt=(Intent) intentMap.clone();
        nt.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        nt.putExtra("CALLER",context.getString(R.string.app_name));
        nt.putExtra("CENTER",centerLoc);
        nt.putExtra("StartGPS",false);
        nt.putExtra("Tail",true);
        if (zoom!=null) nt.putExtra("ZOOM",zoom);
        zoom=null;
        runningMap=true;
        startActivity(nt);
        waitMap=true;
        registerReceiver(mReceiver,filter);
        return;
    }

    void fromMap(){
        if (runningMap){
            runningMap=false;
        }
    }

    private final BroadcastReceiver mReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!waitMap) return;
            String origin=intent.getStringExtra("NAME");
            int vc=intent.getIntExtra("VERSION",0);
            unregisterReceiver(mReceiver);
            ckVcMap(vc);
            waitMap=false;
//            if (refPath ==null) {
//                dispatch(2);
//            } else {
//                setRef();
//            }
            dispatch();
        }
    };

    void dispatch() {
        if (refPath != null) {
            vHandler = new VHandler(MainActivity.this);
            Vtrack vtr = new Vtrack();
            vtr.vtrk(context, vHandler, refPath);
        } else beginTrk();
    }

    void beginTrk(){
        Location p;
        Float far;
        startLine=true;
        disWpt(target,null,0);
        arrowOrg=null;
        for (int i=0;i<trajectory.size();i++){
            p=trajectory.get(i);
            far=target.distanceTo(p);
            dispTrk(p,far.toString(),colorz(far),startLine,true);
            startLine=false;
        }
    }

    void ckVcMap(int vc){
        if (vc<17){
            Toast.makeText(context,"Msb2Map revision should be at least 1.7",
                    Toast.LENGTH_LONG).show();
        }
    }

    void disWpt(Location loc, String infoBubble,int typ){
        Intent nt = new Intent();
        nt.setAction("org.js.LOC");
        nt.putExtra("WPT",loc);
        String namWpt=loc.getExtras().getString("name", "?");
        if (infoBubble==null) {
            if (loc.hasAltitude()) {
                namWpt = String.format(Locale.ENGLISH, "%s (%.1f)",
                        loc.getExtras().getString("name", "?"), loc.getAltitude());
            }
            nt.putExtra("BUBBLE",namWpt);
        } else {
            nt.putExtra("BUBBLE",infoBubble);
        }
        nt.putExtra("WPT_NAME", namWpt);
        nt.putExtra("TYPE",typ);
        sendBroadcast(nt);
    }

    void dispTrk(Location loc, String bubbleMap, int color, Boolean startLine, Boolean actTail){
        Intent nt=new Intent();
        nt.setAction("org.js.LOC");
        nt.putExtra("LOC",loc);
        nt.putExtra("COLOR",color);
        nt.putExtra("BUBBLE",bubbleMap);
        if (actTail) {
            if (arrowOrg != null) {
                float dist = arrowOrg.distanceTo(loc);
                if (dist > 10.0) {
                    float bearing = -arrowOrg.bearingTo(loc);
                    nt.putExtra("ORIENT", bearing);
                    arrowOrg = loc;
                }
            } else arrowOrg = loc;
        }
        if (startLine){
            nt.putExtra("ORIENT",0.0f);
            nt.putExtra("START",startLine);
            nt.putExtra("Tail",actTail);
        }
        sendBroadcast(nt);
    }

    private ServGPS gpsService;

    private final ServiceConnection gpsConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gpsService=((ServGPS.MyBinder) service).getService();
            gpsService.setHandler(mHandler);
            gpsService.listenGps();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            gpsService=null;
        }
    };

    private void startService(Class<?> service, ServiceConnection serviceConnection,
                              Bundle extras){
        if (!ServGPS.SERVICE_CONNECTED){
            Intent startSer=new Intent(context,service);
            context.startService(startSer);
        }
        Intent bindingIntent=new Intent(context,service);
        context.bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        binded=true;
    }

    void closeServ(){
        if (binded) {
            context.unbindService(gpsConnection);
            gpsService.stopSelf();
        }
    }

    static class MyHandler extends Handler{

        final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity){
            mActivity=new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg){
            if (msg.what==ServGPS.DONE) {
                mActivity.get().stop();
            } else if (msg.what==ServGPS.NWLOC){
                Location loc=(Location) msg.obj;
                mActivity.get().herePos(loc);
            } else {
                mActivity.get().stateGps(msg.what,msg.obj);
            }
        }
    }

    private static class VHandler extends Handler {
        public final WeakReference<MainActivity> mActivity;

        public VHandler(MainActivity activity){
            mActivity=new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg){
            int code=msg.what;
            switch (code) {
                case 0:
                    mActivity.get().beginTrk();
                    break;
            }
        }
    }

}
