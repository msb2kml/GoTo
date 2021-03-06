package org.js.agoto;

import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class navDir {

    String exPath= Environment.getExternalStorageDirectory().getAbsolutePath();
    String rmvPath=null;
    private String curDir=exPath;
    private Pattern patrn=null;
    private Boolean writeable=false;
    Boolean wthSaf=(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT);

    public navDir(String exP, String rmvP){
        exPath=exP;
        rmvPath=rmvP;
    }

    public void setCurDir(String dir){
        curDir=dir;
    }

    public String getDir(){
        return curDir;
    }

    public Boolean getWriteable() {
        return writeable;
    }

    public String upDir(){
        if (curDir.contentEquals(exPath) || (rmvPath!=null && curDir.contentEquals(rmvPath))){
            curDir="/";
        } else {
            File dir = new File(curDir);
            curDir = dir.getParent();
        }
        return curDir;
    }

    public String dnDir(String down){
        if (!curDir.equals("/") && !down.startsWith("/")) {
            curDir+="/";
        } else curDir="";
        if (down.endsWith("/")){
            curDir+=down.substring(0,down.length()-1);
        } else curDir+=down;
        return curDir;
    }

    public Boolean setMask(String m){
        if (m==null){
            patrn=null;
            return true;
        }
        try {
            patrn=Pattern.compile(m);
            return true;
        } catch (PatternSyntaxException e) {
            patrn=null;
            return false;
        }
    }

    public String[] get() {
        if (curDir == null) curDir = "/";
        File dir = new File(curDir);
        if (curDir.equals("/") || !dir.exists() || !dir.isDirectory()) {
            if (rmvPath != null) {
                ArrayList<String> directories = new ArrayList<>();
                directories.add("../ (up)");
                directories.add(rmvPath + "/");
                directories.add(exPath + "/");
                String[] ar = directories.toArray(new String[0]);
                return ar;
            } else {
                curDir = exPath;
                dir = new File(curDir);
            }
        }
        if (wthSaf && rmvPath != null && curDir.startsWith(rmvPath)) writeable = false;
        else writeable = dir.canWrite();
        ArrayList<String> directories = new ArrayList<String>();
        ArrayList<String> files = new ArrayList<String>();
        directories.add("../   (up)");
        String s[] = dir.list();
        if (s != null && s.length > 0) {
            Arrays.sort(s);
            for (int i = 0; i < s.length; i++) {
                File f = new File(curDir + "/" + s[i]);
                if (f.isDirectory()) {
                    if (f.canRead()) directories.add(s[i] + "/");
                } else {
                    if (patrn == null) files.add(s[i]);
                    else {
                        if (patrn.matcher(s[i]).matches()) files.add(s[i]);

                    }
                }
            }
        }
        ArrayList<String> all = new ArrayList<String>();
        all.addAll(directories);
        all.addAll(files);
        String[] ar = all.toArray(new String[0]);
        return ar;
    }

}
