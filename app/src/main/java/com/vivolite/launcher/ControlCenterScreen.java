package com.vivolite.launcher;

import android.media.AudioManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;

public class ControlCenterScreen extends LinearLayout {
    private final TextView flashlight;
    private final TextView brightnessValue;
    private final TextView volumeValue;

    public ControlCenterScreen(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(Ui.dp(context,20),Ui.dp(context,20),Ui.dp(context,20),Ui.dp(context,12));
        setBackground(new LauncherWallpaperDrawable(Ui.ACCENT,0));

        LinearLayout header=new LinearLayout(context);
        header.setOrientation(VERTICAL);
        header.setPadding(Ui.dp(context,4),Ui.dp(context,4),Ui.dp(context,4),Ui.dp(context,14));
        TextView eyebrow=text(context,"VIVO LITE  /  DEVICE",9,Ui.ACCENT,true);
        eyebrow.setLetterSpacing(0.18f);
        header.addView(eyebrow);
        TextView title=text(context,"Control centre",25,Ui.primaryText(),false);
        title.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,38));
        titleParams.topMargin=Ui.dp(context,3);
        header.addView(title,titleParams);
        TextView subtitle=text(context,"Your essentials, in one quiet space.",11,Ui.secondaryText(),false);
        header.addView(subtitle);
        addView(header);

        ScrollView scroll=new ScrollView(context);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=new LinearLayout(context);
        content.setOrientation(VERTICAL);
        content.setPadding(Ui.dp(context,2),Ui.dp(context,2),Ui.dp(context,2),Ui.dp(context,10));
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        TextView quickLabel=sectionLabel(context,"QUICK CONTROLS");
        content.addView(quickLabel);
        LinearLayout row=new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        addTile(context,row,"Wi-Fi","◉",()->wifi());
        addTile(context,row,"Bluetooth","ᛒ",()->bluetooth());
        addTile(context,row,"Sound","♫",()->open(Settings.ACTION_SOUND_SETTINGS));
        LinearLayout.LayoutParams tileRow=new LinearLayout.LayoutParams(-1,Ui.dp(context,100));
        tileRow.topMargin=Ui.dp(context,8);
        content.addView(row,tileRow);

        LinearLayout flashRow=new LinearLayout(context);
        flashRow.setGravity(Gravity.CENTER_VERTICAL);
        flashRow.setPadding(Ui.dp(context,14),0,Ui.dp(context,12),0);
        flashRow.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,18));
        TextView flashIcon=text(context,"ϟ",19,Ui.ACCENT,true);
        flashIcon.setGravity(Gravity.CENTER);
        flashRow.addView(flashIcon,new LinearLayout.LayoutParams(Ui.dp(context,34),Ui.dp(context,44)));
        flashlight=text(context,"Flashlight",12,Ui.primaryText(),true);
        LinearLayout.LayoutParams flashTextParams=new LinearLayout.LayoutParams(0,Ui.dp(context,44),1);
        flashTextParams.leftMargin=Ui.dp(context,8);
        flashRow.addView(flashlight,flashTextParams);
        TextView flashAction=text(context,"TOGGLE",9,Ui.ACCENT,true);
        flashAction.setLetterSpacing(0.12f);
        flashRow.addView(flashAction);
        LinearLayout.LayoutParams flashParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,52));
        flashParams.topMargin=Ui.dp(context,9);
        content.addView(flashRow,flashParams);
        flashRow.setOnClickListener(v->((MainActivity)context).toggleTorch());
        Ui.attachTouchFeedback(flashRow,true);

        TextView displayLabel=sectionLabel(context,"DISPLAY");
        LinearLayout.LayoutParams displayParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,28));
        displayParams.topMargin=Ui.dp(context,16);
        content.addView(displayLabel,displayParams);
        LinearLayout brightnessCard=controlCard(context);
        TextView brightnessTitle=text(context,"Screen brightness",12,Ui.primaryText(),true);
        brightnessCard.addView(brightnessTitle);
        brightnessValue=text(context,"—",10,Ui.secondaryText(),false);
        brightnessValue.setGravity(Gravity.RIGHT);
        LinearLayout brightnessHeader=new LinearLayout(context);
        brightnessHeader.setGravity(Gravity.CENTER_VERTICAL);
        brightnessHeader.addView(brightnessTitle,new LinearLayout.LayoutParams(0,Ui.dp(context,26),1));
        brightnessHeader.addView(brightnessValue);
        brightnessCard.addView(brightnessHeader);
        SeekBar bar=new SeekBar(context);
        bar.setMax(255);
        tint(bar);
        try {
            int current=Settings.System.getInt(context.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,130);
            bar.setProgress(current);
            brightnessValue.setText(Math.round(current*100f/255f)+"%");
        } catch(Exception ignored) { bar.setProgress(130); }
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int value,boolean user) {
                brightnessValue.setText(Math.round(value*100f/255f)+"%");
                if(!user)return;
                if(android.os.Build.VERSION.SDK_INT>=23&&!Settings.System.canWrite(context)) {
                    openBrightnessPermission(context);
                    return;
                }
                try {
                    Settings.System.putInt(context.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    Settings.System.putInt(context.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(15,value));
                } catch(Exception e) {
                    Toast.makeText(context,"Use system brightness settings",Toast.LENGTH_SHORT).show();
                }
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        LinearLayout.LayoutParams barParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,36));
        barParams.topMargin=Ui.dp(context,4);
        brightnessCard.addView(bar,barParams);
        LinearLayout.LayoutParams brightnessCardParams=new LinearLayout.LayoutParams(-1,-2);
        brightnessCardParams.topMargin=Ui.dp(context,7);
        content.addView(brightnessCard,brightnessCardParams);

        TextView soundLabel=sectionLabel(context,"SOUND");
        LinearLayout.LayoutParams soundParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,28));
        soundParams.topMargin=Ui.dp(context,13);
        content.addView(soundLabel,soundParams);
        LinearLayout volumeCard=controlCard(context);
        LinearLayout volumeHeader=new LinearLayout(context);
        volumeHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView volumeTitle=text(context,"Media volume",12,Ui.primaryText(),true);
        volumeHeader.addView(volumeTitle,new LinearLayout.LayoutParams(0,Ui.dp(context,26),1));
        volumeValue=text(context,"—",10,Ui.secondaryText(),false);
        volumeHeader.addView(volumeValue);
        volumeCard.addView(volumeHeader);
        SeekBar volume=new SeekBar(context);
        AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if(audio!=null) {
            int max=Math.max(1,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            int current=audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            volume.setMax(max);
            volume.setProgress(current);
            volumeValue.setText(Math.round(current*100f/max)+"%");
            volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seek,int value,boolean user) {
                    volumeValue.setText(Math.round(value*100f/max)+"%");
                    if(user)audio.setStreamVolume(AudioManager.STREAM_MUSIC,value,0);
                }
                public void onStartTrackingTouch(SeekBar seek) {}
                public void onStopTrackingTouch(SeekBar seek) {}
            });
        }
        tint(volume);
        LinearLayout.LayoutParams volumeBarParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,36));
        volumeBarParams.topMargin=Ui.dp(context,4);
        volumeCard.addView(volume,volumeBarParams);
        LinearLayout.LayoutParams volumeCardParams=new LinearLayout.LayoutParams(-1,-2);
        volumeCardParams.topMargin=Ui.dp(context,7);
        content.addView(volumeCard,volumeCardParams);

        LinearLayout info=new LinearLayout(context);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(Ui.dp(context,13),0,Ui.dp(context,13),0);
        info.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,16));
        TextView battery=text(context,batteryLabel(context),10,Ui.secondaryText(),true);
        info.addView(battery,new LinearLayout.LayoutParams(0,Ui.dp(context,42),1));
        TextView settingsShortcut=text(context,"SYSTEM SETTINGS  ↗",9,Ui.ACCENT,true);
        settingsShortcut.setLetterSpacing(0.06f);
        info.addView(settingsShortcut);
        LinearLayout.LayoutParams infoParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,44));
        infoParams.topMargin=Ui.dp(context,12);
        content.addView(info,infoParams);
        settingsShortcut.setOnClickListener(v->open(Settings.ACTION_SETTINGS));
        info.setOnClickListener(v->open(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        Ui.attachTouchFeedback(settingsShortcut,true);
        Ui.attachTouchFeedback(info,true);

        TextView notifications=text(context,"VIEW NOTIFICATIONS",10,Ui.primaryText(),true);
        notifications.setGravity(Gravity.CENTER);
        notifications.setLetterSpacing(0.10f);
        notifications.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,16));
        LinearLayout.LayoutParams notificationParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,44));
        notificationParams.topMargin=Ui.dp(context,9);
        content.addView(notifications,notificationParams);
        notifications.setOnClickListener(v->((MainActivity)context).navigateToSurface(SurfaceNavigation.NOTIFICATIONS));
        Ui.attachTouchFeedback(notifications,true);

        LinearLayout.LayoutParams navigationParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,58));
        navigationParams.topMargin=Ui.dp(context,9);
        addView(new SurfaceNavigation(context,SurfaceNavigation.CONTROL),navigationParams);
    }

    void setTorchState(boolean enabled) {
        flashlight.setText("Flashlight  ·  "+(enabled?"ON":"OFF"));
    }

    private TextView sectionLabel(Context c,String value) {
        TextView t=text(c,value,9,Ui.secondaryText(),true);
        t.setLetterSpacing(0.16f);
        return t;
    }

    private LinearLayout controlCard(Context c) {
        LinearLayout card=new LinearLayout(c);
        card.setOrientation(VERTICAL);
        card.setPadding(Ui.dp(c,14),Ui.dp(c,9),Ui.dp(c,14),Ui.dp(c,8));
        card.setBackground(Ui.panel(c,Ui.GLASS_LIGHT,18));
        return card;
    }

    private void tint(SeekBar bar) {
        android.content.res.ColorStateList accent=android.content.res.ColorStateList.valueOf(Ui.ACCENT);
        bar.setProgressTintList(accent);
        bar.setThumbTintList(accent);
    }

    private void addTile(Context c,LinearLayout row,String label,String glyph,Runnable action) {
        LinearLayout tile=new LinearLayout(c);
        tile.setOrientation(VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(Ui.dp(c,5),Ui.dp(c,8),Ui.dp(c,5),Ui.dp(c,7));
        tile.setBackground(Ui.panel(c,Ui.GLASS_LIGHT,19));
        TextView symbol=text(c,glyph,22,Ui.ACCENT,true);
        symbol.setGravity(Gravity.CENTER);
        tile.addView(symbol,new LinearLayout.LayoutParams(-1,Ui.dp(c,38)));
        TextView caption=text(c,label,10,Ui.primaryText(),true);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams captionParams=new LinearLayout.LayoutParams(-1,Ui.dp(c,23));
        captionParams.topMargin=Ui.dp(c,3);
        tile.addView(caption,captionParams);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
        p.setMargins(Ui.dp(c,4),0,Ui.dp(c,4),0);
        row.addView(tile,p);
        tile.setOnClickListener(v->action.run());
        Ui.attachTouchFeedback(tile,true);
        tile.setOnLongClickListener(v->{
            if(label.equals("Wi-Fi"))open(Settings.ACTION_WIFI_SETTINGS);
            else if(label.equals("Bluetooth"))open(Settings.ACTION_BLUETOOTH_SETTINGS);
            else open(Settings.ACTION_SOUND_SETTINGS);
            return true;
        });
    }

    private void wifi() {
        try {
            WifiManager wm=(WifiManager)getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(wm!=null) {
                boolean ok=wm.setWifiEnabled(!wm.isWifiEnabled());
                if(!ok)open(Settings.ACTION_WIFI_SETTINGS);
            }
        } catch(Exception e) { open(Settings.ACTION_WIFI_SETTINGS); }
    }

    private void bluetooth() {
        try {
            BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
            if(adapter!=null) {
                if(adapter.isEnabled())adapter.disable();else adapter.enable();
            } else open(Settings.ACTION_BLUETOOTH_SETTINGS);
        } catch(Exception e) { open(Settings.ACTION_BLUETOOTH_SETTINGS); }
    }

    private void open(String action) {
        try { getContext().startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch(Exception ignored) { Toast.makeText(getContext(),"Setting unavailable",Toast.LENGTH_SHORT).show(); }
    }

    private void openBrightnessPermission(Context context) {
        try {
            context.startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                android.net.Uri.parse("package:"+context.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch(Exception ignored) { open(Settings.ACTION_DISPLAY_SETTINGS); }
        Toast.makeText(context,"Allow VIVO LITE to modify system settings, then return to adjust brightness",Toast.LENGTH_LONG).show();
    }

    private String batteryLabel(Context context) {
        try {
            Intent status=context.registerReceiver(null,new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(status!=null) {
                int level=status.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,-1);
                int scale=status.getIntExtra(android.os.BatteryManager.EXTRA_SCALE,-1);
                if(level>=0&&scale>0)return "BATTERY  ·  "+(level*100/scale)+"%";
            }
        } catch(Exception ignored) {}
        return "BATTERY  ·  unavailable";
    }

    private TextView text(Context c,String value,int size,int color,boolean bold) {
        TextView t=new TextView(c);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        return t;
    }
}
