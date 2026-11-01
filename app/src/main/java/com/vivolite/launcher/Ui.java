package com.vivolite.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public final class Ui {
    public static final int WHITE = Color.rgb(245,245,247);
    public static final int MUTED = Color.rgb(157,159,166);
    public static final int GLASS = Color.argb(218, 25,26,30);
    public static final int GLASS_LIGHT = Color.argb(232, 38,39,44);
    public static final int BORDER = Color.argb(48,255,255,255);
    public static final int BLACK = Color.rgb(8,8,10);

    private Ui() {}

    public static int dp(Context c, float value) {
        return (int)(value*c.getResources().getDisplayMetrics().density+0.5f);
    }

    public static GradientDrawable panel(Context c, int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c,radius));
        d.setStroke(dp(c,1), BORDER);
        return d;
    }

    public static void enter(View view, boolean enabled, int distance) {
        if (!enabled) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(view.getContext(), distance));
        view.animate().alpha(1f).translationY(0f).setDuration(170)
            .setInterpolator(new DecelerateInterpolator()).start();
    }

    public static void press(View view, boolean enabled) {
        if (!enabled) return;
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(75)
            .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(110).start()).start();
    }
}
