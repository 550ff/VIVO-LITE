package com.vivolite.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(21, 23, 28);
    private final int tile = Color.rgb(47, 51, 60);
    private final int primary = Color.rgb(245, 246, 249);
    private final int secondary = Color.rgb(169, 175, 187);
    private LinearLayout root;
    private GridLayout grid;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildScreen();
        loadApps();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(26), dp(24), dp(16));
        root.setBackgroundColor(bg);

        TextView brand = text("VIVO LITE", 13, secondary, true);
        root.addView(brand, new LinearLayout.LayoutParams(-1, dp(28)));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("h:mm");
        clock.setTextSize(48);
        clock.setTextColor(primary);
        clock.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(-1, dp(64));
        clockParams.topMargin = dp(24);
        root.addView(clock, clockParams);

        TextView date = text(new java.text.SimpleDateFormat("EEEE, d MMMM",
                java.util.Locale.getDefault()).format(new java.util.Date()), 14, secondary, false);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, dp(28));
        dateParams.bottomMargin = dp(28);
        root.addView(date, dateParams);

        TextView heading = text("Apps", 18, primary, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, dp(34));
        headingParams.bottomMargin = dp(12);
        root.addView(heading, headingParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setUseDefaultMargins(false);
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView footer = text("VIVO LITE  ·  V0.1", 11, secondary, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(34)));

        setContentView(root);
    }

    private void loadApps() {
        grid.removeAllViews();
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(query, 0);
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString());
            }
        });

        int count = 0;
        for (final ResolveInfo info : apps) {
            if (info.activityInfo.packageName.equals(getPackageName())) continue;
            final String packageName = info.activityInfo.packageName;
            String label = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(10), dp(4), dp(10));
            item.setBackgroundResource(android.R.drawable.list_selector_background);

            ImageView image = new ImageView(this);
            image.setImageDrawable(icon);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(48), dp(48));
            imageParams.gravity = Gravity.CENTER;
            item.addView(image, imageParams);

            TextView name = text(label, 12, primary, false);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(1);
            name.setSingleLine(true);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, dp(28));
            nameParams.topMargin = dp(5);
            item.addView(name, nameParams);

            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Intent launch = pm.getLaunchIntentForPackage(packageName);
                    if (launch != null) {
                        try { startActivity(launch); }
                        catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Unable to open app", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(96);
            params.columnSpec = GridLayout.spec(count % 3, 1f);
            params.rowSpec = GridLayout.spec(count / 3);
            grid.addView(item, params);
            count++;
        }

        if (count == 0) {
            TextView empty = text("No launchable apps found.", 14, secondary, false);
            grid.addView(empty);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (grid != null) loadApps();
    }
}
