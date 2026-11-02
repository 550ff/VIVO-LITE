package com.vivolite.launcher;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Window;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

/** A separate, alphabetical app library; it never changes home-screen ordering. */
public final class AppLibraryDialog extends Dialog {
    public interface Listener { void onLaunch(String id); }

    private final Context context;
    private final List<LauncherItem> apps;
    private final Listener listener;
    private final LinearLayout results;
    private final int columns;
    private final ArrayList<Cell> cells=new ArrayList<>();
    private final Map<Character,TextView> sectionTitles=new LinkedHashMap<>();
    private final Map<Character,GridLayout> sectionGrids=new LinkedHashMap<>();
    private final TextView emptyState;
    private final ScrollView appScroll;
    private final Handler searchHandler=new Handler(Looper.getMainLooper());
    private Runnable pendingRender;

    public AppLibraryDialog(Context context, List<LauncherItem> apps, Listener listener) {
        super(context);
        this.context = context;
        this.apps = new ArrayList<>(apps);
        this.listener = listener;
        columns = context.getResources().getDisplayMetrics().widthPixels / context.getResources().getDisplayMetrics().density >= 420f ? 5 : 4;

        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(context, 18), Ui.dp(context, 20), Ui.dp(context, 18), Ui.dp(context, 14));
        panel.setBackground(Ui.panel(context, Ui.GLASS, 26));

