package com.vivolite.launcher;

import android.app.Notification;
import android.app.PendingIntent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

/** Stores only a small, in-memory snapshot of notifications the user granted access to. */
public final class LauncherNotificationService extends NotificationListenerService {
    private static final int MAX_ITEMS = 40;
    private static final ArrayList<Entry> entries = new ArrayList<>();
    private static volatile LauncherNotificationService instance;

    public static final class Entry {
        public final String key, packageName, title, text;
        public final long when;
        public final PendingIntent contentIntent;
        Entry(StatusBarNotification status) {
            key=status.getKey(); packageName=status.getPackageName();
            Notification notification=status.getNotification();
            contentIntent=notification.contentIntent;
            Bundle extras=notification.extras;
            CharSequence titleValue=extras==null?null:extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textValue=extras==null?null:extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            if(textValue==null&&extras!=null)textValue=extras.getCharSequence(Notification.EXTRA_TEXT);
            title=titleValue==null?"":titleValue.toString();
            text=textValue==null?"":textValue.toString();
            when=status.getPostTime();
        }
    }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        instance=this;
        synchronized(entries) {
            entries.clear();
            StatusBarNotification[] active=getActiveNotifications();
            if(active!=null)for(StatusBarNotification status:active)add(status);
        }
    }

    @Override public void onListenerDisconnected() {
        super.onListenerDisconnected();
        releaseSession();
    }

    @Override public void onDestroy() {
        releaseSession();
        super.onDestroy();
    }

    private void releaseSession() {
        if(instance==this)instance=null;
        synchronized(entries) { entries.clear(); }
    }

    @Override public void onNotificationPosted(StatusBarNotification status) {
        synchronized(entries) { add(status); }
    }

    @Override public void onNotificationRemoved(StatusBarNotification status) {
        synchronized(entries) {
            for(int i=entries.size()-1;i>=0;i--)if(entries.get(i).key.equals(status.getKey()))entries.remove(i);
        }
    }

    private static void add(StatusBarNotification status) {
        for(int i=entries.size()-1;i>=0;i--)if(entries.get(i).key.equals(status.getKey()))entries.remove(i);
        entries.add(0,new Entry(status));
        while(entries.size()>MAX_ITEMS)entries.remove(entries.size()-1);
    }

    public static List<Entry> snapshot() {
        synchronized(entries) { return new ArrayList<>(entries); }
    }

    public static boolean isConnected() { return instance!=null; }

    public static boolean dismiss(String key) {
        LauncherNotificationService service=instance;
        if(service==null||key==null)return false;
        try { service.cancelNotification(key); return true; }
        catch(RuntimeException e) { return false; }
    }

    public static boolean dismissAll() {
        LauncherNotificationService service=instance;
        if(service==null)return false;
        try {
            StatusBarNotification[] active=service.getActiveNotifications();
            if(active==null)return false;
            for(StatusBarNotification status:active)service.cancelNotification(status.getKey());
            return true;
        } catch(RuntimeException e) { return false; }
    }
}
