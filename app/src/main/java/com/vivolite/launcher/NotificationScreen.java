package com.vivolite.launcher;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

/** Displays actual notifications only when Notification Access is enabled by the user. */
public final class NotificationScreen extends LinearLayout {
    private final Context context;
    private final LinearLayout list;
    private final LruCache<String,Drawable> appIcons=new LruCache<>(16);

    public NotificationScreen(Context context) {
        super(context);
        this.context=context;
        LinearLayout panel=this;
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(context,22),Ui.dp(context,24),Ui.dp(context,22),Ui.dp(context,18));
        panel.setBackground(Ui.panel(context,Ui.GLASS,24));
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=text("‹",30,Ui.primaryText(),false);back.setGravity(Gravity.CENTER);
        header.addView(back,new LinearLayout.LayoutParams(Ui.dp(context,48),Ui.dp(context,44)));
        back.setOnClickListener(v->((MainActivity)context).navigateToSurface(SurfaceNavigation.HOME));
        Ui.attachTouchFeedback(back,true);
        TextView heading=text("NOTIFICATIONS",13,Ui.primaryText(),true);heading.setLetterSpacing(0.12f);
        header.addView(heading,new LinearLayout.LayoutParams(0,Ui.dp(context,44),1));panel.addView(header);
        ScrollView scroll=new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        list=new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list,new ScrollView.LayoutParams(-1,-2));
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView clear=text("CLEAR ALL",11,Ui.primaryText(),true);
        clear.setGravity(Gravity.CENTER);
        clear.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,16));
        LinearLayout.LayoutParams clearParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,42));
        clearParams.topMargin=Ui.dp(context,8);panel.addView(clear,clearParams);
        clear.setOnClickListener(v->{
            if(!LauncherNotificationService.isConnected())
                android.widget.Toast.makeText(context,"Notification access is enabled but the listener is reconnecting",android.widget.Toast.LENGTH_SHORT).show();
            else if(LauncherNotificationService.dismissAll()) android.widget.Toast.makeText(context,"Clear request sent",android.widget.Toast.LENGTH_SHORT).show();
            else android.widget.Toast.makeText(context,"Notifications could not be cleared",android.widget.Toast.LENGTH_SHORT).show();
            refresh();
        });
        Ui.attachTouchFeedback(clear,true);
        LinearLayout.LayoutParams navigationParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,58));
        navigationParams.topMargin=Ui.dp(context,10);
        panel.addView(new SurfaceNavigation(context,SurfaceNavigation.NOTIFICATIONS),navigationParams);
        refresh();
    }

    private void refresh() {
        list.removeAllViews();
        android.app.ActivityManager am=(android.app.ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        if(am!=null&&am.isLowRamDevice()) {
            addMessage("Android disables notification listeners on low-memory devices running Android 10 or earlier. VIVO-LITE cannot display notifications on this device.");
            return;
        }
        if(!hasAccess()) {
            TextView explanation=text("Notification access is off. Enable it in Android settings to view real notifications.",13,Ui.secondaryText(),false);
            explanation.setGravity(Gravity.CENTER);
            explanation.setPadding(Ui.dp(context,12),Ui.dp(context,18),Ui.dp(context,12),Ui.dp(context,18));
            list.addView(explanation,new LinearLayout.LayoutParams(-1,-2));
            TextView enable=text("OPEN NOTIFICATION ACCESS",11,Ui.primaryText(),true);
            enable.setGravity(Gravity.CENTER);
            enable.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,16));
            list.addView(enable,new LinearLayout.LayoutParams(-1,Ui.dp(context,44)));
            enable.setOnClickListener(v->{try{context.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception ignored){}});
            Ui.attachTouchFeedback(enable,true);
            return;
        }
        if(!LauncherNotificationService.isConnected()) {
            addMessage("Notification access is enabled, but Android has not connected the listener. Try again later.");
            return;
        }
        List<LauncherNotificationService.Entry> entries=LauncherNotificationService.snapshot();
        if(entries.isEmpty()) {
            TextView empty=text("You’re all caught up",14,Ui.secondaryText(),false);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty,new LinearLayout.LayoutParams(-1,Ui.dp(context,110)));
            return;
        }
        for(LauncherNotificationService.Entry entry:entries)addEntry(entry);
    }

    private void addMessage(String message) {
        TextView text=text(message,13,Ui.secondaryText(),false);text.setGravity(Gravity.CENTER);
        text.setPadding(Ui.dp(context,12),Ui.dp(context,20),Ui.dp(context,12),Ui.dp(context,20));
        list.addView(text,new LinearLayout.LayoutParams(-1,-2));
    }

    void onShown() { refresh(); }
    void onHidden() { list.removeAllViews();appIcons.evictAll(); }

    private boolean hasAccess() {
        String enabled=android.provider.Settings.Secure.getString(context.getContentResolver(),"enabled_notification_listeners");
        return enabled!=null&&enabled.contains(context.getPackageName()+"/");
    }

    private void addEntry(LauncherNotificationService.Entry entry) {
        LinearLayout card=new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(context,12),Ui.dp(context,10),Ui.dp(context,12),Ui.dp(context,10));
        card.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,16));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
        params.bottomMargin=Ui.dp(context,7);list.addView(card,params);
        String appName=entry.packageName==null?"App":entry.packageName;
        Drawable appIcon=entry.packageName==null?null:appIcons.get(entry.packageName);
        try {
            if(entry.packageName!=null) {
                android.content.pm.ApplicationInfo info=context.getPackageManager().getApplicationInfo(entry.packageName,0);
                appName=context.getPackageManager().getApplicationLabel(info).toString();
                if(appIcon==null){appIcon=context.getPackageManager().getApplicationIcon(info);appIcons.put(entry.packageName,appIcon);}
            }
        }
        catch(Exception ignored) { }
        if(appIcon!=null){android.widget.ImageView icon=new android.widget.ImageView(context);icon.setImageDrawable(appIcon);card.addView(icon,new LinearLayout.LayoutParams(Ui.dp(context,28),Ui.dp(context,28)));}
        TextView identity=text(appName+"  ·  "+DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(entry.when)),10,Ui.secondaryText(),false);
        card.addView(identity);
        if(!entry.title.isEmpty()) {
            TextView title=text(entry.title,13,Ui.primaryText(),true);
            LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=Ui.dp(context,4);card.addView(title,tp);
        }
        if(!entry.text.isEmpty()) {
            TextView body=text(entry.text,12,Ui.secondaryText(),false);
            body.setMaxLines(4);card.addView(body);
        }
        card.setOnClickListener(v->{
            try { PendingIntent pending=entry.contentIntent;if(pending!=null)pending.send(); }
            catch(Exception ignored) { }
            ((MainActivity)context).navigateToSurface(SurfaceNavigation.HOME);
        });
        Ui.attachTouchFeedback(card,true);
        card.setOnLongClickListener(v->{
            if(LauncherNotificationService.dismiss(entry.key))refresh();
            else android.widget.Toast.makeText(context,"Notification is no longer available",android.widget.Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private TextView text(String value,int size,int color,boolean bold) {
        TextView view=new TextView(context);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }
}
