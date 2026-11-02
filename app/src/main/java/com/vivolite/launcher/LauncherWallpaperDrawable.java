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

/** Static layered gradients; no bitmap, blur pass, or continuously running animation. */
public final class LauncherWallpaperDrawable extends Drawable {
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent;
    private final int preset;

    public LauncherWallpaperDrawable(int accent, int preset) {
        this.accent = accent;
        this.preset = preset;
        base.setStyle(Paint.Style.FILL);
        glow.setStyle(Paint.Style.FILL);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        int top = preset == 1 ? 0xFF173244 : (preset == 2 ? 0xFF30213D : 0xFF27354D);
        int middle = preset == 1 ? 0xFF101F2A : (preset == 2 ? 0xFF1A1525 : 0xFF151C2A);
        base.setShader(new android.graphics.LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                new int[]{top, middle, 0xFF080C14}, new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP));
        int neutral = Color.rgb(220,224,232);
        int glowColor = (0x22 << 24) | (neutral & 0x00FFFFFF);
        int softColor = (0x0C << 24) | (neutral & 0x00FFFFFF);
        glow.setShader(new RadialGradient(bounds.left + bounds.width() * 0.78f,
                bounds.top + bounds.height() * 0.22f, Math.max(bounds.width(), bounds.height()) * 0.82f,
                new int[]{glowColor, softColor, Color.TRANSPARENT}, new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), base);
        canvas.drawRect(getBounds(), glow);
    }

    @Override public void setAlpha(int alpha) { base.setAlpha(alpha); glow.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { base.setColorFilter(filter); glow.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
