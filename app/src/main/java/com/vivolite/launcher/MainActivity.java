package com.vivolite.launcher;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(9,13,19), PANEL = Color.rgb(20,27,37);
    private static final int TEXT = Color.rgb(242,246,250), MUTED = Color.rgb(145,158,174);
    private static final int ACCENT = Color.rgb(85,200,242);
    private final ArrayList<AppItem> apps = new ArrayList<>();
    private final Set<String> favorites = new HashSet<>(), hidden = new HashSet<>();
    private android.content.SharedPreferences prefs;
    private LinearLayout root, appGrid, dock;
    private EditText search;
    private TextView count;
    private int columns = 5;
    private boolean showClock = true, showDock = true;
    private TextClock clock, date;
    private static class AppItem {
        String label, pkg, cls;
        android.graphics.drawable.Drawable icon;
        String key() { return pkg + "/" + cls; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("vivolite_v02", MODE_PRIVATE);
        columns = prefs.getInt("columns", 5);
        showClock = prefs.getBoolean("clock", true);
        showDock = prefs.getBoolean("dock", true);
        favorites.addAll(prefs.getStringSet("favorites", new HashSet<String>()));
        hidden.addAll(prefs.getStringSet("hidden", new HashSet<String>()));
        build();
        loadApps();
    }
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(1); return l; }
    private void build() {
        root = vertical(); root.setBackgroundColor(BG); root.setPadding(dp(18),dp(18),dp(18),dp(8));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("V I V O   L I T E",12,ACCENT,true);
        top.addView(brand,new LinearLayout.LayoutParams(0,dp(32),1));
        TextView settings = label("SETTINGS",11,MUTED,true); settings.setGravity(Gravity.CENTER);
        settings.setPadding(dp(12),0,dp(12),0); settings.setBackground(shape(PANEL,18,0));
        settings.setOnClickListener(v -> settings()); top.addView(settings,new LinearLayout.LayoutParams(-2,dp(34)));
        root.addView(top);
        clock = new TextClock(this); clock.setFormat24Hour("HH:mm"); clock.setFormat12Hour("h:mm");
        clock.setTextSize(48); clock.setTextColor(TEXT); clock.setTypeface(Typeface.create("sans-serif-light",0));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,dp(62)); cp.topMargin=dp(20); root.addView(clock,cp);
        date = new TextClock(this); date.setFormat24Hour("EEEE, d MMMM"); date.setFormat12Hour("EEEE, d MMMM");
        date.setTextSize(13); date.setTextColor(MUTED);
        LinearLayout.LayoutParams datep = new LinearLayout.LayoutParams(-1,dp(26)); datep.bottomMargin=dp(18); root.addView(date,datep);
        if (!showClock) { clock.setVisibility(View.GONE); date.setVisibility(View.GONE); }
        search = new EditText(this); search.setSingleLine(true); search.setTextSize(14); search.setTextColor(TEXT);
        search.setHintTextColor(MUTED); search.setHint("Search apps"); search.setPadding(dp(16),0,dp(16),0);
        search.setBackground(shape(PANEL,16,Color.rgb(35,48,62)));
        root.addView(search,new LinearLayout.LayoutParams(-1,dp(46)));
        LinearLayout heading = new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("YOUR APPS",11,MUTED,true); heading.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
        count=label("0 APPS",10,MUTED,false); heading.addView(count); root.addView(heading);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(false);
        appGrid=new LinearLayout(this); appGrid.setOrientation(1); scroll.addView(appGrid);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if (showDock) {
            TextView dockTitle=label("FAVOURITES",10,MUTED,true);
            LinearLayout.LayoutParams dt=new LinearLayout.LayoutParams(-1,dp(28)); dt.topMargin=dp(4); root.addView(dockTitle,dt);
            dock=new LinearLayout(this); dock.setGravity(Gravity.CENTER_VERTICAL); dock.setBackground(shape(PANEL,18,0));
            dock.setPadding(dp(8),dp(8),dp(8),dp(8)); root.addView(dock,new LinearLayout.LayoutParams(-1,dp(68)));
        }
        TextView foot=label("V0.2  •  LIGHTWEIGHT HOME",9,MUTED,false); foot.setGravity(Gravity.CENTER);
        root.addView(foot,new LinearLayout.LayoutParams(-1,dp(24)));
        setContentView(root);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){ render(s.toString()); }
            public void afterTextChanged(Editable e){}
        });
    }
    private void loadApps() {
        apps.clear(); PackageManager pm=getPackageManager();
        Intent q=new Intent(Intent.ACTION_MAIN); q.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list=pm.queryIntentActivities(q,0);
        HashSet<String> seen=new HashSet<>();
        for (ResolveInfo r:list) {
            if(r.activityInfo==null || r.activityInfo.packageName.equals(getPackageName())) continue;
            AppItem a=new AppItem(); a.pkg=r.activityInfo.packageName; a.cls=r.activityInfo.name;
            a.label=String.valueOf(r.loadLabel(pm)); a.icon=r.loadIcon(pm);
            if(seen.add(a.key())) apps.add(a);
        }
        Collections.sort(apps,(a,b)->a.label.compareToIgnoreCase(b.label));
        render(search==null?"":search.getText().toString());
    }
    private void render(String filter) {
        if(appGrid==null) return; appGrid.removeAllViews();
        ArrayList<AppItem> visible=new ArrayList<>();
        for(AppItem a:apps) if(!hidden.contains(a.key()) && a.label.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))) visible.add(a);
        count.setText(visible.size()+" APPS");
        int width=getResources().getDisplayMetrics().widthPixels-dp(36);
        int cell=Math.max(dp(52),width/columns);
        LinearLayout row=null;
        for(int i=0;i<visible.size();i++) {
            if(i%columns==0) { row=new LinearLayout(this); row.setGravity(Gravity.TOP); appGrid.addView(row,new LinearLayout.LayoutParams(-1,dp(100))); }
            AppItem a=visible.get(i);
            LinearLayout item=vertical(); item.setGravity(Gravity.CENTER); item.setPadding(dp(2),dp(7),dp(2),dp(4));
            ImageView icon=new ImageView(this); icon.setImageDrawable(a.icon); icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            item.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));
            TextView name=label(a.label,10,TEXT,false); name.setGravity(Gravity.CENTER); name.setSingleLine(true); name.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,dp(24)); np.topMargin=dp(4); item.addView(name,np);
            item.setOnClickListener(v->launch(a));
            item.setOnLongClickListener(v->{ menu(a); return true; });
            row.addView(item,new LinearLayout.LayoutParams(cell,dp(96)));
        }
        if(visible.isEmpty()) { TextView empty=label(filter.isEmpty()?"No apps available":"No matching apps",13,MUTED,false); empty.setGravity(Gravity.CENTER); appGrid.addView(empty,new LinearLayout.LayoutParams(-1,dp(70))); }
        renderDock();
    }
    private void launch(AppItem a) {
        Intent i=new Intent(Intent.ACTION_MAIN); i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setComponent(new ComponentName(a.pkg,a.cls)); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(i); } catch(Exception e) { Toast.makeText(this,"Couldn't open app",Toast.LENGTH_SHORT).show(); }
    }
    private void menu(AppItem a) {
        String pin=favorites.contains(a.key())?"Remove from favourites":"Add to favourites";
        String[] opts={"Open",pin,"App info","Hide app"};
        new AlertDialog.Builder(this).setTitle(a.label).setItems(opts,(d,n)->{
            if(n==0) launch(a);
            else if(n==1) { if(!favorites.add(a.key())) favorites.remove(a.key()); save(); render(search.getText().toString()); }
            else if(n==2) { Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+a.pkg)); startActivity(i); }
            else { hidden.add(a.key()); save(); render(search.getText().toString()); }
        }).show();
    }
    private void renderDock() {
        if(dock==null) return; dock.removeAllViews(); int shown=0;
        for(AppItem a:apps) if(favorites.contains(a.key()) && !hidden.contains(a.key())) {
            ImageView iv=new ImageView(this); iv.setImageDrawable(a.icon); iv.setPadding(dp(8),dp(5),dp(8),dp(5));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER); dock.addView(iv,new LinearLayout.LayoutParams(0,dp(50),1));
            iv.setOnClickListener(v->launch(a)); shown++;
        }
        if(shown==0) { TextView hint=label("Long-press an app to pin it here",11,MUTED,false); hint.setGravity(Gravity.CENTER); dock.addView(hint,new LinearLayout.LayoutParams(-1,-1)); }
    }
    private void save() {
        prefs.edit().putStringSet("favorites",new HashSet<>(favorites)).putStringSet("hidden",new HashSet<>(hidden)).apply();
    }
    private void settings() {
        String[] opts={"4-column grid","5-column grid","Show / hide clock","Show / hide favourites dock","Restore hidden apps","Refresh app list","Android home settings"};
        new AlertDialog.Builder(this).setTitle("VIVO LITE SETTINGS").setItems(opts,(d,n)->{
            if(n==0||n==1){columns=n==0?4:5;prefs.edit().putInt("columns",columns).apply();render(search.getText().toString());}
            if(n==2){showClock=!showClock;prefs.edit().putBoolean("clock",showClock).apply();clock.setVisibility(showClock?View.VISIBLE:View.GONE);date.setVisibility(showClock?View.VISIBLE:View.GONE);}
            if(n==3){showDock=!showDock;prefs.edit().putBoolean("dock",showDock).apply();build();loadApps();}
            if(n==4){hidden.clear();save();render(search.getText().toString());}
            if(n==5)loadApps();
            if(n==6){try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
        }).show();
    }
    @Override protected void onResume(){super.onResume(); if(appGrid!=null) loadApps();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i); if(search!=null) search.setText("");}
    @Override public void onBackPressed() {
        if(search!=null && search.length()>0){search.setText(""); return;}
        super.onBackPressed();
    }
}
