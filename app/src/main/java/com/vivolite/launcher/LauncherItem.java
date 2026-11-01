package com.vivolite.launcher;

import java.util.ArrayList;
import java.util.List;

public class LauncherItem {
    public static final int TYPE_APP = 0;
    public static final int TYPE_FOLDER = 1;

    public int type = TYPE_APP;
    public String id;
    public String title;
    public String packageName;
    public String className;
    public final List<String> children = new ArrayList<>();

    public static LauncherItem app(String id, String title, String pkg, String cls) {
        LauncherItem item = new LauncherItem();
        item.type = TYPE_APP; item.id = id; item.title = title;
        item.packageName = pkg; item.className = cls;
        return item;
    }

    public static LauncherItem folder(String id, String title) {
        LauncherItem item = new LauncherItem();
        item.type = TYPE_FOLDER; item.id = id; item.title = title;
        return item;
    }

    public boolean isFolder() { return type == TYPE_FOLDER; }
}
