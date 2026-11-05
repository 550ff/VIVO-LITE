package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.MotionEvent;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

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
    // Reuse curves: touch feedback runs often, so don't allocate interpolators per gesture.
    private static final Interpolator EASE_OUT = new PathInterpolator(0.20f, 0.80f, 0.20f, 1.00f);
    private static final Interpolator PRESS_IN = new PathInterpolator(0.30f, 0.00f, 0.70f, 0.20f);

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
        if (!enabled || view == null) return;
        // Translation is compositor-friendly. Avoid alpha here: large translucent
        // views can require off-screen buffers and extra fill-rate on older GPUs.
        view.animate().cancel();
        view.setTranslationY(dp(view.getContext(), distance));
        view.animate().translationY(0f).setDuration(210)
            .setInterpolator(EASE_OUT).start();
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
        if (!enabled || view == null || event == null) return;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            view.animate().cancel();
            view.animate().scaleX(0.975f).scaleY(0.975f)
                .setDuration(95).setInterpolator(PRESS_IN).start();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // Stop the press-in animator before reversing direction. This avoids
            // competing property animations when taps are very quick.
            view.animate().cancel();
            view.animate().scaleX(1f).scaleY(1f)
                .setDuration(175).setInterpolator(EASE_OUT).start();
        }
    }

    public static void press(View view, boolean enabled) {
        if (!enabled) return;
        view.animate().cancel();
        view.animate().scaleX(0.975f).scaleY(0.975f).setDuration(90)
            .setInterpolator(PRESS_IN)
            .withEndAction(() -> {
                if (view.getParent() == null) return;
                view.animate().scaleX(1f).scaleY(1f)
                    .setDuration(175).setInterpolator(EASE_OUT).start();
            })
            .start();
    }
}
