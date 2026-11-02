package com.vivolite.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final Map<String,Drawable> DRAWABLES = new HashMap<>();
    private LauncherStorage storage;
    private IconCache iconCache;
    private final ArrayList<LauncherItem> apps = new ArrayList<>();
    private final ArrayList<LauncherItem> homeItems = new ArrayList<>();
    private final Set<String> hidden = new HashSet<>();
    private final Set<String> favorites = new HashSet<>();
    private LinearLayout root, dock;
    private GridLayout grid;
    private EditText search;
    private TextClock clock, date;
    private TextView count;
    private FolderDialog activeFolderDialog;
    private AppLibraryScreen activeLibraryScreen;
    private View activeSurfaceView;
    private NotificationScreen activeNotificationScreen;
    private int activeSurface = SurfaceNavigation.HOME;
    private int columns = 4;
    private boolean receiverRegistered = false;
    private boolean iconsTrimmed = false;
    private boolean activityResumed = false;
    private boolean packagesDirty = false;
    private String draggingId;
    private String armedDragId;
    private final Handler packageRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable packageRefresh = () -> {
        if(activityResumed)loadApps();else packagesDirty=true;
    };
    private final Handler searchRenderHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRender;

    public static Drawable iconFor(String id) { return DRAWABLES.get(id); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= 21) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        storage = new LauncherStorage(this);
        iconCache = new IconCache(this);
        columns = storage.getColumns();
        applyAppearance();
        hidden.addAll(storage.getHidden());
        favorites.addAll(storage.getFavorites());
        buildHome();
        loadApps();
        registerPackageReceiver();
    }

    private void buildHome() {
        dock = null;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this,22),Ui.dp(this,15),Ui.dp(this,22),Ui.dp(this,9));
        root.setBackground(new LauncherWallpaperDrawable(storage.getAccent(),storage.getWallpaper()));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brandBlock=new LinearLayout(this);
        brandBlock.setOrientation(LinearLayout.VERTICAL);
        brandBlock.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("VIVO LITE",12,Ui.WHITE,true);
        brand.setLetterSpacing(0.15f);
        brandBlock.addView(brand);
        TextView strap=text("A CALMER HOME",8,Ui.MUTED,false);
        strap.setLetterSpacing(0.12f);
        brandBlock.addView(strap);
        top.addView(brandBlock,new LinearLayout.LayoutParams(0,Ui.dp(this,38),1));
        TextView style = pill("STYLE");
        LinearLayout.LayoutParams styleP=new LinearLayout.LayoutParams(Ui.dp(this,62),Ui.dp(this,34));
        styleP.rightMargin=Ui.dp(this,7);top.addView(style,styleP);
        style.setOnClickListener(v->settings());
        TextView library = pill("LIBRARY");
        LinearLayout.LayoutParams libraryP=new LinearLayout.LayoutParams(Ui.dp(this,70),Ui.dp(this,34));
        libraryP.rightMargin=Ui.dp(this,7);top.addView(library,libraryP);
        library.setOnClickListener(v->openLibrary());
        TextView control = pill("CONTROL");
        top.addView(control,new LinearLayout.LayoutParams(Ui.dp(this,82),Ui.dp(this,34)));
        control.setOnClickListener(v->showControlCenter());
        root.addView(top);

        clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm"); clock.setFormat12Hour("h:mm");
        clock.setGravity(Gravity.CENTER);
        clock.setTextSize(58); clock.setTextColor(Ui.WHITE);
        clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        final float[] clockTouchY={0f};
        clock.setOnTouchListener((view,event)->{
            if(event.getAction()==MotionEvent.ACTION_DOWN){clockTouchY[0]=event.getY();return true;}
            if(event.getAction()==MotionEvent.ACTION_UP){
                float delta=event.getY()-clockTouchY[0];
                if(Math.abs(delta)>Ui.dp(this,42)) {
                    if(delta<0)openLibrary();
                    else showControlCenter();
                }
                return true;
            }
            return true;
        });
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,Ui.dp(this,70));
        cp.topMargin=Ui.dp(this,11); root.addView(clock,cp);

        date = new TextClock(this);
        date.setGravity(Gravity.CENTER);
        date.setFormat24Hour("EEEE  ·  d MMMM"); date.setFormat12Hour("EEEE  ·  d MMMM");
        date.setTextSize(13); date.setTextColor(Ui.MUTED);
        date.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,Ui.dp(this,25));
        dp.bottomMargin=Ui.dp(this,16); root.addView(date,dp);
        if(!storage.showClock()){clock.setVisibility(View.GONE);date.setVisibility(View.GONE);}

        search = new EditText(this);
        search.setSingleLine(true); search.setTextSize(14); search.setTextColor(Ui.primaryText());
        search.setHintTextColor(Ui.secondaryText()); search.setHint("⌕   Search your apps");
        search.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);
        search.setBackground(Ui.panel(this,Ui.GLASS_LIGHT,22));
        search.setCompoundDrawablePadding(Ui.dp(this,8));
        root.addView(search,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("YOUR APPS",10,Ui.MUTED,true);
        title.setLetterSpacing(0.14f);
        heading.addView(title,new LinearLayout.LayoutParams(0,Ui.dp(this,38),1));
        count=text("0 ITEMS",9,Ui.MUTED,false); count.setLetterSpacing(0.06f); heading.addView(count);
        root.addView(heading);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        grid=new GridLayout(this); grid.setColumnCount(columns);
        scroll.addView(grid,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        if(storage.showDock()) {
            dock=new LinearLayout(this);
            dock.setGravity(Gravity.CENTER_VERTICAL);
            dock.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,8));
            dock.setBackground(Ui.panel(this,Ui.GLASS_LIGHT,26));
            LinearLayout.LayoutParams dpock=new LinearLayout.LayoutParams(-1,Ui.dp(this,68));
            dpock.setMargins(Ui.dp(this,8),Ui.dp(this,11),Ui.dp(this,8),0); root.addView(dock,dpock);
        }

        LinearLayout.LayoutParams navigationParams=new LinearLayout.LayoutParams(-1,Ui.dp(this,58));
        navigationParams.topMargin=Ui.dp(this,7);
        root.addView(new SurfaceNavigation(this,SurfaceNavigation.HOME),navigationParams);
        setContentView(root);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int c){scheduleSearchRender(s.toString());}
            public void afterTextChanged(Editable e){}
        });
    }

    private void scheduleSearchRender(String query) {
        if(searchRender!=null)searchRenderHandler.removeCallbacks(searchRender);
        searchRender=()->render(query,false,false);
        searchRenderHandler.postDelayed(searchRender,100);
    }

    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }

    private TextView pill(String value) {
        TextView t=text(value,9,Ui.primaryText(),true);t.setGravity(Gravity.CENTER);
        t.setLetterSpacing(0.04f);
        t.setBackground(Ui.panel(this,Ui.GLASS_LIGHT,18));return t;
    }

    private void applyAppearance() {
        Ui.configure(storage.darkGlass(),storage.getAccent(),storage.getIconSize());
        int accent=storage.getAccent();
        int accentStyle=accent==0xFFB49AFF?com.vivolite.launcher.R.style.AccentViolet:
                accent==0xFF83D7C2?com.vivolite.launcher.R.style.AccentMint:
                accent==0xFFFFA6B7?com.vivolite.launcher.R.style.AccentRose:com.vivolite.launcher.R.style.AccentGlacier;
        getTheme().applyStyle(accentStyle,true);
        getTheme().applyStyle(storage.darkGlass()?com.vivolite.launcher.R.style.UseDarkAlerts:
                com.vivolite.launcher.R.style.UseLightAlerts,true);
        getWindow().setStatusBarColor(Color.rgb(13,19,30));
        getWindow().setNavigationBarColor(Color.rgb(13,19,30));
        if(root!=null)root.setBackground(new LauncherWallpaperDrawable(storage.getAccent(),storage.getWallpaper()));
    }

    private void loadApps() {
        apps.clear(); DRAWABLES.clear();
        PackageManager pm=getPackageManager();
        Intent query=new Intent(Intent.ACTION_MAIN);query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found=pm.queryIntentActivities(query,0);
        Set<String> seen=new HashSet<>();
        for(ResolveInfo r:found) {
            if(r.activityInfo==null || r.activityInfo.packageName.equals(getPackageName()))continue;
            String pkg=r.activityInfo.packageName, cls=r.activityInfo.name, id=pkg+"/"+cls;
            if(!seen.add(id))continue;
            LauncherItem item=LauncherItem.app(id,String.valueOf(r.loadLabel(pm)),pkg,cls);
            apps.add(item);
            Drawable drawable=r.loadIcon(pm);
            DRAWABLES.put(id,iconCache.get(id,drawable));
        }
        Collections.sort(apps,new Comparator<LauncherItem>() {
            public int compare(LauncherItem a,LauncherItem b){return a.title.compareToIgnoreCase(b.title);}
        });
        restoreHomeItems();
        render(search==null?"":search.getText().toString());
    }

    private void restoreHomeItems() {
        homeItems.clear();
        Map<String,LauncherItem> byId=new HashMap<>();
        for(LauncherItem a:apps)byId.put(a.id,a);
        List<String> order=storage.getOrder("home_order");
        JSONObject folders=storage.getFolders();
        Set<String> consumed=new HashSet<>();
        Map<String,LauncherItem> restoredFolders=new HashMap<>();

        // Claim each installed app for at most one folder before restoring top-level entries.
        for(String id:order) {
            if(id.startsWith("folder:")) {
                if(restoredFolders.containsKey(id))continue;
                try {
                    JSONObject data=folders.optJSONObject(id);
                    if(data!=null) {
                        LauncherItem folder=LauncherItem.folder(id,data.optString("name","Folder"));
                        JSONArray children=data.optJSONArray("children");
                        if(children!=null)for(int i=0;i<children.length();i++) {
                            String child=children.optString(i);
                            if(byId.containsKey(child)&&consumed.add(child))folder.children.add(child);
                        }
                        restoredFolders.put(id,folder);
                    }
                } catch(Exception ignored) {}
            }
        }
        Set<String> added=new HashSet<>();
        for(String id:order) {
            if(id.startsWith("folder:")) {
                LauncherItem folder=restoredFolders.get(id);
                if(folder!=null&&!added.contains(id)) {
                    homeItems.add(folder);
                    added.add(id);
                }
            } else if(byId.containsKey(id)&&!consumed.contains(id)&&added.add(id)) {
                homeItems.add(byId.get(id));
            }
        }
        for(LauncherItem a:apps)if(!consumed.contains(a.id)&&!contains(homeItems,a.id))homeItems.add(a);
        saveHomeOrder();
    }

    private boolean contains(List<LauncherItem> list,String id) {
        for(LauncherItem item:list)if(item.id.equals(id))return true;
        return false;
    }

    private void saveHomeOrder() {
        ArrayList<String> order=new ArrayList<>();
        for(LauncherItem item:homeItems)order.add(item.id);
        storage.setOrder("home_order",order);
        JSONObject folders=new JSONObject();
        for(LauncherItem item:homeItems)if(item.isFolder()) {
            try {
                JSONObject data=new JSONObject();data.put("name",item.title);
                JSONArray children=new JSONArray();for(String id:item.children)children.put(id);
                data.put("children",children);folders.put(item.id,data);
            } catch(Exception ignored) {}
        }
        storage.setFolders(folders);
    }

    private void render(String filter) {
        render(filter,true,true);
    }

    private void render(String filter,boolean animate,boolean refreshDock) {
        if(grid==null)return;
        grid.removeAllViews();
        ArrayList<LauncherItem> visible=new ArrayList<>();
        String q=filter==null?"":filter.toLowerCase(Locale.ROOT).trim();
        for(LauncherItem item:homeItems) {
            if(item.isFolder()) {
                boolean match=item.title.toLowerCase(Locale.ROOT).contains(q);
                if(q.isEmpty()||match)visible.add(item);
                else for(String id:item.children) {
                    LauncherItem a=findApp(id);
                    if(a!=null&&!hidden.contains(id)&&a.title.toLowerCase(Locale.ROOT).contains(q)){visible.add(item);break;}
                }
            } else if(!hidden.contains(item.id)&&item.title.toLowerCase(Locale.ROOT).contains(q))visible.add(item);
        }
        count.setText(visible.size()+" ITEMS");
        int totalWidth=getResources().getDisplayMetrics().widthPixels-Ui.dp(this,40);
        int cell=totalWidth/columns;
        grid.setColumnCount(columns);
        for(int i=0;i<visible.size();i++) {
            LauncherItem item=visible.get(i);
            LinearLayout cellView=makeCell(item,animate&&q.isEmpty()&&i<columns);
            GridLayout.LayoutParams p=new GridLayout.LayoutParams();
            p.width=cell;p.height=Ui.dp(this,104);
            p.setMargins(0,0,0,0);
            grid.addView(cellView,p);
        }
        if(refreshDock)renderDock();
    }

    private LinearLayout makeCell(LauncherItem item,boolean animate) {
        LinearLayout cell=new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);
        cell.setPadding(Ui.dp(this,2),Ui.dp(this,8),Ui.dp(this,2),Ui.dp(this,4));
        View iconView;
        if(item.isFolder()) iconView=folderPreview(item);
        else {
            ImageView icon=new ImageView(this);
            Drawable d=DRAWABLES.get(item.id);
            if(d!=null)icon.setImageDrawable(d);
            icon.setBackground(Ui.iconPlate(this));
            icon.setPadding(Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3));
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int available= Math.max(32,(getResources().getDisplayMetrics().widthPixels-Ui.dp(this,40))/columns-8);
            int iconSize=Ui.dp(this,Math.min(Ui.ICON_DP,available));
            iconView=icon;
            cell.addView(iconView,new LinearLayout.LayoutParams(iconSize,iconSize));
        }
        if(item.isFolder())cell.addView(iconView,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,46)));
        TextView name=text(item.title,10,Ui.WHITE,false);
        name.setGravity(Gravity.CENTER);name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,Ui.dp(this,25));
        np.topMargin=Ui.dp(this,4);cell.addView(name,np);
        cell.setOnClickListener(v->{
            Ui.press(cell,storage.animations());
            if(item.isFolder())openFolder(item);else launch(item.id);
        });
        cell.setOnLongClickListener(v->{showItemMenu(item);return true;});
        cell.setOnTouchListener((v,event)->{
            if(event.getAction()==MotionEvent.ACTION_DOWN && item.id.equals(armedDragId)) {
                android.content.ClipData data = android.content.ClipData.newPlainText("VIVO_LITE_ITEM", item.id);
                v.startDrag(data, new View.DragShadowBuilder(v), item.id, 0);
                armedDragId = null;
                return true;
            }
            return false;
        });
        cell.setOnDragListener((v,event)->handleDrop(v,item,event));
        if(item.isFolder())cell.setOnClickListener(v->openFolder(item));
        if(animate&&storage.animations())Ui.enter(cell,true,4);
        return cell;
    }

    private View folderPreview(LauncherItem folder) {
        GridLayout preview=new GridLayout(this);preview.setColumnCount(2);
        preview.setPadding(Ui.dp(this,5),Ui.dp(this,5),Ui.dp(this,5),Ui.dp(this,5));
        preview.setBackground(Ui.panel(this,Ui.GLASS_LIGHT,13));
        for(int i=0;i<4;i++) {
            ImageView image=new ImageView(this);
            if(i<folder.children.size()) {
                Drawable d=DRAWABLES.get(folder.children.get(i));
                if(d!=null)image.setImageDrawable(d);
            }
            GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=Ui.dp(this,16);p.height=Ui.dp(this,16);
            preview.addView(image,p);
        }
        return preview;
    }

    private boolean handleDrop(View target,LauncherItem targetItem,android.view.DragEvent event) {
        switch(event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED: return true;
            case DragEvent.ACTION_DRAG_ENTERED: target.setAlpha(0.65f);return true;
            case DragEvent.ACTION_DRAG_EXITED: target.setAlpha(1f);return true;
            case DragEvent.ACTION_DROP:
                target.setAlpha(1f);
                String source=(String)event.getLocalState();
                if(source==null||source.equals(targetItem.id))return true;
                LauncherItem sourceItem=findHome(source);
                if(sourceItem==null)return true;
                if(targetItem.isFolder()) {
                    if(!sourceItem.isFolder()) {
                        for(LauncherItem folder:homeItems) {
                            if(folder.isFolder()&&folder!=targetItem)folder.children.remove(source);
                        }
                        if(!targetItem.children.contains(source))targetItem.children.add(source);
                        homeItems.remove(sourceItem);
                    }
                } else if(!sourceItem.isFolder()) {
                    // Center drop merges; edge drop reorders.
                    float x=event.getX()/Math.max(1,target.getWidth());
                    if(x>0.25f&&x<0.75f) {
                        String folderId;
                        do { folderId="folder:"+UUID.randomUUID().toString(); }
                        while(findHome(folderId)!=null);
                        LauncherItem folder=LauncherItem.folder(folderId,"Folder");
                        folder.children.add(source);folder.children.add(targetItem.id);
                        int index=homeItems.indexOf(targetItem);
                        homeItems.remove(sourceItem);index=homeItems.indexOf(targetItem);
                        homeItems.remove(targetItem);homeItems.add(Math.max(0,index),folder);
                    } else {
                        int from=homeItems.indexOf(sourceItem),to=homeItems.indexOf(targetItem);
                        homeItems.remove(sourceItem);if(from<to)to--;
                        if(x>=0.75f)to++;
                        homeItems.add(Math.max(0,Math.min(to,homeItems.size())),sourceItem);
                    }
                }
                saveHomeOrder();render(search.getText().toString());return true;
            case DragEvent.ACTION_DRAG_ENDED: target.setAlpha(1f);return true;
        }
        return true;
    }

    private LauncherItem findHome(String id) {
        for(LauncherItem item:homeItems)if(item.id.equals(id))return item;
        return null;
    }

    private LauncherItem findApp(String id) {
        for(LauncherItem item:apps)if(item.id.equals(id))return item;
        return null;
    }

    private void showItemMenu(LauncherItem item) {
        String[] options=item.isFolder()?new String[]{"Open folder","Rename folder","Delete folder"}:
            new String[]{"Open","Add/remove favourite","App info","Hide app","Start moving"};
        new AlertDialog.Builder(this).setTitle(item.title).setItems(options,(d,which)->{
            if(item.isFolder()) {
                if(which==0)openFolder(item);
                if(which==1)renameFolder(item);
                if(which==2){restoreFolderChildren(item);homeItems.remove(item);saveHomeOrder();render("");}
            } else {
                if(which==0)launch(item.id);
                if(which==1){if(!favorites.add(item.id))favorites.remove(item.id);storage.setFavorites(favorites);render(search.getText().toString());}
                if(which==2)openAppInfo(item);
                if(which==3){hidden.add(item.id);storage.setHidden(hidden);render(search.getText().toString());}
                if(which==4)startMove(item);
            }
        }).show();
    }

    private void restoreFolderChildren(LauncherItem folder) {
        int index=Math.max(0,homeItems.indexOf(folder));
        for(String child:folder.children) {
            LauncherItem app=findApp(child);
            if(app!=null&&!contains(homeItems,child)&&!isChildOfAnotherFolder(child,folder)) {
                homeItems.add(Math.min(index++,homeItems.size()),app);
            }
        }
    }

    private boolean isChildOfAnotherFolder(String appId,LauncherItem except) {
        for(LauncherItem item:homeItems) {
            if(item.isFolder()&&item!=except&&item.children.contains(appId))return true;
        }
        return false;
    }

    private void startMove(LauncherItem item) {
        armedDragId = item.id;
        Toast.makeText(this,"Now touch this app and drag it onto another item",Toast.LENGTH_LONG).show();
    }

    private void openFolder(LauncherItem folder) {
        ArrayList<String> previousChildren=new ArrayList<>(folder.children);
        FolderDialog dialog=new FolderDialog(this,folder,apps,iconCache,storage.animations(),new FolderDialog.Listener(){
            public void onChanged(String title,List<String> children) {
                int index=Math.max(0,homeItems.indexOf(folder)+1);
                for(String child:previousChildren) {
                    if(!children.contains(child)&&!isChildOfAnotherFolder(child,folder)&&!contains(homeItems,child)) {
                        LauncherItem app=findApp(child);
                        if(app!=null)homeItems.add(Math.min(index++,homeItems.size()),app);
                    }
                }
                previousChildren.clear();
                previousChildren.addAll(children);
                folder.title=title.isEmpty()?"Folder":title;
                saveHomeOrder();render(search.getText().toString());
            }
            public void onLaunch(String key){launch(key);}
        });
        activeFolderDialog=dialog;
        dialog.setOnDismissListener(d->{if(activeFolderDialog==dialog)activeFolderDialog=null;});
        dialog.getWindow().setWindowAnimations(storage.animations()?com.vivolite.launcher.R.style.VivoDialogAnimation:0);
        dialog.show();
    }

    private void renameFolder(LauncherItem folder) {
        EditText input=new EditText(this);input.setText(folder.title);input.setTextColor(Ui.primaryText());
        input.setHintTextColor(Ui.secondaryText());
        new AlertDialog.Builder(this).setTitle("Rename folder").setView(input)
            .setPositiveButton("SAVE",(d,w)->{folder.title=input.getText().toString().trim();if(folder.title.isEmpty())folder.title="Folder";saveHomeOrder();render("");})
            .setNegativeButton("CANCEL",null).show();
    }

    private void launch(String id) {
        LauncherItem a=findApp(id);
        if(a==null)return;
        if(activeSurface!=SurfaceNavigation.HOME)navigateToSurface(SurfaceNavigation.HOME);
        Intent intent=new Intent(Intent.ACTION_MAIN);intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(a.packageName,a.className));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(intent);
            if(storage.animations())overridePendingTransition(com.vivolite.launcher.R.anim.app_enter,com.vivolite.launcher.R.anim.app_exit);
        } catch(Exception e) { Toast.makeText(this,"Unable to open app",Toast.LENGTH_SHORT).show(); }
    }

    private void openAppInfo(LauncherItem a) {
        try {startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.packageName)));}
        catch(Exception ignored){}
    }

    private void renderDock() {
        if(dock==null)return;
        dock.removeAllViews();
        int shown=0;
        for(String id:storage.getOrder("dock_order")) {
            LauncherItem app=findApp(id);
            if(app!=null&&favorites.contains(id)&&!hidden.contains(id)) {addDockIcon(app);shown++;}
        }
        for(LauncherItem app:apps)if(favorites.contains(app.id)&&!hidden.contains(app.id)&&!dockContains(app.id)) {addDockIcon(app);shown++;}
        if(shown==0) {
            TextView hint=text("LONG-PRESS AN APP  ·  ADD TO FAVOURITES",9,Ui.secondaryText(),false);
            hint.setGravity(Gravity.CENTER);dock.addView(hint,new LinearLayout.LayoutParams(-1,-1));
        }
    }

    private boolean dockContains(String id) {
        for(int i=0;i<dock.getChildCount();i++)if(id.equals(dock.getChildAt(i).getTag()))return true;
        return false;
    }

    private void addDockIcon(LauncherItem app) {
        ImageView icon=new ImageView(this);icon.setTag(app.id);
        icon.setBackground(Ui.iconPlate(this));
        icon.setPadding(Ui.dp(this,4),Ui.dp(this,4),Ui.dp(this,4),Ui.dp(this,4));
        icon.setImageDrawable(DRAWABLES.get(app.id));icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,Math.min(Ui.ICON_DP,48)),1);
        dock.addView(icon,p);
        icon.setOnClickListener(v->{Ui.press(icon,storage.animations());launch(app.id);});
        icon.setOnLongClickListener(v->{
            new AlertDialog.Builder(this).setItems(new String[]{"Move left","Move right","Remove favourite"},(d,n)->{
                ArrayList<String> order=new ArrayList<>(storage.getOrder("dock_order"));
                if(order.isEmpty())for(int i=0;i<dock.getChildCount();i++)order.add((String)dock.getChildAt(i).getTag());
                int index=order.indexOf(app.id);
                if(n==2){favorites.remove(app.id);storage.setFavorites(favorites);}
                else if(index>=0){int next=Math.max(0,Math.min(order.size()-1,index+(n==0?-1:1)));Collections.swap(order,index,next);storage.setOrder("dock_order",order);}
                renderDock();
            }).show();return true;
        });
    }

    private void registerPackageReceiver() {
        IntentFilter filter=new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);filter.addDataScheme("package");
        try {registerReceiver(packageReceiver,filter);receiverRegistered=true;}catch(Exception ignored){}
    }

    private final BroadcastReceiver packageReceiver=new BroadcastReceiver() {
        @Override public void onReceive(Context context,Intent intent) {
            packageRefreshHandler.removeCallbacks(packageRefresh);
            packageRefreshHandler.postDelayed(packageRefresh,500);
        }
    };

    private void settings() {
        String[] sections={"Personalisation","Home screen","Motion & performance","Notifications","Android system"};
        String[] descriptions={"Wallpaper, accent, glass and icons","Grid, clock and dock","Animation and app management","Notification access and live alerts","Device settings shortcuts"};
        SettingsScreen screen=new SettingsScreen(this,sections,descriptions,storage.animations(),new SettingsScreen.Listener(){
            public String[] options(int category){return settingsOptions(category);}
            public String[] descriptions(int category){return settingsDescriptions(category);}
            public void onSelect(int category,int option){applySetting(category,option);}
        });
        showSurface(screen,SurfaceNavigation.SETTINGS);
    }

    private String[] settingsOptions(int section) {
        if(section==0) return new String[]{"Wallpaper · "+wallpaperName(storage.getWallpaper()),
                "Appearance · "+(storage.darkGlass()?"Dark glass":"Light glass"),"Icon size · "+storage.getIconSize()+" dp"};
        if(section==1)return new String[]{"Grid columns · "+columns,"Clock and date · "+(storage.showClock()?"Shown":"Hidden"),"Dock · "+(storage.showDock()?"Shown":"Hidden")};
        if(section==2)return new String[]{"Animations · "+(storage.animations()?"On":"Off"),"Restore hidden apps","Refresh app library"};
        if(section==3)return new String[]{"Open notifications","Notification access settings"};
        return new String[]{"Android Home settings","Android display settings","Android sound settings"};
    }

    private String[] settingsDescriptions(int section) {
        if(section==0)return new String[]{"Choose one of the included static gradient wallpapers.","Switch between light and dark glass surfaces.","Choose the displayed app icon size. Accent colours are intentionally monochrome."};
        if(section==1)return new String[]{"Toggle between four and five home grid columns.","Show or hide the home clock and date.","Show or hide the favourites dock."};
        if(section==2)return new String[]{"Enable or disable launcher page and interaction animations.","Make hidden apps visible again.","Re-query installed launchable apps."};
        if(section==3)return new String[]{"View notifications only when Android Notification Access is enabled.","Open Android's notification access settings."};
        return new String[]{"Open Android Home app selection.","Open Android display settings.","Open Android sound settings."};
    }

    private void applySetting(int section,int index) {
        if(section==0) {
            if(index==0)chooseWallpaper();
            else if(index==1){storage.setDarkGlass(!storage.darkGlass());applyAppearance();rebuildHomeUi();}
            else chooseIconSize();
        } else if(section==1) {
            if(index==0){columns=columns==4?5:4;storage.setColumns(columns);rebuildHomeUi();}
            else if(index==1){storage.setShowClock(!storage.showClock());clock.setVisibility(storage.showClock()?View.VISIBLE:View.GONE);date.setVisibility(storage.showClock()?View.VISIBLE:View.GONE);}
            else {storage.setShowDock(!storage.showDock());rebuildHomeUi();}
        } else if(section==2) {
            if(index==0){storage.setAnimations(!storage.animations());Toast.makeText(this,"Animations "+(storage.animations()?"enabled":"disabled"),Toast.LENGTH_SHORT).show();}
            else if(index==1){hidden.clear();storage.setHidden(hidden);render(search.getText().toString());}
            else loadApps();
        } else if(section==3) {
            if(index==0)showNotifications();
            else openSystemSetting(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,Settings.ACTION_SETTINGS);
        } else {
            if(index==0)openSystemSetting(Settings.ACTION_HOME_SETTINGS,Settings.ACTION_SETTINGS);
            else if(index==1)openSystemSetting(Settings.ACTION_DISPLAY_SETTINGS,Settings.ACTION_SETTINGS);
            else openSystemSetting(Settings.ACTION_SOUND_SETTINGS,Settings.ACTION_SETTINGS);
        }
    }

    private void openSystemSetting(String preferred,String fallback) {
        try { startActivity(new Intent(preferred)); }
        catch(Exception e) { try { startActivity(new Intent(fallback)); } catch(Exception ignored) {} }
    }

    private void openLibrary() {
        AppLibraryScreen screen = new AppLibraryScreen(this, apps, this::launch);
        showSurface(screen,SurfaceNavigation.LIBRARY);
        activeLibraryScreen=screen;
    }

    private void showNotifications() {
        NotificationScreen screen=new NotificationScreen(this);
        showSurface(screen,SurfaceNavigation.NOTIFICATIONS);
        activeNotificationScreen=screen;
    }

    private void showControlCenter() {
        showSurface(new ControlCenterScreen(this),SurfaceNavigation.CONTROL);
    }

    private void showSurface(View screen,int destination) {
        releaseActiveSurface();
        activeSurfaceView=screen;
        activeSurface=destination;
        setContentView(screen);
        if(storage.animations())Ui.enter(screen,true,10);
    }

    public void navigateToSurface(int destination) {
        if(destination==activeSurface)return;
        releaseActiveSurface();
        activeSurfaceView=null;
        activeSurface=SurfaceNavigation.HOME;
        if(destination==SurfaceNavigation.HOME){setContentView(root);return;}
        if(destination==SurfaceNavigation.LIBRARY)openLibrary();
        else if(destination==SurfaceNavigation.NOTIFICATIONS)showNotifications();
        else if(destination==SurfaceNavigation.CONTROL)showControlCenter();
        else if(destination==SurfaceNavigation.SETTINGS)settings();
    }

    public boolean isSurfaceActive(int surface) { return activeSurface==surface; }

    private void releaseActiveSurface() {
        if(activeLibraryScreen!=null){activeLibraryScreen.onHidden();activeLibraryScreen=null;}
        if(activeNotificationScreen!=null){activeNotificationScreen.onHidden();activeNotificationScreen=null;}
    }

    private void chooseWallpaper() {
        String[] names={"Dynamic dark gradient","Deep ocean","Violet dusk"};
        new AlertDialog.Builder(this).setTitle("WALLPAPER").setSingleChoiceItems(names,storage.getWallpaper(),(dialog,which)->{
            storage.setWallpaper(which);applyAppearance();
            dialog.dismiss();
        }).setNegativeButton("CANCEL",null).show();
    }

    private void chooseAccent() {
        String[] names={"Glacier blue","Violet","Mint","Rose"};
        int[] colors={0xFF89AEFF,0xFFB49AFF,0xFF83D7C2,0xFFFFA6B7};
        int selected=0;
        for(int i=0;i<colors.length;i++)if(colors[i]==storage.getAccent())selected=i;
        new AlertDialog.Builder(this).setTitle("ACCENT COLOUR").setSingleChoiceItems(names,selected,(dialog,which)->{
            storage.setAccent(colors[which]);applyAppearance();rebuildHomeUi();dialog.dismiss();
        }).setNegativeButton("CANCEL",null).show();
    }

    private void chooseIconSize() {
        String[] names={"40 dp","46 dp","52 dp"};
        int[] sizes={40,46,52};
        int selected=1;
        for(int i=0;i<sizes.length;i++)if(sizes[i]==storage.getIconSize())selected=i;
        new AlertDialog.Builder(this).setTitle("APP ICON SIZE").setSingleChoiceItems(names,selected,(dialog,which)->{
            storage.setIconSize(sizes[which]);applyAppearance();rebuildHomeUi();dialog.dismiss();
        }).setNegativeButton("CANCEL",null).show();
    }

    private String wallpaperName(int index) {
        String[] names={"Dynamic dark gradient","Deep ocean","Violet dusk"};
        return names[Math.max(0,Math.min(index,names.length-1))];
    }

    private String accentName(int color) {
        if(color==0xFFB49AFF)return "Violet";
        if(color==0xFF83D7C2)return "Mint";
        if(color==0xFFFFA6B7)return "Rose";
        return "Glacier blue";
    }

    private void rebuildHomeUi() {
        String query=search==null?"":search.getText().toString();
        applyAppearance();
        buildHome();
        search.setText(query);
        if(searchRender!=null)searchRenderHandler.removeCallbacks(searchRender);
        render(query,false,true);
    }

    @Override protected void onResume() {
        super.onResume();
        activityResumed=true;
        if(iconsTrimmed||packagesDirty) {
            iconsTrimmed=false;
            packagesDirty=false;
            loadApps();
            if(activeSurface==SurfaceNavigation.LIBRARY)openLibrary();
        }
        if(activeNotificationScreen!=null)activeNotificationScreen.onShown();
    }

    @Override protected void onPause() {
        activityResumed=false;
        super.onPause();
        try {((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}catch(Exception ignored){}
    }

    @Override public void onBackPressed() {
        if(activeSurface==SurfaceNavigation.LIBRARY&&activeLibraryScreen!=null&&activeLibraryScreen.handleBack())return;
        if(activeSurface==SurfaceNavigation.SETTINGS&&activeSurfaceView instanceof SettingsScreen&&((SettingsScreen)activeSurfaceView).handleBack())return;
        if(activeSurface!=SurfaceNavigation.HOME){navigateToSurface(SurfaceNavigation.HOME);return;}
        if(search!=null&&search.length()>0){
            search.setText("");
            try { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0); }
            catch(Exception ignored) {}
            return;
        }
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if(level>=TRIM_MEMORY_UI_HIDDEN&&iconCache!=null) {
            DRAWABLES.clear();
            clearImageDrawables(root);
            if(activeFolderDialog!=null)activeFolderDialog.dismiss();
            if(activeSurfaceView!=null)clearImageDrawables(activeSurfaceView);
            releaseActiveSurface();
            activeSurfaceView=null;
            activeSurface=SurfaceNavigation.HOME;
            setContentView(root);
            iconCache.trim();
            iconsTrimmed=true;
            if(searchRender!=null)searchRenderHandler.removeCallbacks(searchRender);
        }
    }

    private void clearImageDrawables(View view) {
        if(view instanceof ImageView) ((ImageView)view).setImageDrawable(null);
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)clearImageDrawables(group.getChildAt(i));
        }
    }

    @Override protected void onDestroy() {
        packageRefreshHandler.removeCallbacks(packageRefresh);
        if(searchRender!=null)searchRenderHandler.removeCallbacks(searchRender);
        releaseActiveSurface();
        activeSurfaceView=null;
        if(activeFolderDialog!=null)activeFolderDialog.dismiss();
        if(receiverRegistered)try{unregisterReceiver(packageReceiver);}catch(Exception ignored){}
        super.onDestroy();
    }
}
