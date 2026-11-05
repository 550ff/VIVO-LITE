package com.vivolite.launcher;

import android.content.Context;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * Shared low-overhead motion profile for the launcher.
 *
 * Durations are expressed in display frames rather than arbitrary milliseconds.
 * Only compositor-friendly translation and scale properties are used; no
 * per-frame callbacks, alpha fades, shadows, or temporary hardware layers.
 */
public final class LauncherMotionEngine {
    private static final Interpolator PRESS_IN =
            new PathInterpolator(0.30f, 0.00f, 0.70f, 0.20f);
    private static final Interpolator EASE_OUT =
            new PathInterpolator(0.20f, 0.80f, 0.20f, 1.00f);
    private static final float PRESS_SCALE = 0.975f;
    private static float frameDurationMs = 1000f / 60f;

    private LauncherMotionEngine() {}

    /** Read the device display's refresh interval once; the Y21L profile is 60 Hz. */
    public static void configure(Context context) {
        if(context==null)return;
        try {
            WindowManager manager=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            if(manager==null)return;
            Display display=manager.getDefaultDisplay();
            if(display==null)return;
            float refreshRate=display.getRefreshRate();
            if(!Float.isNaN(refreshRate)&&!Float.isInfinite(refreshRate)
                    &&refreshRate>=30f&&refreshRate<=90f) {
                frameDurationMs=1000f/refreshRate;
            }
        } catch(RuntimeException ignored) {
            // Keep the conservative 60 Hz fallback on unusual vendor firmware.
        }
    }

    private static long frames(int count) {
        return Math.max(1L,Math.round(frameDurationMs*count));
    }

    public static void enter(View view,boolean enabled,int distanceDp) {
        if(!enabled||view==null)return;
        ViewPropertyAnimator animator=view.animate();
        animator.cancel();
        view.setTranslationY(Ui.dp(view.getContext(),distanceDp));
        animator.translationY(0f)
                .setDuration(frames(12))
                .setInterpolator(EASE_OUT)
                .start();
    }

    /** Immediate tactile response without intercepting click, drag, or long-press. */
    public static void touchFeedback(View view,MotionEvent event,boolean enabled) {
        if(!enabled||view==null||event==null)return;
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN) {
            ViewPropertyAnimator animator=view.animate();
            animator.cancel();
            animator.scaleX(PRESS_SCALE).scaleY(PRESS_SCALE)
                    .setDuration(frames(6))
                    .setInterpolator(PRESS_IN)
                    .start();
        } else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL) {
            release(view);
        }
    }

    public static void press(View view,boolean enabled) {
        if(!enabled||view==null)return;
        ViewPropertyAnimator animator=view.animate();
        animator.cancel();
        animator.scaleX(PRESS_SCALE).scaleY(PRESS_SCALE)
                .setDuration(frames(6))
                .setInterpolator(PRESS_IN)
                .withEndAction(()->{
                    if(view.getParent()!=null)release(view);
                })
                .start();
    }

    private static void release(View view) {
        ViewPropertyAnimator animator=view.animate();
        animator.cancel();
        animator.scaleX(1f).scaleY(1f)
                .setDuration(frames(10))
                .setInterpolator(EASE_OUT)
                .start();
    }
}
