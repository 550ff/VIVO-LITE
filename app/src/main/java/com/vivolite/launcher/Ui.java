package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public final class Ui {
    public static final int WHITE = Color.rgb(247,249,253);
    public static final int MUTED = Color.rgb(183,194,210);
    public static final int INK = Color.rgb(37,48,66);
    public static final int MUTED_INK = Color.rgb(101,115,134);
    public static final int ACCENT_BLUE = Color.rgb(232,235,240);
    public static int ACCENT = ACCENT_BLUE;
    public static int GLASS = Color.argb(210, 31,34,40);
    public static int GLASS_LIGHT = Color.argb(225, 48,51,58);
    public static final int BORDER = Color.argb(118,255,255,255);
    public static final int BLACK = Color.rgb(8,8,10);
    public static int ICON_DP = 46;
    private static boolean darkGlass;

    private Ui() {}

    public static int dp(Context c, float value) {
        return (int)(value*c.getResources().getDisplayMetrics().density+0.5f);
    }

    public static void configure(boolean useDarkGlass, int accent, int iconSizeDp) {
        darkGlass = useDarkGlass;
        ACCENT = ACCENT_BLUE;
        ICON_DP = iconSizeDp;
        if (darkGlass) {
            GLASS = Color.argb(224, 20,21,24);
            GLASS_LIGHT = Color.argb(232, 43,45,50);
        } else {
            GLASS = Color.argb(218, 44,46,51);
            GLASS_LIGHT = Color.argb(232, 62,64,70);
        }
    }

    public static int primaryText() { return darkGlass ? WHITE : INK; }
    public static int secondaryText() { return darkGlass ? MUTED : MUTED_INK; }

    public static Drawable panel(Context c, int color, int radius) {
        return new GlassDrawable(color, dp(c,radius), darkGlass);
    }

    public static void enter(View view, boolean enabled, int distance) {
        if (!enabled) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(view.getContext(), distance));
        view.animate().alpha(1f).translationY(0f).setDuration(145)
            .setInterpolator(new DecelerateInterpolator()).start();
    }

    public static void press(View view, boolean enabled) {
        if (!enabled) return;
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(75)
            .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(110).start()).start();
    }
}
