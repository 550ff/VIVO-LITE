package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

/** Shared, lightweight navigation for the launcher-owned full-screen surfaces. */
final class SurfaceNavigation extends LinearLayout {
    static final int HOME = 0;
    static final int LIBRARY = 1;
    static final int NOTIFICATIONS = 2;
    static final int CONTROL = 3;
    static final int SETTINGS = 4;

    private static final String[] LABELS = {"HOME", "APPS", "ALERTS", "CONTROL", "STYLE"};
    private static final String[] GLYPHS = {"⌂", "▦", "◌", "◉", "⚙"};

    SurfaceNavigation(Context context, int selected) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setPadding(Ui.dp(context, 6), Ui.dp(context, 6), Ui.dp(context, 6), Ui.dp(context, 6));
        setBackground(Ui.panel(context, Ui.GLASS_LIGHT, 24));
        setElevation(Ui.dp(context, 5));
        for (int i = 0; i < LABELS.length; i++) addItem(context, i, selected);
    }

    private void addItem(Context context, int destination, int selected) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setFocusable(true);
        item.setClickable(true);
        item.setContentDescription(LABELS[destination]);
        boolean active = destination == selected;

        TextView glyph = new TextView(context);
        glyph.setText(GLYPHS[destination]);
        glyph.setTextSize(18);
        glyph.setTextColor(active ? Ui.ACCENT : Ui.secondaryText());
        glyph.setGravity(Gravity.CENTER);
        glyph.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        item.addView(glyph, new LayoutParams(-1, Ui.dp(context, 24)));

        TextView label = new TextView(context);
        label.setText(LABELS[destination]);
        label.setTextSize(7);
        label.setLetterSpacing(0.075f);
        label.setTextColor(active ? Ui.primaryText() : Ui.secondaryText());
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL),
            active ? Typeface.BOLD : Typeface.NORMAL);
        label.setGravity(Gravity.CENTER);
        item.addView(label, new LayoutParams(-1, Ui.dp(context, 13)));

        if (active) {
            GradientDrawable selectedSurface = new GradientDrawable();
            selectedSurface.setColor(Ui.GLASS);
            selectedSurface.setCornerRadius(Ui.dp(context, 17));
            selectedSurface.setStroke(Ui.dp(context, 1), Ui.subtleBorder());
            item.setBackground(selectedSurface);
            View indicator = new View(context);
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(Ui.ACCENT);
            indicator.setBackground(dot);
            LayoutParams dotParams = new LayoutParams(Ui.dp(context, 4), Ui.dp(context, 4));
            dotParams.topMargin = Ui.dp(context, 2);
            item.addView(indicator, dotParams);
        }

        LayoutParams params = new LayoutParams(0, Ui.dp(context, 52), 1f);
        params.setMargins(Ui.dp(context, 2), 0, Ui.dp(context, 2), 0);
        addView(item, params);
        item.setOnClickListener(v -> {
            Ui.press(item, true);
            if (context instanceof MainActivity) {
                ((MainActivity) context).navigateToSurface(destination);
            }
        });
    }
}
