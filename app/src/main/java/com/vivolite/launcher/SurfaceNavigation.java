package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        setPadding(Ui.dp(context, 5), Ui.dp(context, 5), Ui.dp(context, 5), Ui.dp(context, 5));
        setBackground(Ui.panel(context, Ui.GLASS_LIGHT, 22));
        for (int i = 0; i < LABELS.length; i++) addItem(context, i, selected);
    }

    private void addItem(Context context, int destination, int selected) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setFocusable(true);
        item.setContentDescription(LABELS[destination]);
        boolean active = destination == selected;
        TextView glyph = new TextView(context);
        glyph.setText(GLYPHS[destination]);
        glyph.setTextSize(17);
        glyph.setTextColor(active ? Ui.primaryText() : Ui.secondaryText());
        glyph.setGravity(Gravity.CENTER);
        glyph.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        item.addView(glyph, new LayoutParams(-1, Ui.dp(context, 23)));
        TextView label = new TextView(context);
        label.setText(LABELS[destination]);
        label.setTextSize(7);
        label.setLetterSpacing(0.05f);
        label.setTextColor(active ? Ui.primaryText() : Ui.secondaryText());
        label.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        label.setGravity(Gravity.CENTER);
        item.addView(label, new LayoutParams(-1, Ui.dp(context, 14)));
        if (active) item.setBackground(Ui.panel(context, Ui.GLASS, 16));
        LayoutParams params = new LayoutParams(0, Ui.dp(context, 48), 1f);
        params.setMargins(Ui.dp(context, 2), 0, Ui.dp(context, 2), 0);
        addView(item, params);
        item.setOnClickListener(v -> {
            if (context instanceof MainActivity) ((MainActivity) context).navigateToSurface(destination);
        });
    }
}
