package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
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
        Bitmap bitmap = cache.get(key);
        if (bitmap == null) {
            int size = (int)(48 * context.getResources().getDisplayMetrics().density + 0.5f);
            bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            source.setBounds(0, 0, size, size);
            source.draw(canvas);
            cache.put(key, bitmap);
        }
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public void trim() { cache.evictAll(); }
}
