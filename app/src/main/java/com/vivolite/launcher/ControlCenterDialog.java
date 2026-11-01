package com.vivolite.launcher;

import android.app.Dialog;
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

public class ControlCenterDialog extends Dialog {
    public ControlCenterDialog(Context context) {
        super(context);
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(context,20),Ui.dp(context,18),Ui.dp(context,20),Ui.dp(context,20));
        panel.setBackground(Ui.panel(context,Ui.GLASS,28));

        TextView heading = text(context,"CONTROL CENTRE",13,Ui.MUTED);
        heading.setLetterSpacing(0.12f);
        panel.addView(heading,new LinearLayout.LayoutParams(-1,Ui.dp(context,32)));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addTile(context,row,"Wi-Fi",()->wifi());
        addTile(context,row,"Bluetooth",()->bluetooth());
        addTile(context,row,"Sound",()->open(Settings.ACTION_SOUND_SETTINGS));
        panel.addView(row,new LinearLayout.LayoutParams(-1,Ui.dp(context,76)));

        TextView brightness = text(context,"BRIGHTNESS",11,Ui.MUTED);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1,Ui.dp(context,30));
        bp.topMargin=Ui.dp(context,14); panel.addView(brightness,bp);
        SeekBar bar = new SeekBar(context);
        bar.setMax(255);
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

        TextView settings = text(context,"OPEN ANDROID SETTINGS  ↗",12,Ui.WHITE);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,18));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,Ui.dp(context,46));
        sp.topMargin=Ui.dp(context,12); panel.addView(settings,sp);
        settings.setOnClickListener(v->open(Settings.ACTION_SETTINGS));

        setContentView(panel);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setLayout(Ui.dp(context,320),-2);
    }

    private void addTile(Context c,LinearLayout row,String label,Runnable action) {
        TextView tile=text(c,label,12,Ui.WHITE);
        tile.setGravity(Gravity.CENTER);
        tile.setBackground(Ui.panel(c,Ui.GLASS_LIGHT,18));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(c,64),1);
        p.setMargins(Ui.dp(c,3),0,Ui.dp(c,3),0);
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

    private TextView text(Context c,String value,int size,int color) {
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;
    }
}
