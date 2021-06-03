package org.js.agoto;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.widget.Toast;

public class Vtrack {

    public static Handler topCom;
    public Context context;
    public String refPath;
    Track track=null;
    Location dispLoc=null;
    Location startLoc=null;
    Track.enttGpx curEntity= Track.enttGpx.ALIEN;
    String curEntName=null;
    Boolean setStart=true;
    int HalfMagenta= Color.argb(0x80,0xFF,0x00,0xFF);

    class BgThread extends Thread{
        private Thread t;
        private String threadName;

        BgThread(String name){
            threadName=name;
        }

        public void run(){
            dispBg();
        }

        public void start(){
            if (t==null){
                t=new Thread(this,threadName);
                t.start();
            }
        }
    }

    public void vtrk(Context context, Handler topCom, String bgPath){
        this.context=context;
        this.topCom=topCom;
        this.refPath =bgPath;
        BgThread t1=new BgThread("Thread BG");
        t1.start();
    }

    void dispBg(){
        Long sizeRef=null;
        track=new Track();
        sizeRef=track.open(refPath);
        dispLoc=track.nextPt();
        if (dispLoc==null){
            Toast.makeText(context,"No valid item in "+ refPath,Toast.LENGTH_LONG).show();
            track.close();
            sendSig();
            return;
        }
        while (dispLoc!=null){
            if (noTail()){
                track.close();
                sendSig();
                return;
            }
        }
        track.close();
        sendSig();
    }

    Boolean noTail() {
        if (dispLoc == null) return true;
        Track.enttGpx entity = (Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
        switch (entity) {
            case WPT:
                curEntity = entity;
            case RTEWPT:
            case TRKWPT:
                break;
            case TRK:
                curEntName = "Track " + dispLoc.getExtras().getString("name", null);
                setStart = true;
                startLoc = null;
                curEntity = entity;
                dispLoc = track.nextPt();
                return false;
            case RTE:
                curEntName = "Route " + dispLoc.getExtras().getString("name", null);
                setStart = true;
                startLoc = null;
                curEntity = entity;
                dispLoc = track.nextPt();
                return false;
            case ALIEN:
                track.close();
                Toast.makeText(context, "Sorry, " + refPath + " is not compatible.",
                        Toast.LENGTH_LONG).show();
                return true;
        }
        while (true) {
            entity = (Track.enttGpx) dispLoc.getExtras().getSerializable("ENTITY");
            switch (curEntity) {
                case WPT:
                    if (entity != curEntity) return false;
                    dispWpt(dispLoc, dispLoc.getExtras().getString("name", "?"),
                            2);
                    break;
                case TRKWPT:
                case RTEWPT:
                    if (entity != curEntity) {
                        return false;
                    }
                    dispTrk(dispLoc, " - ", HalfMagenta, false, false);
                    break;
                case RTE:
                    if (entity != Track.enttGpx.RTEWPT) return false;
                    dispWpt(dispLoc, curEntName, 1);
                    dispTrk(dispLoc, " - ", HalfMagenta, true, false);
                    curEntity = entity;
                    break;
                case TRK:
                    if (entity != Track.enttGpx.TRKWPT) return false;
                    dispWpt(dispLoc, curEntName, 1);
                    dispTrk(dispLoc, " - ", HalfMagenta, true, false);
                    curEntity = entity;
                    break;
            }
            dispLoc = track.nextPt();
            if (dispLoc == null) return false;
        }
    }

    void sendSig(){
        topCom.obtainMessage(0,(Object) true).sendToTarget();
    }

    void dispWpt(Location loc, String namWpt,int typ){
        Intent nt = new Intent();
        nt.setAction("org.js.LOC");
        nt.putExtra("WPT",loc);
        nt.putExtra("TYPE",typ);
        nt.putExtra("WPT_NAME",namWpt);
        context.sendBroadcast(nt);
    }

    void dispTrk(Location loc, String bubble, int color, Boolean startLine, Boolean actTail){
        Intent nt = new Intent();
        nt.setAction("org.js.LOC");
        nt.putExtra("LOC",loc);
        nt.putExtra("COLOR",color);
        nt.putExtra("BUBBLE",bubble);
        if (startLine){
            nt.putExtra("START",startLine);
            nt.putExtra("Tail",actTail);
        }
        context.sendBroadcast(nt);
    }
}