        TextView heading = text("APP LIBRARY", 12, Ui.secondaryText(), true);
        heading.setLetterSpacing(0.13f);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, Ui.dp(context, 32)));

        EditText search = new EditText(context);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(Ui.primaryText());
        search.setHintTextColor(Ui.secondaryText());
        search.setHint("Search all apps");
        search.setPadding(Ui.dp(context, 14), 0, Ui.dp(context, 14), 0);
        search.setBackground(Ui.panel(context, Ui.GLASS_LIGHT, 18));
        panel.addView(search, new LinearLayout.LayoutParams(-1, Ui.dp(context, 46)));

        HorizontalScrollView alphabetScroll=new HorizontalScrollView(context);
        alphabetScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout alphabet=new LinearLayout(context);alphabet.setGravity(Gravity.CENTER_VERTICAL);
        alphabetScroll.addView(alphabet,new HorizontalScrollView.LayoutParams(-2,Ui.dp(context,38)));
        panel.addView(alphabetScroll,new LinearLayout.LayoutParams(-1,Ui.dp(context,40)));

        appScroll = new ScrollView(context);
        appScroll.setFillViewport(false);
        appScroll.setVerticalScrollBarEnabled(false);
        results = new LinearLayout(context);
        results.setOrientation(LinearLayout.VERTICAL);
        emptyState=text("No apps installed",14,Ui.secondaryText(),false);
        emptyState.setGravity(Gravity.CENTER);
        emptyState.setVisibility(android.view.View.GONE);
        results.addView(emptyState,new LinearLayout.LayoutParams(-1,Ui.dp(context,140)));
        appScroll.addView(results, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = Ui.dp(context, 8);
        panel.addView(appScroll, scrollParams);

        setContentView(panel);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(Math.min(Ui.dp(context, 420), context.getResources().getDisplayMetrics().widthPixels - Ui.dp(context, 28)),
                    Math.min(context.getResources().getDisplayMetrics().heightPixels - Ui.dp(context, 56), Ui.dp(context, 640)));
        }

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(pendingRender!=null)searchHandler.removeCallbacks(pendingRender);
                final String query=s.toString();
                pendingRender=()->filter(query);
                searchHandler.postDelayed(pendingRender,90);
            }
            @Override public void afterTextChanged(Editable editable) { }
        });
        buildCells();
        buildAlphabet(alphabet);
        filter("");
        setOnDismissListener(dialog->{
            if(pendingRender!=null)searchHandler.removeCallbacks(pendingRender);
            results.removeAllViews();
        });
    }

    private void buildAlphabet(LinearLayout alphabet) {
        for(char letter='A';letter<='Z';letter++) addIndex(alphabet,letter);
        addIndex(alphabet,'#');
    }

    private void addIndex(LinearLayout alphabet,char letter) {
        TextView chip=text(String.valueOf(letter),11,Ui.secondaryText(),true);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,12));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.dp(context,30),Ui.dp(context,30));
        p.setMargins(Ui.dp(context,2),0,Ui.dp(context,2),0);alphabet.addView(chip,p);
        chip.setOnClickListener(v->{
            TextView target=sectionTitles.get(letter);
            if(target!=null&&target.getVisibility()==android.view.View.VISIBLE)appScroll.smoothScrollTo(0,target.getTop());
        });
        if(!sectionTitles.containsKey(letter))chip.setAlpha(0.38f);
    }

    private void buildCells() {
        ArrayList<LauncherItem> sorted=new ArrayList<>(apps);
        java.util.Collections.sort(sorted,(a,b)->String.CASE_INSENSITIVE_ORDER.compare(a.title==null?"":a.title,b.title==null?"":b.title));
        for (LauncherItem app : sorted) {
            if(app.title==null)continue;
            String title=app.title.trim();
            char initial=title.isEmpty()?'#':Character.toUpperCase(title.charAt(0));
            char section=(initial>='A'&&initial<='Z')?initial:'#';
            if(!sectionGrids.containsKey(section)) {
                TextView heading=text(String.valueOf(section),11,Ui.ACCENT,true);
                heading.setPadding(Ui.dp(context,5),Ui.dp(context,10),0,Ui.dp(context,4));
                sectionTitles.put(section,heading);results.addView(heading,new LinearLayout.LayoutParams(-1,Ui.dp(context,32)));
                GridLayout newGrid=new GridLayout(context);newGrid.setColumnCount(columns);
                sectionGrids.put(section,newGrid);results.addView(newGrid,new LinearLayout.LayoutParams(-1,-2));
            }
            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(Ui.dp(context, 2), Ui.dp(context, 10), Ui.dp(context, 2), Ui.dp(context, 6));
            ImageView icon = new ImageView(context);
            Drawable drawable = MainActivity.iconFor(app.id);
            if (drawable != null) icon.setImageDrawable(drawable);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int cellWidth = (context.getResources().getDisplayMetrics().widthPixels - Ui.dp(context, 64)) / columns;
            int size = Ui.dp(context, Math.min(Ui.ICON_DP, Math.max(36, cellWidth - Ui.dp(context, 12))));
            cell.addView(icon, new LinearLayout.LayoutParams(size, size));
            TextView label = text(title, 10, Ui.primaryText(), false);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, Ui.dp(context, 28));
            labelParams.topMargin = Ui.dp(context, 4);
            cell.addView(label, labelParams);
            cell.setOnClickListener(v -> { Ui.press(cell,true);listener.onLaunch(app.id); dismiss(); });
            cell.setOnLongClickListener(v->{
                try { context.startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:"+app.packageName)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)); }
                catch(Exception ignored) { }
                return true;
            });
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = Ui.dp(context, 86);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            sectionGrids.get(section).addView(cell, params);
            cells.add(new Cell(app,section,cell));
        }
    }

    private void filter(String query) {
        String normalized=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        Map<Character,Boolean> visibleSections=new LinkedHashMap<>();
        int visibleCount=0;
        for(Cell cell:cells) {
            String title=cell.app.title==null?"":cell.app.title.toLowerCase(Locale.ROOT);
            boolean visible=title.contains(normalized);
            cell.view.setVisibility(visible?android.view.View.VISIBLE:android.view.View.GONE);
            if(visible){visibleCount++;visibleSections.put(cell.section,true);}
        }
        for(Map.Entry<Character,GridLayout> entry:sectionGrids.entrySet()) {
            boolean visible=visibleSections.containsKey(entry.getKey());
            entry.getValue().setVisibility(visible?android.view.View.VISIBLE:android.view.View.GONE);
            sectionTitles.get(entry.getKey()).setVisibility(visible?android.view.View.VISIBLE:android.view.View.GONE);
        }
        emptyState.setText(normalized.isEmpty()?"No apps installed":"No apps match your search");
        emptyState.setVisibility(visibleCount==0?android.view.View.VISIBLE:android.view.View.GONE);
    }

    private static final class Cell {
        final LauncherItem app; final char section; final LinearLayout view;
        Cell(LauncherItem app,char section,LinearLayout view){this.app=app;this.section=section;this.view=view;}
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return view;
    }
}
