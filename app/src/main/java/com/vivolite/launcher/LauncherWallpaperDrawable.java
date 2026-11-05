package com.vivolite.launcher;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Premium layered wallpaper rendered with gradients only; no bitmap or live blur. */
public final class LauncherWallpaperDrawable extends Drawable {
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ambient = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent;
    private final int preset;

    public LauncherWallpaperDrawable(int accent, int preset) {
        this.accent = accent;
        this.preset = Math.max(0, Math.min(2, preset));
        base.setStyle(Paint.Style.FILL);
        glow.setStyle(Paint.Style.FILL);
        ambient.setStyle(Paint.Style.FILL);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        int top;
        int middle;
        int bottom = 0xFF070A10;
        if (preset == 1) {
            top = 0xFF12333B;
            middle = 0xFF0D2029;
        } else if (preset == 2) {
            top = 0xFF30223E;
            middle = 0xFF1A1629;
        } else {
            top = 0xFF202E47;
            middle = 0xFF111B2B;
        }
        base.setShader(new android.graphics.LinearGradient(
            bounds.left, bounds.top, bounds.right, bounds.bottom,
            new int[]{top, middle, bottom}, new float[]{0f, 0.48f, 1f},
            Shader.TileMode.CLAMP
        ));

        int accentGlow = Color.argb(0x42, Color.red(accent), Color.green(accent), Color.blue(accent));
        int softGlow = Color.argb(0x16, Color.red(accent), Color.green(accent), Color.blue(accent));
        glow.setShader(new RadialGradient(
            bounds.left + bounds.width() * 0.84f,
            bounds.top + bounds.height() * 0.18f,
            Math.max(1f, Math.max(bounds.width(), bounds.height()) * 0.76f),
            new int[]{accentGlow, softGlow, Color.TRANSPARENT},
            new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP
        ));

        int ambientColor = preset == 1 ? 0x183F9B9B : (preset == 2 ? 0x183F2D65 : 0x18334D83);
        ambient.setShader(new RadialGradient(
            bounds.left + bounds.width() * 0.12f,
            bounds.top + bounds.height() * 0.78f,
            Math.max(1f, bounds.width() * 0.82f),
            new int[]{ambientColor, Color.TRANSPARENT},
            new float[]{0f, 1f}, Shader.TileMode.CLAMP
        ));
    }

    @Override public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), base);
        canvas.drawRect(getBounds(), ambient);
        canvas.drawRect(getBounds(), glow);
    }

    @Override public void setAlpha(int alpha) {
        base.setAlpha(alpha);
        glow.setAlpha(alpha);
        ambient.setAlpha(alpha);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        base.setColorFilter(filter);
        glow.setColorFilter(filter);
        ambient.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
