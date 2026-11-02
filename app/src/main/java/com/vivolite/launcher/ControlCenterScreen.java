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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class ControlCenterScreen extends LinearLayout {
    public ControlCenterScreen(Context context) {
        super(context);
        LinearLayout panel = this;
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(context,24),Ui.dp(context,28),Ui.dp(context,24),Ui.dp(context,24));
        panel.setBackground(Ui.panel(context,Ui.GLASS,28));

        TextView heading = text(context,"CONTROL CENTRE",12,Ui.primaryText());
        heading.setLetterSpacing(0.15f);
        panel.addView(heading,new LinearLayout.LayoutParams(-1,Ui.dp(context,32)));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addTile(context,row,"Wi-Fi",()->wifi());
        addTile(context,row,"Bluetooth",()->bluetooth());
        addTile(context,row,"Sound",()->open(Settings.ACTION_SOUND_SETTINGS));
        LinearLayout.LayoutParams tileRow=new LinearLayout.LayoutParams(-1,Ui.dp(context,82));
        tileRow.topMargin=Ui.dp(context,8);panel.addView(row,tileRow);

        TextView brightness = text(context,"BRIGHTNESS",11,Ui.secondaryText());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1,Ui.dp(context,30));
        bp.topMargin=Ui.dp(context,14); panel.addView(brightness,bp);
        SeekBar bar = new SeekBar(context);
        bar.setMax(255);
        android.content.res.ColorStateList accent=android.content.res.ColorStateList.valueOf(Ui.ACCENT);
        bar.setProgressTintList(accent);
        bar.setThumbTintList(accent);
        try {
            int current = Settings.System.getInt(context.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,130);
            bar.setProgress(current);
        } catch(Exception ignored) { bar.setProgress(130); }
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int value,boolean user) {
                if(!user) return;
                int safe=Math.max(15,value);
                try {
                    Settings.System.putInt(context.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    Settings.System.putInt(context.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,safe);
                    Toast.makeText(context,"Brightness updated",Toast.LENGTH_SHORT).show();
                } catch(Exception e) {
                    Toast.makeText(context,"Use system brightness settings",Toast.LENGTH_SHORT).show();
                }
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        panel.addView(bar,new LinearLayout.LayoutParams(-1,Ui.dp(context,42)));

        TextView volumeLabel = text(context,"VOLUME",11,Ui.secondaryText());
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1,Ui.dp(context,30));
        vp.topMargin=Ui.dp(context,8); panel.addView(volumeLabel,vp);
        SeekBar volume = new SeekBar(context);
        AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if(audio!=null) {
            int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            volume.setMax(Math.max(1,max));
            volume.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
            volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seek,int value,boolean user) {
                    if(user)audio.setStreamVolume(AudioManager.STREAM_MUSIC,value,0);
                }
                public void onStartTrackingTouch(SeekBar seek) { }
                public void onStopTrackingTouch(SeekBar seek) { }
            });
        }
        panel.addView(volume,new LinearLayout.LayoutParams(-1,Ui.dp(context,38)));

        TextView battery=text(context,batteryLabel(context),11,Ui.secondaryText());
        LinearLayout.LayoutParams batteryParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,28));
        batteryParams.topMargin=Ui.dp(context,4);panel.addView(battery,batteryParams);

        TextView notifications=text(context,"VIEW NOTIFICATIONS",11,Ui.primaryText());
        notifications.setGravity(Gravity.CENTER);
        notifications.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,16));
        LinearLayout.LayoutParams notificationParams=new LinearLayout.LayoutParams(-1,Ui.dp(context,40));
        notificationParams.topMargin=Ui.dp(context,6);panel.addView(notifications,notificationParams);
        notifications.setOnClickListener(v->((MainActivity)context).navigateToSurface(SurfaceNavigation.NOTIFICATIONS));

        TextView settings = text(context,"OPEN ANDROID SETTINGS  ↗",12,Ui.primaryText());
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,18));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,Ui.dp(context,46));
        sp.topMargin=Ui.dp(context,12); panel.addView(settings,sp);
        settings.setOnClickListener(v->open(Settings.ACTION_SETTINGS));

        LinearLayout.LayoutParams navigationParams = new LinearLayout.LayoutParams(-1, Ui.dp(context, 58));
        navigationParams.topMargin = Ui.dp(context, 14);
        panel.addView(new SurfaceNavigation(context, SurfaceNavigation.CONTROL), navigationParams);

    }

    private void addTile(Context c,LinearLayout row,String label,Runnable action) {
        LinearLayout tile=new LinearLayout(c);
        tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);
        tile.setBackground(Ui.panel(c,Ui.GLASS_LIGHT,18));
        TextView glyph=text(c,label.equals("Wi-Fi")?"◉":label.equals("Bluetooth")?"ᛒ":"♫",20,Ui.primaryText());
        glyph.setGravity(Gravity.CENTER);tile.addView(glyph,new LinearLayout.LayoutParams(-1,Ui.dp(c,30)));
        TextView caption=text(c,label,10,Ui.secondaryText());caption.setGravity(Gravity.CENTER);
        tile.addView(caption,new LinearLayout.LayoutParams(-1,Ui.dp(c,20)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(c,76),1);
        p.setMargins(Ui.dp(c,4),0,Ui.dp(c,4),0);
        row.addView(tile,p); tile.setOnClickListener(v->action.run());
        tile.setOnLongClickListener(v->{ if(label.equals("Wi-Fi"))open(Settings.ACTION_WIFI_SETTINGS); else if(label.equals("Bluetooth"))open(Settings.ACTION_BLUETOOTH_SETTINGS); else open(Settings.ACTION_SOUND_SETTINGS); return true;});
    }

    private void wifi() {
        try {
            WifiManager wm=(WifiManager)getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(wm!=null) {
                boolean ok=wm.setWifiEnabled(!wm.isWifiEnabled());
                if(!ok) open(Settings.ACTION_WIFI_SETTINGS);
            }
        } catch(Exception e) { open(Settings.ACTION_WIFI_SETTINGS); }
    }

    private void bluetooth() {
        try {
            BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
            if(adapter!=null) {
                if(adapter.isEnabled()) adapter.disable(); else adapter.enable();
            } else open(Settings.ACTION_BLUETOOTH_SETTINGS);
        } catch(Exception e) { open(Settings.ACTION_BLUETOOTH_SETTINGS); }
    }

    private void open(String action) {
        try { getContext().startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch(Exception ignored) { Toast.makeText(getContext(),"Setting unavailable",Toast.LENGTH_SHORT).show(); }
    }

    private String batteryLabel(Context context) {
        try {
            Intent status=context.registerReceiver(null,new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(status!=null) {
                int level=status.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,-1);
                int scale=status.getIntExtra(android.os.BatteryManager.EXTRA_SCALE,-1);
                if(level>=0&&scale>0)return "BATTERY  ·  "+(level*100/scale)+"%";
            }
        } catch(Exception ignored) { }
        return "BATTERY  ·  unavailable";
    }

    private TextView text(Context c,String value,int size,int color) {
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;
    }
}
