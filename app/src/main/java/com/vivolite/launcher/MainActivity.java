package com.vivolite.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int COLUMNS = 3;

    private final int bg = Color.rgb(21, 23, 28);
    private final int tile = Color.rgb(47, 51, 60);
    private final int primary = Color.rgb(245, 246, 249);
    private final int secondary = Color.rgb(169, 175, 187);
    private LinearLayout root;
    private ScrollView scroll;
    private GridLayout grid;
    private TextView heading;

    // In-memory caches keyed by "package/class". They live as long as this
    // Activity instance and are pruned to the apps that are currently installed.
    private final Map<String, String> labelCache = new HashMap<String, String>();
    private final Map<String, Drawable> iconCache = new HashMap<String, Drawable>();

    // Fingerprint of the app list currently on screen. Lets onResume() skip
    // rebuilding the grid when no app was installed or removed.
    private String shownSignature = null;

    private static final class AppEntry {
        final ResolveInfo info;
        final ComponentName component;
        final String key;
        final String label;

        AppEntry(ResolveInfo info, ComponentName component, String label) {
            this.info = info;
            this.component = component;
            this.key = component.flattenToString();
            this.label = label;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildScreen();
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

        // TextClock keeps itself current (including after midnight and when the
        // time zone or clock changes), so the date no longer goes stale.
        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d MMMM");
        date.setFormat12Hour("EEEE, d MMMM");
        date.setTextSize(14);
        date.setTextColor(secondary);
        date.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, dp(28));
        dateParams.bottomMargin = dp(28);
        root.addView(date, dateParams);

        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setOrientation(LinearLayout.HORIZONTAL);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);

        heading = text("Apps", 18, primary, true);
        headingRow.addView(heading, new LinearLayout.LayoutParams(0, -1, 1f));

        TextView settings = text("Settings", 14, secondary, false);
        settings.setGravity(Gravity.CENTER);
        settings.setPadding(dp(16), 0, 0, 0);
        settings.setBackgroundResource(android.R.drawable.list_selector_background);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                openSettings();
            }
        });
        headingRow.addView(settings, new LinearLayout.LayoutParams(-2, -1));

        LinearLayout.LayoutParams headingRowParams = new LinearLayout.LayoutParams(-1, dp(48));
        headingRowParams.bottomMargin = dp(4);
        root.addView(headingRow, headingRowParams);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        grid = new GridLayout(this);
        grid.setColumnCount(COLUMNS);
        grid.setUseDefaultMargins(false);
        scroll.addView(grid, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView footer = text("VIVO LITE  \u00b7  V0.1.1", 11, secondary, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(34)));

        setContentView(root);
    }

    // Queries the installed launchable apps. Labels and icons are loaded only
    // once per app (cached), and the grid is rebuilt only if the set of apps
    // changed since the last time it was shown.
    private void refreshApps() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved;
        try {
            resolved = pm.queryIntentActivities(query, 0);
        } catch (RuntimeException e) {
            if (heading != null) {
                heading.setText("Apps (PM Error: " + e.getClass().getSimpleName() + ")");
            }
            return;
        }

        if (resolved == null) {
            resolved = Collections.emptyList();
        }

        List<AppEntry> entries = new ArrayList<AppEntry>(resolved.size());
        int skipErrors = 0;

        for (ResolveInfo info : resolved) {
            if (info == null || info.activityInfo == null) {
                skipErrors++;
                continue;
            }
            try {
                String packageName = info.activityInfo.packageName;
                String activityName = info.activityInfo.name;
                if (packageName == null || activityName == null) {
                    skipErrors++;
                    continue;
                }
                if (packageName.equals(getPackageName())) continue;

                ComponentName component = new ComponentName(packageName, activityName);
                String key = component.flattenToString();
                String label = labelCache.get(key);
                if (label == null) {
                    CharSequence rawLabel = info.loadLabel(pm);
                    label = (rawLabel != null && rawLabel.length() > 0)
                            ? rawLabel.toString()
                            : packageName;
                    labelCache.put(key, label);
                }
                entries.add(new AppEntry(info, component, label));
            } catch (RuntimeException e) {
                skipErrors++;
            }
        }

        Collections.sort(entries, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry a, AppEntry b) {
                int byLabel = a.label.compareToIgnoreCase(b.label);
                return byLabel != 0 ? byLabel : a.key.compareTo(b.key);
            }
        });

        if (heading != null) {
            String diag = "Apps (" + entries.size() + "/" + resolved.size()
                    + (skipErrors > 0 ? " err:" + skipErrors : "") + ")";
            heading.setText(diag);
        }

        StringBuilder sig = new StringBuilder();
        Set<String> present = new HashSet<String>();
        for (AppEntry e : entries) {
            sig.append(e.key).append('\n');
            present.add(e.key);
        }
        String signature = sig.toString();
        if (signature.equals(shownSignature)) return;

        // Forget cached labels/icons of apps that are no longer installed.
        labelCache.keySet().retainAll(present);
        iconCache.keySet().retainAll(present);

        showApps(pm, entries);
        shownSignature = signature;
    }

    private void showApps(PackageManager pm, List<AppEntry> entries) {
        grid.removeAllViews();

        int totalWidth = getResources().getDisplayMetrics().widthPixels;
        int availWidth = Math.max(dp(180), totalWidth - dp(48)); // root left+right padding is 24dp * 2
        int cellWidth = availWidth / COLUMNS;

        int count = 0;
        for (final AppEntry entry : entries) {
            Drawable icon = iconCache.get(entry.key);
            if (icon == null) {
                try {
                    icon = entry.info.loadIcon(pm);
                } catch (RuntimeException e) {
                    icon = null;
                }
                if (icon == null) {
                    icon = getApplicationInfo().loadIcon(pm);
                }
                iconCache.put(entry.key, icon);
            }

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(10), dp(4), dp(10));
            item.setBackgroundResource(android.R.drawable.list_selector_background);

            ImageView image = new ImageView(this);
            image.setImageDrawable(icon);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(48), dp(48));
            imageParams.gravity = Gravity.CENTER;
            item.addView(image, imageParams);

            TextView name = text(entry.label, 12, primary, false);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(1);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, dp(28));
            nameParams.topMargin = dp(5);
            item.addView(name, nameParams);

            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    launchApp(entry.component);
                }
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(count / COLUMNS),
                    GridLayout.spec(count % COLUMNS)
            );
            params.width = cellWidth;
            params.height = dp(96);
            grid.addView(item, params);
            count++;
        }

        if (count == 0) {
            TextView empty = text("No launchable apps found.", 14, secondary, false);
            GridLayout.LayoutParams emptyParams = new GridLayout.LayoutParams(
                    GridLayout.spec(0),
                    GridLayout.spec(0, COLUMNS)
            );
            emptyParams.width = availWidth;
            emptyParams.height = dp(48);
            grid.addView(empty, emptyParams);
        }
    }

    // Launches the exact activity that was tapped (not just the package's
    // first launcher activity).
    private void launchApp(ComponentName component) {
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.setComponent(component);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(launch);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Unable to open Settings", Toast.LENGTH_SHORT).show();
        }
    }

    // A home screen has nowhere to go "back" to. Without this override, Back
    // would finish the Activity and the system would immediately relaunch it.
    @Override
    public void onBackPressed() {
        // Intentionally empty.
    }

    // Delivered when Home is pressed while this screen already exists
    // (relies on android:launchMode="singleTask" in the manifest).
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (scroll != null) scroll.smoothScrollTo(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (grid != null) refreshApps();
    }
}
