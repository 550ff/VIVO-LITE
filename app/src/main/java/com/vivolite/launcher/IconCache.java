package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

public class IconCache {
    private final Context context;
    private final LruCache<String, Bitmap> cache;

    public IconCache(Context context) {
        this.context = context.getApplicationContext();
        int maxKb = Math.max(256, (int)(Runtime.getRuntime().maxMemory() / 1024 / 24));
        cache = new LruCache<String, Bitmap>(maxKb) {
            @Override protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    public Drawable get(String key, Drawable source) {
        return get(key, source, true);
    }

    public Drawable get(String key, Drawable source, boolean monochrome) {
        if (source == null) return new ColorDrawable(android.graphics.Color.TRANSPARENT);
        String cacheKey = key + (monochrome ? "|mono" : "|color");
        Bitmap bitmap = cache.get(cacheKey);
        if (bitmap == null) {
            int size = (int)(48 * context.getResources().getDisplayMetrics().density + 0.5f);
            bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            source.setBounds(0, 0, size, size);
            ColorFilter previous = source.getColorFilter();
            if (monochrome) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0f);
                source.setColorFilter(new ColorMatrixColorFilter(matrix));
            }
            try {
                source.draw(canvas);
            } finally {
                if (monochrome) source.setColorFilter(previous);
            }
            cache.put(cacheKey, bitmap);
        }
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public void trim() { cache.evictAll(); }
}
