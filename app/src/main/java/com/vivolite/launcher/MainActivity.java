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
    private int columns = 4;
    private boolean receiverRegistered = false;
    private String draggingId;
    private String armedDragId;

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
        root.setPadding(Ui.dp(this,18),Ui.dp(this,14),Ui.dp(this,18),Ui.dp(this,8));
        root.setBackgroundResource(com.vivolite.launcher.R.drawable.wallpaper_dark);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("V I V O   L I T E",11,Ui.WHITE,true);
        top.addView(brand,new LinearLayout.LayoutParams(0,Ui.dp(this,34),1));
        TextView control = pill("CONTROL");
        top.addView(control,new LinearLayout.LayoutParams(Ui.dp(this,94),Ui.dp(this,34)));
        control.setOnClickListener(v->new ControlCenterDialog(this).show());
        root.addView(top);

        clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm"); clock.setFormat12Hour("h:mm");
        clock.setTextSize(54); clock.setTextColor(Ui.WHITE);
        clock.setTypeface(Typeface.create("sans-serif-thin",Typeface.NORMAL));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,Ui.dp(this,66));
        cp.topMargin=Ui.dp(this,13); root.addView(clock,cp);

        date = new TextClock(this);
        date.setFormat24Hour("EEEE  ·  d MMMM"); date.setFormat12Hour("EEEE  ·  d MMMM");
        date.setTextSize(12); date.setTextColor(Ui.MUTED);
        LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,Ui.dp(this,26));
        dp.bottomMargin=Ui.dp(this,14); root.addView(date,dp);
        if(!storage.showClock()){clock.setVisibility(View.GONE);date.setVisibility(View.GONE);}

        search = new EditText(this);
        search.setSingleLine(true); search.setTextSize(14); search.setTextColor(Ui.WHITE);
        search.setHintTextColor(Ui.MUTED); search.setHint("⌕   Search apps");
        search.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);
        search.setBackground(Ui.panel(this,Ui.GLASS,22));
        search.setCompoundDrawablePadding(Ui.dp(this,8));
        root.addView(search,new LinearLayout.LayoutParams(-1,Ui.dp(this,46)));

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("APPLICATIONS",10,Ui.MUTED,true);
        heading.addView(title,new LinearLayout.LayoutParams(0,Ui.dp(this,40),1));
        count=text("0",10,Ui.MUTED,false); heading.addView(count);
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
            dock.setPadding(Ui.dp(this,8),Ui.dp(this,6),Ui.dp(this,8),Ui.dp(this,6));
            dock.setBackground(Ui.panel(this,Ui.GLASS,24));
            LinearLayout.LayoutParams dpock=new LinearLayout.LayoutParams(-1,Ui.dp(this,66));
            dpock.topMargin=Ui.dp(this,8); root.addView(dock,dpock);
        }

        TextView footer=text("PURE  ·  FLUID  ·  MINIMAL",8,Ui.MUTED,false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer,new LinearLayout.LayoutParams(-1,Ui.dp(this,22)));
        setContentView(root);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int c){render(s.toString());}
            public void afterTextChanged(Editable e){}
        });
    }

    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }

    private TextView pill(String value) {
        TextView t=text(value,10,Ui.WHITE,true);t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.panel(this,Ui.GLASS_LIGHT,18));return t;
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
        for(String id:order) {
            if(id.startsWith("folder:")) {
                try {
                    JSONObject data=folders.optJSONObject(id);
                    if(data!=null) {
                        LauncherItem folder=LauncherItem.folder(id,data.optString("name","Folder"));
                        JSONArray children=data.optJSONArray("children");
                        if(children!=null)for(int i=0;i<children.length();i++) {
                            String child=children.optString(i);
                            if(byId.containsKey(child))folder.children.add(child);
                        }
                        homeItems.add(folder);
                        consumed.addAll(folder.children);
                    }
                } catch(Exception ignored) {}
            } else if(byId.containsKey(id) && !consumed.contains(id)) homeItems.add(byId.get(id));
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
        int totalWidth=getResources().getDisplayMetrics().widthPixels-Ui.dp(this,36);
        int cell=totalWidth/columns;
        grid.setColumnCount(columns);
        for(int i=0;i<visible.size();i++) {
            LauncherItem item=visible.get(i);
            LinearLayout cellView=makeCell(item);
            GridLayout.LayoutParams p=new GridLayout.LayoutParams();
            p.width=cell;p.height=Ui.dp(this,104);
            p.setMargins(0,0,0,0);
            grid.addView(cellView,p);
        }
        renderDock();
    }

    private LinearLayout makeCell(LauncherItem item) {
        LinearLayout cell=new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);
        cell.setPadding(Ui.dp(this,2),Ui.dp(this,8),Ui.dp(this,2),Ui.dp(this,4));
        View iconView;
        if(item.isFolder()) iconView=folderPreview(item);
        else {
            ImageView icon=new ImageView(this);
            Drawable d=DRAWABLES.get(item.id);
            if(d!=null)icon.setImageDrawable(d);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconView=icon;
        }
        cell.addView(iconView,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,46)));
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
        Ui.enter(cell,storage.animations(),5);
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
                    if(!sourceItem.isFolder()&&!targetItem.children.contains(source)) {
                        targetItem.children.add(source);homeItems.remove(sourceItem);
                    }
                } else if(!sourceItem.isFolder()) {
                    // Center drop merges; edge drop reorders.
                    float x=event.getX()/Math.max(1,target.getWidth());
                    if(x>0.25f&&x<0.75f) {
                        LauncherItem folder=LauncherItem.folder("folder:"+System.currentTimeMillis(),"Folder");
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
                if(which==2){homeItems.remove(item);saveHomeOrder();render("");}
            } else {
                if(which==0)launch(item.id);
                if(which==1){if(!favorites.add(item.id))favorites.remove(item.id);storage.setFavorites(favorites);render(search.getText().toString());}
                if(which==2)openAppInfo(item);
                if(which==3){hidden.add(item.id);storage.setHidden(hidden);render(search.getText().toString());}
                if(which==4)startMove(item);
            }
        }).show();
    }

    private void startMove(LauncherItem item) {
        armedDragId = item.id;
        Toast.makeText(this,"Now touch this app and drag it onto another item",Toast.LENGTH_LONG).show();
    }

    private void openFolder(LauncherItem folder) {
        FolderDialog dialog=new FolderDialog(this,folder,apps,iconCache,storage.animations(),new FolderDialog.Listener(){
            public void onChanged(String title,List<String> children) {
                folder.title=title.isEmpty()?"Folder":title;
                saveHomeOrder();render(search.getText().toString());
            }
            public void onLaunch(String key){launch(key);}
        });
        dialog.show();
        Ui.enter(dialog.getWindow().getDecorView(),storage.animations(),8);
    }

    private void renameFolder(LauncherItem folder) {
        EditText input=new EditText(this);input.setText(folder.title);input.setTextColor(Ui.WHITE);
        new AlertDialog.Builder(this).setTitle("Rename folder").setView(input)
            .setPositiveButton("SAVE",(d,w)->{folder.title=input.getText().toString().trim();if(folder.title.isEmpty())folder.title="Folder";saveHomeOrder();render("");})
            .setNegativeButton("CANCEL",null).show();
    }

    private void launch(String id) {
        LauncherItem a=findApp(id);
        if(a==null)return;
        Intent intent=new Intent(Intent.ACTION_MAIN);intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(a.packageName,a.className));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(intent); } catch(Exception e) { Toast.makeText(this,"Unable to open app",Toast.LENGTH_SHORT).show(); }
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
            TextView hint=text("LONG-PRESS AN APP  ·  ADD TO FAVOURITES",9,Ui.MUTED,false);
            hint.setGravity(Gravity.CENTER);dock.addView(hint,new LinearLayout.LayoutParams(-1,-1));
        }
    }

    private boolean dockContains(String id) {
        for(int i=0;i<dock.getChildCount();i++)if(id.equals(dock.getChildAt(i).getTag()))return true;
        return false;
    }

    private void addDockIcon(LauncherItem app) {
        ImageView icon=new ImageView(this);icon.setTag(app.id);
        icon.setImageDrawable(DRAWABLES.get(app.id));icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1);
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
        @Override public void onReceive(Context context,Intent intent) {loadApps();}
    };

    private void settings() {
        String[] choices={"4 columns","5 columns","Show / hide clock","Show / hide dock","Restore hidden apps","Animations on / off","Refresh applications","Android Home settings"};
        new AlertDialog.Builder(this).setTitle("VIVO LITE SETTINGS").setItems(choices,(d,n)->{
            if(n==0||n==1){columns=n==0?4:5;storage.setColumns(columns);render(search.getText().toString());}
            else if(n==2){storage.setShowClock(!storage.showClock());clock.setVisibility(storage.showClock()?View.VISIBLE:View.GONE);date.setVisibility(storage.showClock()?View.VISIBLE:View.GONE);}
            else if(n==3){storage.setShowDock(!storage.showDock());rebuild();}
            else if(n==4){hidden.clear();storage.setHidden(hidden);render(search.getText().toString());}
            else if(n==5){storage.setAnimations(!storage.animations());Toast.makeText(this,"Animations "+(storage.animations()?"enabled":"disabled"),Toast.LENGTH_SHORT).show();}
            else if(n==6)loadApps();
            else if(n==7){try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
        }).show();
    }

    private void rebuild() {
        buildHome();loadApps();
    }

    @Override protected void onResume() {
        super.onResume();
        if(grid!=null)loadApps();
    }

    @Override protected void onPause() {
        super.onPause();
        try {((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}catch(Exception ignored){}
    }

    @Override public void onBackPressed() {
        if(search!=null&&search.length()>0){search.setText("");return;}
        super.onBackPressed();
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if(level>=TRIM_MEMORY_MODERATE&&iconCache!=null)iconCache.trim();
    }

    @Override protected void onDestroy() {
        if(receiverRegistered)try{unregisterReceiver(packageReceiver);}catch(Exception ignored){}
        super.onDestroy();
    }
}
