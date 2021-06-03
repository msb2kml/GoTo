package org.js.agoto;

import android.location.Location;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class readWpt {

    String pathWpt;
    String patrnLat="lat=\"(-?[0-9.-]+)\"";
    String patrnLon="lon=\"(-?[0-9.-]+)\"";
    String patrnNam="<name>(.+)</name>";
    String patrnEle="<ele>(.+)</ele>";
    String patrnTim="<time>(.+)</time>";
    Pattern pLat;
    Pattern pLon;
    Pattern pNam;
    Pattern pEle;
    Pattern pTim;

    public readWpt(String path){
        pathWpt=path;
        pLat=Pattern.compile(patrnLat);
        pLon=Pattern.compile(patrnLon);
        pNam=Pattern.compile(patrnNam);
        pEle=Pattern.compile(patrnEle);
        pTim=Pattern.compile(patrnTim);
    }

    public Map<String ,Location> readW(){
        boolean lkWpt=true;
        boolean lkEwpt=false;
        boolean lkCwpt=false;
        Integer len;
        Integer curnt;
        String expr="";
        Location loc=null;
        String name=null;
        Double alt=null;
        Map<String,Location> Points=new HashMap<>();
        File sg=new File(pathWpt);
        if (!sg.exists()) return Points;
        try {
            BufferedReader f=new BufferedReader(new FileReader(sg));
            String line="";
            while (line!=null) {
                line = f.readLine();
                if (line == null) continue;
                len = line.length();
                curnt = 0;
                while (curnt < len) {
                    if (lkWpt) {
                        Integer i = line.indexOf("<wpt", curnt);
                        if (i < 0) {
                            curnt = len;
                            continue;
                        }
                        expr = "";
                        curnt=i+4;
                        lkWpt=false;
                        lkEwpt=true;
                        loc=null;
                    } else if (lkEwpt){
                        Integer j=line.indexOf(">",curnt);
                        if (j<0){
                            expr=expr+line.substring(curnt);
                            curnt=len;
                        } else {
                            expr=expr+line.substring(curnt,j);
                            curnt=j+1;
                            loc=LatLon(expr);
                            lkEwpt=false;
                            if (loc!=null){
                                lkCwpt=true;
                                name=null;
                                expr="";
                                loc.setAltitude(0);
                            } else {
                                lkWpt=true;
                            }
                        }
                    } else if (lkCwpt){
                        Integer i=line.indexOf("</wpt>",curnt);
                        if (i<0) {
                            String tname=rName(line.substring(curnt));
                            if (tname!=null) name=tname;
                            alt=rEle(line.substring(curnt));
                            if (alt!=null) loc.setAltitude(alt);
                            curnt=len;
                            continue;
                        }
                        if (i>curnt) {
                            String tname=rName(line.substring(curnt,i));
                            if (tname!=null) name=tname;
                            alt=rEle(line.substring(curnt,i));
                            if (alt!=null) loc.setAltitude(alt);
                        }
                        lkCwpt=false;
                        lkWpt=true;
                        if (name!=null){
                            Points.put(name,loc);
                            name=null;
                        }
                        curnt=i+6;
                    }
                }
            }
            f.close();
        } catch (Exception e){ return Points; }
        return Points;


    }

    private Location LatLon(String expr){
        Location loc=new Location("");
        Double lat=null;
        Double lon=null;
        Matcher m;
        m=pLat.matcher(expr);
        if (m.find()){
            try {
                lat=Double.parseDouble(m.group(1));
            } catch (NumberFormatException e){
                return null;
            }
        } else return null;
        m=pLon.matcher(expr);
        if (m.find()){
            try {
                lon=Double.parseDouble(m.group(1));
            } catch (NumberFormatException e){
                return null;
            }
        } else return null;
        if (lat>180 || lat<-180 || lon>180 || lon<-180) return null;
        loc.setLongitude(lon);
        loc.setLatitude(lat);
        return loc;
    }

    private String rName(String expr){
        String name;
        Matcher m;
        m=pNam.matcher(expr);
        if (m.find()){
            name=m.group(1);
            name.trim();
            if (name.length()>0) return name;
            return null;
        }
        return null;
    }

    private Double rEle(String expr){
        Double alt;
        Matcher m;
        m=pEle.matcher(expr);
        if (m.find()){
            try {
                alt=Double.parseDouble(m.group(1));
                return alt;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Long rTime(String expr){
        Matcher m;
        Long tim;
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        m=pTim.matcher(expr);
        if (m.find()){
            try {
                Date date=sdf.parse(m.group(1));
                tim=date.getTime();
                return tim;
            } catch (Exception e){
                return null;
            }
        } else return null;
    }

}
