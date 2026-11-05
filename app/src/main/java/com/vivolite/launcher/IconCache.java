package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ColorFilter;
import android.graphics.Rect;
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
        if (bitmap != null) {
            try {
                return new BitmapDrawable(context.getResources(), bitmap);
            } catch (OutOfMemoryError exhausted) {
                // A native icon is preferable to failing the entire app-library scan.
                return source;
            }
        }

        Bitmap created = null;
        Rect previousBounds = null;
        ColorFilter previousFilter = null;
        try {
            int size = (int)(48 * context.getResources().getDisplayMetrics().density + 0.5f);
            created = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(created);
            previousBounds = new Rect(source.getBounds());
            previousFilter = source.getColorFilter();
            source.setBounds(0, 0, size, size);
            if (monochrome) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0f);
                source.setColorFilter(new ColorMatrixColorFilter(matrix));
            }
            try {
                source.draw(canvas);
            } finally {
                source.setColorFilter(previousFilter);
                source.setBounds(previousBounds);
            }

            BitmapDrawable result = new BitmapDrawable(context.getResources(), created);
            cache.put(cacheKey, created);
            return result;
        } catch (OutOfMemoryError exhausted) {
            // Rasterization can fail after bitmap allocation too (Canvas, filters,
            // drawable wrappers, or cache bookkeeping). Keep this app entry usable.
            if (created != null && !created.isRecycled()) created.recycle();
            cache.evictAll();
            return source;
        } finally {
            // Restore shared icon-pack drawables even if rasterization failed midway.
            if (previousBounds != null) {
                source.setColorFilter(previousFilter);
                source.setBounds(previousBounds);
            }
        }
    }

    public void trim() { cache.evictAll(); }
}
