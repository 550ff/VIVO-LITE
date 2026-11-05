package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.MotionEvent;

public final class Ui {
    public static final int WHITE = Color.rgb(245, 247, 251);
    public static final int MUTED = Color.rgb(157, 170, 190);
    public static final int INK = Color.rgb(25, 35, 50);
    public static final int MUTED_INK = Color.rgb(93, 109, 131);
    public static final int ACCENT_BLUE = 0xFF91B4FF;
    public static int ACCENT = ACCENT_BLUE;
    public static int GLASS = Color.argb(226, 15, 20, 29);
    public static int GLASS_LIGHT = Color.argb(235, 27, 35, 48);
    public static final int BORDER = Color.argb(58, 255, 255, 255);
    public static final int BLACK = Color.rgb(6, 8, 12);
    public static int ICON_DP = 46;
    private static boolean darkGlass = true;

    private Ui() {}

    public static int dp(Context c, float value) {
        return (int)(value * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void configure(boolean useDarkGlass, int accent, int iconSizeDp) {
        darkGlass = useDarkGlass;
        ACCENT = accent;
        ICON_DP = Math.max(36, Math.min(64, iconSizeDp));
        if (darkGlass) {
            GLASS = Color.argb(226, 15, 20, 29);
            GLASS_LIGHT = Color.argb(235, 27, 35, 48);
        } else {
            GLASS = Color.argb(226, 238, 243, 250);
            GLASS_LIGHT = Color.argb(238, 249, 251, 255);
        }
    }

    public static int primaryText() { return darkGlass ? WHITE : INK; }
    public static int secondaryText() { return darkGlass ? MUTED : MUTED_INK; }
    public static int subtleBorder() { return darkGlass ? 0x42FFFFFF : 0x553A506B; }

    public static Drawable panel(Context c, int color, int radius) {
        return new GlassDrawable(color, dp(c, radius), darkGlass);
    }

    public static Drawable iconPlate(Context c) {
        return new GlassDrawable(GLASS_LIGHT, dp(c, 15), darkGlass);
    }

    public static void enter(View view, boolean enabled, int distance) {
        LauncherMotionEngine.enter(view,enabled,distance);
    }

    /** Immediate visual response while the finger is down; returns false so normal click/long-click handling continues. */
    public static void attachTouchFeedback(View view, boolean enabled) {
        if (view == null || !enabled) return;
        view.setOnTouchListener((target, event) -> {
            touchFeedback(target, event, true);
            return false;
        });
    }

    /** Use inside an existing touch listener when a view also handles gestures or drag-and-drop. */
    public static void touchFeedback(View view, MotionEvent event, boolean enabled) {
        LauncherMotionEngine.touchFeedback(view,event,enabled);
    }

    public static void press(View view, boolean enabled) {
        LauncherMotionEngine.press(view,enabled);
    }
}
