package com.vivolite.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small ADW appfilter reader. It keeps names only; pack drawables are decoded on demand. */
final class IconPackManager {
    static final class Pack {
        final String packageName,label;
        Pack(String packageName,String label){this.packageName=packageName;this.label=label;}
    }

    private final Context context;
    private String activePackage="";
    private Resources activeResources;
    private final Map<String,String> mappings=new HashMap<>();

    IconPackManager(Context context){this.context=context.getApplicationContext();}

    List<Pack> discover() {
        PackageManager pm=context.getPackageManager();
        Map<String,Pack> packs=new LinkedHashMap<>();
        String[] actions={"org.adw.ActivityStarter.THEMES","com.novalauncher.THEME"};
        for(String action:actions) {
            for(ResolveInfo result:pm.queryIntentActivities(new Intent(action),0)) {
                if(result.activityInfo==null)continue;
                String pkg=result.activityInfo.packageName;
                try {
                    CharSequence label=pm.getApplicationLabel(pm.getApplicationInfo(pkg,0));
                    packs.put(pkg,new Pack(pkg,label==null?pkg:label.toString()));
                } catch(PackageManager.NameNotFoundException ignored) { }
            }
        }
        ArrayList<Pack> result=new ArrayList<>(packs.values());
        Collections.sort(result,(a,b)->String.CASE_INSENSITIVE_ORDER.compare(a.label,b.label));
        return result;
    }

    void select(String packageName) {
        String selected=packageName==null?"":packageName;
        if(selected.equals(activePackage)&&(selected.isEmpty()||activeResources!=null))return;
        activePackage=selected;activeResources=null;mappings.clear();
        if(selected.isEmpty())return;
        try {
            activeResources=context.getPackageManager().getResourcesForApplication(selected);
            int xml=activeResources.getIdentifier("appfilter","xml",selected);
            if(xml!=0)readMappings(activeResources.getXml(xml));
            else {
                int raw=activeResources.getIdentifier("appfilter","raw",selected);
                if(raw==0)return;
                InputStream input=activeResources.openRawResource(raw);
                try {XmlPullParser parser=Xml.newPullParser();parser.setInput(input,"UTF-8");readMappings(parser);}
                finally {input.close();}
            }
        } catch(Exception e) { activeResources=null;mappings.clear(); }
    }

    void invalidate() {
        activePackage="";
        activeResources=null;
        mappings.clear();
    }

    private void readMappings(XmlPullParser parser) throws Exception {
        try {
            int event;
            while((event=parser.next())!=XmlPullParser.END_DOCUMENT) {
                if(event!=XmlPullParser.START_TAG||!"item".equals(parser.getName()))continue;
                String key=normalizeComponent(parser.getAttributeValue(null,"component"));
                String drawable=parser.getAttributeValue(null,"drawable");
                if(key!=null&&drawable!=null&&!drawable.isEmpty())mappings.put(key,drawable);
            }
        } finally {if(parser instanceof XmlResourceParser)((XmlResourceParser)parser).close();}
    }

    Drawable icon(String packageName,String className) {
        if(activeResources==null||packageName==null||className==null)return null;
        String name=mappings.get(packageName+"/"+normalizeClass(packageName,className));
        if(name==null)return null;
        try {
            int id=activeResources.getIdentifier(name,"drawable",activePackage);
            if(id==0)id=activeResources.getIdentifier(name,"mipmap",activePackage);
            return id==0?null:activeResources.getDrawable(id);
        } catch(Exception ignored) { return null; }
    }

    private static String normalizeComponent(String raw) {
        if(raw==null)return null;
        String value=raw.trim();
        int start=value.indexOf('{'),end=value.lastIndexOf('}');
        if(start>=0&&end>start)value=value.substring(start+1,end);
        int slash=value.indexOf('/');
        if(slash<1||slash==value.length()-1)return null;
        String pkg=value.substring(0,slash),cls=value.substring(slash+1);
        return pkg+"/"+normalizeClass(pkg,cls);
    }

    private static String normalizeClass(String packageName,String className) {
        if(className.startsWith("."))return packageName+className;
        return className;
    }
}
