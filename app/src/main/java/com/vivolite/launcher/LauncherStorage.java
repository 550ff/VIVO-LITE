package com.vivolite.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class LauncherStorage {
    private final SharedPreferences prefs;
    private static final String KEY = "vivolite_v1";

    public LauncherStorage(Context context) {
        prefs = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
    }

    public Set<String> getHidden() {
        return new HashSet<>(prefs.getStringSet("hidden", new HashSet<String>()));
    }

    public void setHidden(Set<String> hidden) {
        prefs.edit().putStringSet("hidden", new HashSet<>(hidden)).apply();
    }

    public Set<String> getFavorites() {
        return new HashSet<>(prefs.getStringSet("favorites", new HashSet<String>()));
    }

    public void setFavorites(Set<String> values) {
        prefs.edit().putStringSet("favorites", new HashSet<>(values)).apply();
    }

    public int getColumns() { return prefs.getInt("columns", 4); }
    public void setColumns(int value) { prefs.edit().putInt("columns", value).apply(); }
    public int getHomePage() { return Math.max(0,prefs.getInt("home_page",0)); }
    public void setHomePage(int value) { prefs.edit().putInt("home_page",Math.max(0,value)).apply(); }
    public String getQuickNote() { return prefs.getString("quick_note",""); }
    public void setQuickNote(String value) { prefs.edit().putString("quick_note",value==null?"":value).apply(); }
    public boolean showClock() { return prefs.getBoolean("clock", true); }
    public void setShowClock(boolean value) { prefs.edit().putBoolean("clock", value).apply(); }
    public boolean showDock() { return prefs.getBoolean("dock", true); }
    public void setShowDock(boolean value) { prefs.edit().putBoolean("dock", value).apply(); }
    public boolean animations() { return prefs.getBoolean("animations", true); }
    public void setAnimations(boolean value) { prefs.edit().putBoolean("animations", value).apply(); }
    public boolean darkGlass() { return prefs.getBoolean("dark_glass", false); }
    public void setDarkGlass(boolean value) { prefs.edit().putBoolean("dark_glass", value).apply(); }
    public int getAccent() { return prefs.getInt("accent", 0xFF89AEFF); }
    public void setAccent(int value) { prefs.edit().putInt("accent", value).apply(); }
    public int getWallpaper() { return prefs.getInt("wallpaper", 0); }
    public void setWallpaper(int value) { prefs.edit().putInt("wallpaper", value).apply(); }
    public int getIconSize() { return prefs.getInt("icon_size", 46); }
    public void setIconSize(int value) { prefs.edit().putInt("icon_size", value).apply(); }
    public String getIconPack() { return prefs.getString("icon_pack",""); }
    public void setIconPack(String value) { prefs.edit().putString("icon_pack",value==null?"":value).apply(); }
    public int getWidgetHeight(int appWidgetId,int fallbackDp) { return prefs.getInt("widget_height_"+appWidgetId,fallbackDp); }
    public void setWidgetHeight(int appWidgetId,int heightDp) { prefs.edit().putInt("widget_height_"+appWidgetId,heightDp).apply(); }

    public JSONObject getLayout() {
        try { return new JSONObject(prefs.getString("layout", "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }

    public void setLayout(JSONObject object) {
        prefs.edit().putString("layout", object.toString()).apply();
    }

    public JSONObject getFolders() {
        try { return new JSONObject(prefs.getString("folders", "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }

    public void setFolders(JSONObject object) {
        prefs.edit().putString("folders", object.toString()).apply();
    }

    public List<String> getOrder(String key) {
        ArrayList<String> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(key, "[]"));
            for (int i=0;i<array.length();i++) result.add(array.getString(i));
        } catch (Exception ignored) {}
        return result;
    }

    public void setOrder(String key, List<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        prefs.edit().putString(key, array.toString()).apply();
    }
}
