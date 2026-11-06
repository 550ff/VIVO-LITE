package com.vivolite.launcher;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** A static, inexpensive translucent surface with a fine edge and a soft top highlight. */
public final class GlassDrawable extends Drawable {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF shape = new RectF();
    private final float radius;
    private final int baseColor;
    private final boolean dark;

    public GlassDrawable(int color, float radiusPx, boolean dark) {
        this.baseColor = color;
        this.radius = radiusPx;
        this.dark = dark;
        fill.setStyle(Paint.Style.FILL);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(1f);
        edge.setColor(dark ? 0x3FFFFFFF : 0x553A506B);
        sheen.setStyle(Paint.Style.FILL);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        shape.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
        int top = blend(baseColor, Color.WHITE, dark ? 0.13f : 0.20f);
        int bottom = blend(baseColor, dark ? Color.BLACK : 0xFFE7F0F8, dark ? 0.12f : 0.08f);
        fill.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom, top, bottom, Shader.TileMode.CLAMP));
        sheen.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
                dark ? 0x12FFFFFF : 0x28FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    private static int blend(int from, int to, float amount) {
        float inverse = 1f - amount;
        return Color.argb(Color.alpha(from),
                Math.round(Color.red(from) * inverse + Color.red(to) * amount),
                Math.round(Color.green(from) * inverse + Color.green(to) * amount),
                Math.round(Color.blue(from) * inverse + Color.blue(to) * amount));
    }

    @Override public void draw(Canvas canvas) {
        canvas.drawRoundRect(shape, radius, radius, fill);
        canvas.drawRoundRect(shape, radius, radius, sheen);
        canvas.drawRoundRect(shape, radius, radius, edge);
    }

    @Override public void setAlpha(int value) {
        fill.setAlpha(value);
        edge.setAlpha(value);
        sheen.setAlpha(value);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        fill.setColorFilter(filter);
        edge.setColorFilter(filter);
        sheen.setColorFilter(filter);
        invalidateSelf();
    }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
