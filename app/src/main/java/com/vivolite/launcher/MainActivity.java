package com.vivolite.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
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
import android.view.Choreographer;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final Map<String,Drawable> DRAWABLES = new HashMap<>();
    private LauncherStorage storage;
    private IconCache iconCache;
    private IconPackManager iconPackManager;
    private final ArrayList<LauncherItem> apps = new ArrayList<>();
    private final ArrayList<LauncherItem> homeItems = new ArrayList<>();
    private final Set<String> hidden = new HashSet<>();
    private final Set<String> favorites = new HashSet<>();
    private LinearLayout root, dock;
    private FrameLayout homeFrame;
    private LinearLayout widgetShelf;
    private GridLayout grid;
    private HorizontalScrollView homePager;
    private LinearLayout pageTrack, pageDots;
    private final ArrayList<GridLayout> homeGrids = new ArrayList<>();
    // Keep page shells cheap; materialize app cells only for the visible neighbourhood.
    private final ArrayList<List<LauncherItem>> homePageItems = new ArrayList<>();
    private final ArrayList<Boolean> homePageBuilt = new ArrayList<>();
    private final ArrayList<Integer> homePageBuildIndex = new ArrayList<>();
    private final ArrayList<Boolean> homePageBuildQueued = new ArrayList<>();
    private final ArrayList<Boolean> homePageBuildAnimate = new ArrayList<>();
    private final ArrayList<Integer> homePageBuildQueue = new ArrayList<>();
    private static final int HOME_CELLS_PER_FRAME = 3;
    private boolean homeBuildFramePosted = false;
    private final Choreographer.FrameCallback homeBuildFrame = frameTimeNanos -> {
        homeBuildFramePosted = false;
        runHomePageBuildFrame();
    };
    private String homeEmptyMessage = "NO APPS AVAILABLE";
    private final Handler pageSnapHandler = new Handler(Looper.getMainLooper());
    private final Runnable pageSnap = this::snapHomePage;
    private boolean homePagerDragging = false;
    private int currentHomePage = 0;
    private static final int WIDGET_HOST_ID=0x564C;
    private static final int REQUEST_WIDGET_PICK=4100;
    private static final int REQUEST_WIDGET_CONFIGURE=4101;
    private AppWidgetManager appWidgetManager;
    private AppWidgetHost appWidgetHost;
    private final ArrayList<Integer> widgetIds=new ArrayList<>();
    private int pendingWidgetId=-1;
    private boolean widgetsListening=false;
    private boolean configuringNewWidget=false;
    private EditText search;
    private TextClock clock, date;
    private TextView count;
    private FolderDialog activeFolderDialog;
    private View activeSurfaceView;
    private NotificationScreen activeNotificationScreen;
    private int activeSurface = SurfaceNavigation.HOME;
    private TextView pulseCapsule;
    private boolean batteryReceiverRegistered=false;
    private LegacyTorchController torchController;
    static final int REQUEST_CAMERA_PERMISSION=5502;
    private int columns = 4;
    private boolean receiverRegistered = false;
    private boolean iconsTrimmed = false;
    private boolean activityResumed = false;
    private boolean packagesDirty = false;
    private boolean appLoadPaused = false;
    private boolean appLoadNeedsApply = false;
    private String draggingId;
    private String armedDragId;
    private final Handler packageRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable packageRefresh = () -> {
        if(activityResumed){loadApps();}
        else packagesDirty=true;
    };
    private final Handler searchRenderHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRender;
    private final Handler appLoadHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService appLoader = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "VIVO-LITE-AppScan");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });
    private Future<?> appLoadTask;
    private int appLoadGeneration = 0;
    private boolean homeRenderedOnce = false;

    public static Drawable iconFor(String id) { return DRAWABLES.get(id); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LauncherMotionEngine.configure(this);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= 21) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        storage = new LauncherStorage(this);
        currentHomePage=storage.getHomePage();
        appWidgetManager=AppWidgetManager.getInstance(this);
        appWidgetHost=new AppWidgetHost(this,WIDGET_HOST_ID);
        for(String id:storage.getOrder("widget_ids"))try{widgetIds.add(Integer.parseInt(id));}catch(Exception ignored){}
        iconCache = new IconCache(this);
        iconPackManager = new IconPackManager(this);
        iconPackManager.select(storage.getIconPack());
        columns = storage.getColumns();
        applyAppearance();
        hidden.addAll(storage.getHidden());
        favorites.addAll(storage.getFavorites());
        buildHome();
        attachTorchPreview();
        loadApps();
        registerPackageReceiver();
    }

    private void buildHome() {

        if(pageSnap!=null)pageSnapHandler.removeCallbacks(pageSnap);
        currentHomePage=storage.getHomePage();
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
        TextView brand = text("VIVO LITE",13,Ui.WHITE,true);
        brand.setLetterSpacing(0.18f);
        brandBlock.addView(brand);
        TextView strap=text("YOUR SPACE, REFINED",8,Ui.secondaryText(),false);
        strap.setLetterSpacing(0.12f);
        brandBlock.addView(strap);
        top.addView(brandBlock,new LinearLayout.LayoutParams(0,Ui.dp(this,38),1));
        TextView control = pill("CONTROL");
        top.addView(control,new LinearLayout.LayoutParams(Ui.dp(this,82),Ui.dp(this,34)));
        control.setOnClickListener(v->showControlCenter());
        Ui.attachTouchFeedback(control,storage.animations());
        TextView addWidget=pill("+ WIDGET");
        LinearLayout.LayoutParams widgetButtonParams=new LinearLayout.LayoutParams(Ui.dp(this,76),Ui.dp(this,34));
        widgetButtonParams.rightMargin=Ui.dp(this,7);top.addView(addWidget,1,widgetButtonParams);
        addWidget.setOnClickListener(v->pickWidget());
        Ui.attachTouchFeedback(addWidget,storage.animations());
        root.addView(top);

        clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm"); clock.setFormat12Hour("h:mm");
        clock.setGravity(Gravity.CENTER);
        clock.setTextSize(60); clock.setTextColor(Ui.WHITE);
        clock.setTypeface(Typeface.create("sans-serif-thin",Typeface.NORMAL));
        clock.setLetterSpacing(0.015f);
        final float[] clockTouchY={0f};
        clock.setOnTouchListener((view,event)->{
            if(event.getAction()==MotionEvent.ACTION_DOWN){clockTouchY[0]=event.getY();return true;}
            if(event.getAction()==MotionEvent.ACTION_UP){
                float delta=event.getY()-clockTouchY[0];
                if(Math.abs(delta)>Ui.dp(this,42)) {
                    if(delta<0)showHomePage(currentHomePage+1,true);
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
        date.setTextSize(12); date.setTextColor(Ui.secondaryText());
        date.setLetterSpacing(0.035f);
        date.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,Ui.dp(this,25));
        dp.bottomMargin=Ui.dp(this,16); root.addView(date,dp);
        if(!storage.showClock()){clock.setVisibility(View.GONE);date.setVisibility(View.GONE);}

        pulseCapsule=text("PULSE  ·  "+batterySummary(),9,Ui.primaryText(),true);
        pulseCapsule.setGravity(Gravity.CENTER);
        pulseCapsule.setLetterSpacing(0.04f);
        pulseCapsule.setBackground(Ui.panel(this,Ui.GLASS_LIGHT,18));
        LinearLayout.LayoutParams pulseParams=new LinearLayout.LayoutParams(-1,Ui.dp(this,32));
        pulseParams.bottomMargin=Ui.dp(this,8);root.addView(pulseCapsule,pulseParams);
        pulseCapsule.setOnClickListener(v->showControlCenter());
        Ui.attachTouchFeedback(pulseCapsule,storage.animations());

        widgetShelf=new LinearLayout(this);widgetShelf.setOrientation(LinearLayout.VERTICAL);
        root.addView(widgetShelf,new LinearLayout.LayoutParams(-1,-2));

        search = new EditText(this);
        search.setSingleLine(true); search.setTextSize(13); search.setTextColor(Ui.primaryText());
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((view,actionId,event)->{
            if(actionId==EditorInfo.IME_ACTION_SEARCH||actionId==EditorInfo.IME_ACTION_DONE) {
                try { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0); }
                catch(Exception ignored) {}
                return true;
            }
            return false;
        });
        search.setHintTextColor(Ui.secondaryText()); search.setHint("⌕   Search your apps");
        search.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);
        search.setBackground(Ui.panel(this,Ui.GLASS_LIGHT,22));
        search.setCompoundDrawablePadding(Ui.dp(this,8));
        root.addView(search,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("YOUR APPS",10,Ui.secondaryText(),true);
        title.setLetterSpacing(0.14f);
        heading.addView(title,new LinearLayout.LayoutParams(0,Ui.dp(this,38),1));
        count=text("0 ITEMS",9,Ui.MUTED,false); count.setLetterSpacing(0.06f); heading.addView(count);
        root.addView(heading);

        homePager=new HorizontalScrollView(this);
        homePager.setFillViewport(true);
        homePager.setHorizontalScrollBarEnabled(false);
        homePager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        pageTrack=new LinearLayout(this);
        pageTrack.setOrientation(LinearLayout.HORIZONTAL);
        homePager.addView(pageTrack,new HorizontalScrollView.LayoutParams(-2,-1));
        homePager.setOnTouchListener((view,event)->{
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN) {
                homePagerDragging=true;
                if(pageSnap!=null)pageSnapHandler.removeCallbacks(pageSnap);
            } else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL) {
                homePagerDragging=false;
                schedulePageSnap();
            }
            return false;
        });
        root.addView(homePager,new LinearLayout.LayoutParams(-1,0,1));
        pageDots=new LinearLayout(this);pageDots.setGravity(Gravity.CENTER);
        root.addView(pageDots,new LinearLayout.LayoutParams(-1,Ui.dp(this,20)));
        homePager.getViewTreeObserver().addOnScrollChangedListener(this::onHomePagerScrolled);

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
        homeFrame=new FrameLayout(this);
        homeFrame.setBackground(new LauncherWallpaperDrawable(storage.getAccent(),storage.getWallpaper()));
        homeFrame.addView(root,new FrameLayout.LayoutParams(-1,-1));
        setContentView(homeFrame);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int c){scheduleSearchRender(s.toString());}
            public void afterTextChanged(Editable e){}
        });
        renderWidgetShelf();
    }

    private void attachTorchPreview() {
        torchController=new LegacyTorchController(this,enabled->{
            if(activeSurfaceView instanceof ControlCenterScreen)((ControlCenterScreen)activeSurfaceView).setTorchState(enabled);
        });
        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(Ui.dp(this,1),Ui.dp(this,1),Gravity.LEFT|Gravity.TOP);
        ((ViewGroup)getWindow().getDecorView()).addView(torchController,params);
    }

    public void toggleTorch() { if(torchController!=null)torchController.toggle(); }
    public boolean isTorchOn() { return torchController!=null&&torchController.isTorchEnabled(); }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQUEST_CAMERA_PERMISSION&&torchController!=null)
            torchController.onPermissionResult(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED);
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
        if(homeFrame!=null)homeFrame.setBackground(new LauncherWallpaperDrawable(storage.getAccent(),storage.getWallpaper()));
    }

    private void loadApps() {
        appLoadPaused = false;
        appLoadNeedsApply = true;
        final int generation = ++appLoadGeneration;
        final String selectedPack = storage.getIconPack();
        final Context appContext = getApplicationContext();
        if (appLoadTask != null) appLoadTask.cancel(true);

        appLoadTask = appLoader.submit(() -> {
            PackageManager pm = appContext.getPackageManager();
            IconPackManager loadedPack = new IconPackManager(appContext);
            loadedPack.select(selectedPack);
            ArrayList<LauncherItem> loadedApps = new ArrayList<>();
            Map<String,Drawable> loadedDrawables = new HashMap<>();
            Intent query = new Intent(Intent.ACTION_MAIN);
            query.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
            Set<String> seen = new HashSet<>();

            for (ResolveInfo r : found) {
                if (Thread.currentThread().isInterrupted()) return;
                if (r.activityInfo == null || r.activityInfo.packageName.equals(getPackageName())) continue;
                String pkg = r.activityInfo.packageName;
                String cls = r.activityInfo.name;
                String id = pkg + "/" + cls;
                if (!seen.add(id)) continue;
                try {
                    CharSequence label = r.loadLabel(pm);
                    LauncherItem item = LauncherItem.app(
                        id, label == null ? cls.substring(cls.lastIndexOf('.') + 1) : String.valueOf(label),
                        pkg, cls
                    );
                    Drawable drawable = loadedPack.icon(pkg, cls);
                    boolean monochrome = drawable == null;
                    if (monochrome) drawable = r.loadIcon(pm);
                    Drawable cachedIcon = iconCache.get(id, drawable, monochrome);
                    loadedApps.add(item);
                    loadedDrawables.put(id, cachedIcon);
                } catch (RuntimeException unavailable) {
                    // An app can be removed or updated while the package scan is running.
                    // Skip only that stale entry instead of aborting the entire library refresh.
                }
            }

            Collections.sort(loadedApps, new Comparator<LauncherItem>() {
                public int compare(LauncherItem a, LauncherItem b) {
                    return a.title.compareToIgnoreCase(b.title);
                }
            });

            appLoadHandler.post(() -> {
                if (generation != appLoadGeneration || isFinishing()) return;
                appLoadNeedsApply = false;
                iconPackManager = loadedPack;
                apps.clear();
                apps.addAll(loadedApps);
                DRAWABLES.clear();
                DRAWABLES.putAll(loadedDrawables);
                restoreHomeItems();
                render(search == null ? "" : search.getText().toString());
            });
        });
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
        if (homePager == null) return;
        boolean animateFirstRender = !homeRenderedOnce;
        homeRenderedOnce = true;
        render(filter,animateFirstRender,true);
    }

    private void render(String filter,boolean animate,boolean refreshDock) {
        if(homePager==null)return;
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
        if(!q.isEmpty())currentHomePage=0;
        renderHomePages(visible,animate,q);
        if(refreshDock)renderDock();
    }

    private void renderHomePages(List<LauncherItem> visible,boolean animate,String query) {
        resetHomePageBuilds();
        pageTrack.removeAllViews();
        pageDots.removeAllViews();
        homeGrids.clear();
        homePageItems.clear();
        homePageBuilt.clear();
        homePageBuildIndex.clear();
        homePageBuildQueued.clear();
        homePageBuildAnimate.clear();
        grid=null;

        int width=homePageWidth();
        int pageCapacity=columns*4;
        int pageCount=Math.max(1,(visible.size()+pageCapacity-1)/pageCapacity);
        currentHomePage=Math.min(currentHomePage,pageCount-1);
        homeEmptyMessage=query.isEmpty()?"NO APPS AVAILABLE":"NO MATCHES  ·  TRY ANOTHER NAME";

        for(int page=0;page<pageCount;page++) {
            ScrollView scroll=new ScrollView(this);
            scroll.setFillViewport(false);
            scroll.setVerticalScrollBarEnabled(false);

            GridLayout pageGrid=new GridLayout(this);
            pageGrid.setColumnCount(columns);
            homeGrids.add(pageGrid);
            if(grid==null)grid=pageGrid;
            scroll.addView(pageGrid,new ScrollView.LayoutParams(-1,-2));

            int start=page*pageCapacity;
            int end=Math.min(visible.size(),start+pageCapacity);
            homePageItems.add(new ArrayList<>(visible.subList(start,end)));
            homePageBuilt.add(false);
            homePageBuildIndex.add(0);
            homePageBuildQueued.add(false);
            homePageBuildAnimate.add(false);
            pageTrack.addView(scroll,new LinearLayout.LayoutParams(width,-1));

            final int targetPage=page;
            TextView dot=text(page==currentHomePage?"━":"•",page==currentHomePage?13:10,
                    page==currentHomePage?Ui.ACCENT:Ui.secondaryText(),false);
            dot.setGravity(Gravity.CENTER);
            dot.setContentDescription("Home page "+(page+1));
            pageDots.addView(dot,new LinearLayout.LayoutParams(Ui.dp(this,22),Ui.dp(this,20)));
            dot.setOnClickListener(v->showHomePage(targetPage,true));
            Ui.attachTouchFeedback(dot,storage.animations());
        }

        if(count!=null)count.setText(visible.isEmpty()
                ?(query.isEmpty()?"0 ITEMS":"NO MATCHES")
                :String.format(Locale.ROOT,"%02d / %02d",currentHomePage+1,pageCount));
        if(query.isEmpty())storage.setHomePage(currentHomePage);

        // Queue the active and neighbouring pages. Cells are added in small
        // frame-scheduled batches rather than one large main-thread layout burst.
        preloadNearbyHomePages(currentHomePage,animate&&query.isEmpty());
        homePager.post(()->{
            homePager.scrollTo(currentHomePage*width,0);
            updatePageIndicator();
            preloadNearbyHomePages(currentHomePage,false);
        });
    }

    private void resetHomePageBuilds() {
        cancelHomePageBuildFrame();
        homePageBuildQueue.clear();
    }

    private void cancelHomePageBuildFrame() {
        if(homeBuildFramePosted) {
            Choreographer.getInstance().removeFrameCallback(homeBuildFrame);
            homeBuildFramePosted=false;
        }
    }

    private void scheduleHomePageBuildFrame() {
        if(homeBuildFramePosted||homePageBuildQueue.isEmpty()||homePager==null
                ||!activityResumed||activeSurface!=SurfaceNavigation.HOME||isFinishing())return;
        homeBuildFramePosted=true;
        Choreographer.getInstance().postFrameCallback(homeBuildFrame);
    }

    private void buildHomePage(int page,boolean animate) {
        if(page<0||page>=homeGrids.size()||homePageBuilt.get(page))return;
        if(homePageBuildQueued.get(page)) {
            if(animate)homePageBuildAnimate.set(page,true);
            return;
        }
        homePageBuildQueued.set(page,true);
        homePageBuildAnimate.set(page,animate);
        homePageBuildQueue.add(page);
        scheduleHomePageBuildFrame();
    }

    private void runHomePageBuildFrame() {
        if(!activityResumed||activeSurface!=SurfaceNavigation.HOME||isFinishing()
                ||homePager==null)return;

        int budget=HOME_CELLS_PER_FRAME;
        while(budget>0&&!homePageBuildQueue.isEmpty()) {
            int page=homePageBuildQueue.get(0);
            if(page<0||page>=homeGrids.size()) {
                homePageBuildQueue.remove(0);
                continue;
            }

            GridLayout pageGrid=homeGrids.get(page);
            List<LauncherItem> items=homePageItems.get(page);
            if(items.isEmpty()) {
                if(pageGrid.getChildCount()==0) {
                    TextView empty=text(homeEmptyMessage,11,Ui.secondaryText(),true);
                    empty.setGravity(Gravity.CENTER);
                    empty.setLetterSpacing(0.05f);
                    GridLayout.LayoutParams params=new GridLayout.LayoutParams();
                    params.width=homePageWidth();
                    params.height=Ui.dp(this,144);
                    pageGrid.addView(empty,params);
                }
                finishHomePageBuild(page);
                continue;
            }

            int index=homePageBuildIndex.get(page);
            int limit=Math.min(items.size(),index+budget);
            while(index<limit) {
                LauncherItem item=items.get(index);
                boolean animate=homePageBuildAnimate.get(page)&&index<columns;
                LinearLayout cellView=makeCell(item,animate);
                GridLayout.LayoutParams params=new GridLayout.LayoutParams();
                params.width=homePageWidth()/columns;
                params.height=Ui.dp(this,104);
                pageGrid.addView(cellView,params);
                index++;
                budget--;
            }
            homePageBuildIndex.set(page,index);
            if(index>=items.size())finishHomePageBuild(page);
        }

        if(!homePageBuildQueue.isEmpty())scheduleHomePageBuildFrame();
    }

    private void finishHomePageBuild(int page) {
        homePageBuilt.set(page,true);
        homePageBuildQueued.set(page,false);
        homePageBuildAnimate.set(page,false);
        homePageBuildIndex.set(page,homePageItems.get(page).size());
        if(!homePageBuildQueue.isEmpty()&&homePageBuildQueue.get(0)==page)
            homePageBuildQueue.remove(0);
    }

    private void trimDistantHomePages(int center) {
        // Keep only the visible page and its immediate neighbours populated.
        // The lightweight page shells remain so horizontal swipe geometry is stable.
        for(int i=homePageBuildQueue.size()-1;i>=0;i--) {
            int page=homePageBuildQueue.get(i);
            if(Math.abs(page-center)>1) {
                homePageBuildQueue.remove(i);
                if(page>=0&&page<homePageBuildQueued.size()) {
                    homePageBuildQueued.set(page,false);
                    homePageBuildIndex.set(page,0);
                    homePageBuildAnimate.set(page,false);
                }
            }
        }
        for(int page=0;page<homeGrids.size();page++) {
            if(Math.abs(page-center)<=1)continue;
            if(homePageBuilt.get(page)||homePageBuildIndex.get(page)>0
                    ||homeGrids.get(page).getChildCount()>0) {
                homeGrids.get(page).removeAllViews();
                homePageBuilt.set(page,false);
                homePageBuildIndex.set(page,0);
                homePageBuildQueued.set(page,false);
                homePageBuildAnimate.set(page,false);
            }
        }
    }

    private void preloadNearbyHomePages(int center,boolean animateCenter) {
        buildHomePage(center,animateCenter);
        buildHomePage(center+1,false);
        buildHomePage(center-1,false);
    }

    private int homePageWidth() {
        return Math.max(Ui.dp(this,240),getResources().getDisplayMetrics().widthPixels-Ui.dp(this,44));
    }

    private void showHomePage(int page,boolean smooth) {
        int width=homePageWidth();
        currentHomePage=Math.max(0,Math.min(page,Math.max(0,homeGrids.size()-1)));
        if(search==null||search.length()==0)storage.setHomePage(currentHomePage);
        preloadNearbyHomePages(currentHomePage,false);
        if(smooth)homePager.smoothScrollTo(currentHomePage*width,0);else homePager.scrollTo(currentHomePage*width,0);
        updatePageIndicator();
    }

    private void editQuickNote() {
        EditText editor=new EditText(this);editor.setText(storage.getQuickNote());editor.setTextColor(Ui.primaryText());editor.setHint("Write a quick note");
        new AlertDialog.Builder(this).setTitle("QUICK NOTE").setView(editor)
            .setPositiveButton("SAVE",(dialog,which)->storage.setQuickNote(editor.getText().toString()))
            .setNegativeButton("CANCEL",null).show();
    }

    private String batterySummary() {
        return batterySummary(registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
    }

    private String batterySummary(Intent status) {
        if(status==null)return "BATTERY UNAVAILABLE";
        int level=status.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,-1);
        int scale=status.getIntExtra(android.os.BatteryManager.EXTRA_SCALE,-1);
        int state=status.getIntExtra(android.os.BatteryManager.EXTRA_STATUS,-1);
        if(level<0||scale<=0)return "BATTERY UNAVAILABLE";
        boolean charging=state==android.os.BatteryManager.BATTERY_STATUS_CHARGING||state==android.os.BatteryManager.BATTERY_STATUS_FULL;
        return (level*100/scale)+"%"+(charging?"  ·  CHARGING":"  ·  BATTERY");
    }

    private final BroadcastReceiver batteryReceiver=new BroadcastReceiver() {
        @Override public void onReceive(Context context,Intent intent) {
            if(pulseCapsule!=null)pulseCapsule.setText("PULSE  ·  "+batterySummary(intent));
        }
    };

    private void onHomePagerScrolled() {
        updatePageIndicator();
        // A fling keeps producing scroll callbacks after ACTION_UP. Debounce from
        // the final scroll frame instead of snapping 90ms after the finger lifts.
        if(!homePagerDragging)schedulePageSnap();
    }

    private void schedulePageSnap() {
        if(homePager==null||homeGrids.isEmpty())return;
        pageSnapHandler.removeCallbacks(pageSnap);
        pageSnapHandler.postDelayed(pageSnap,140);
    }

    private void snapHomePage() {
        if(homePager==null||homeGrids.isEmpty()||homePagerDragging)return;
        int width=homePageWidth();
        int settledPage=Math.max(0,Math.min(Math.round((float)homePager.getScrollX()/width),homeGrids.size()-1));
        preloadNearbyHomePages(settledPage,false);
        int target=settledPage*width;
        if(Math.abs(homePager.getScrollX()-target)>Ui.dp(this,2)) {
            showHomePage(target/width,true);
        }
    }

    private void updatePageIndicator() {
        if(pageDots==null||homePager==null)return;
        int width=homePageWidth();
        int nextPage=Math.max(0,Math.min(Math.round((float)homePager.getScrollX()/width),pageDots.getChildCount()-1));
        if(nextPage==currentHomePage)return;
        currentHomePage=nextPage;
        // Keep the live hierarchy bounded and have the next page ready before
        // the next swipe settles.
        trimDistantHomePages(currentHomePage);
        preloadNearbyHomePages(currentHomePage,false);
        for(int i=0;i<pageDots.getChildCount();i++) {
            TextView dot=(TextView)pageDots.getChildAt(i);
            dot.setTextColor(i==currentHomePage?Ui.ACCENT:Ui.secondaryText());
            dot.setText(i==currentHomePage?"━":"•");
            dot.setTextSize(i==currentHomePage?13:10);
        }
        if(count!=null&&pageDots.getChildCount()>0)count.setText(String.format(Locale.ROOT,"%02d / %02d",currentHomePage+1,pageDots.getChildCount()));
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
            Ui.touchFeedback(v,event,storage.animations());
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
        icon.setOnClickListener(v->launch(app.id));
        Ui.attachTouchFeedback(icon,storage.animations());
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
            Uri changedPackage=intent.getData();
            if(changedPackage!=null&&storage.getIconPack().equals(changedPackage.getSchemeSpecificPart())) {
                iconPackManager.invalidate();
                iconCache.trim();
            }
            packageRefreshHandler.removeCallbacks(packageRefresh);
            packageRefreshHandler.postDelayed(packageRefresh,500);
        }
    };

    private void settings() {
        String[] sections={"Personalisation","Home screen","Motion & performance","Notifications","Android system","Workspace"};
        String[] descriptions={"Wallpaper, monochrome surfaces, icon sizing and icon packs","Grid, clock, pages and dock","Animation and app management","Notification access and live alerts","Device settings shortcuts","Native widgets and your saved quick note"};
        SettingsScreen screen=new SettingsScreen(this,sections,descriptions,storage.animations(),new SettingsScreen.Listener(){
            public String[] options(int category){return settingsOptions(category);}
            public String[] descriptions(int category){return settingsDescriptions(category);}
            public void onSelect(int category,int option){applySetting(category,option);}
        });
        showSurface(screen,SurfaceNavigation.SETTINGS);
    }

    private String[] settingsOptions(int section) {
        if(section==0) return new String[]{"Wallpaper · "+wallpaperName(storage.getWallpaper()),
                "Appearance · "+(storage.darkGlass()?"Dark glass":"Light glass"),"Icon size · "+storage.getIconSize()+" dp","Icon pack · "+iconPackLabel()};
        if(section==1)return new String[]{"Grid columns · "+columns,"Clock and date · "+(storage.showClock()?"Shown":"Hidden"),"Dock · "+(storage.showDock()?"Shown":"Hidden")};
        if(section==2)return new String[]{"Animations · "+(storage.animations()?"On":"Off"),"Restore hidden apps","Refresh app library"};
        if(section==3)return new String[]{"Open notifications","Notification access settings"};
        if(section==4)return new String[]{"Android Home settings","Android display settings","Android sound settings"};
        return new String[]{"Add a native Android widget","Edit the saved quick note"};
    }

    private String[] settingsDescriptions(int section) {
        if(section==0)return new String[]{"Choose one of the included static gradient wallpapers.","Switch between light and dark glass surfaces.","Choose the displayed app icon size.","Apply installed ADW-compatible icon mappings; unmapped apps use their Android icon."};
        if(section==1)return new String[]{"Toggle between four and five home grid columns.","Show or hide the home clock and date.","Show or hide the favourites dock."};
        if(section==2)return new String[]{"Enable or disable launcher page and interaction animations.","Make hidden apps visible again.","Re-query installed launchable apps."};
        if(section==3)return new String[]{"View notifications only when Android Notification Access is enabled.","Open Android's notification access settings."};
        if(section==4)return new String[]{"Choose the Android Home app.","Open Android display settings.","Open Android sound settings."};
        return new String[]{"Use Android's installed widget picker and persist the chosen widget.","Edit the note saved on this device."};
    }

    private void applySetting(int section,int index) {
        if(section==0) {
            if(index==0)chooseWallpaper();
            else if(index==1){storage.setDarkGlass(!storage.darkGlass());applyAppearance();rebuildHomeUi();}
            else if(index==2)chooseIconSize();
            else chooseIconPack();
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
        } else if(section==4) {
            if(index==0)openSystemSetting(Settings.ACTION_HOME_SETTINGS,Settings.ACTION_SETTINGS);
            else if(index==1)openSystemSetting(Settings.ACTION_DISPLAY_SETTINGS,Settings.ACTION_SETTINGS);
            else openSystemSetting(Settings.ACTION_SOUND_SETTINGS,Settings.ACTION_SETTINGS);
        } else {
            if(index==0)pickWidget();
            else editQuickNote();
        }
    }

    private void openSystemSetting(String preferred,String fallback) {
        try { startActivity(new Intent(preferred)); }
        catch(Exception e) { try { startActivity(new Intent(fallback)); } catch(Exception ignored) {} }
    }

    private void showNotifications() {
        NotificationScreen screen=new NotificationScreen(this,storage.animations());
        showSurface(screen,SurfaceNavigation.NOTIFICATIONS);
        activeNotificationScreen=screen;
    }

    private void showControlCenter() {
        ControlCenterScreen screen=new ControlCenterScreen(this,storage.animations());
        screen.setTorchState(isTorchOn());
        showSurface(screen,SurfaceNavigation.CONTROL);
    }

    private void pickWidget() {
        pendingWidgetId=appWidgetHost.allocateAppWidgetId();
        configuringNewWidget=true;
        Intent pick=new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pick.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId);
        try { startActivityForResult(pick,REQUEST_WIDGET_PICK); }
        catch(Exception e) { appWidgetHost.deleteAppWidgetId(pendingWidgetId);pendingWidgetId=-1;Toast.makeText(this,"Widget picker unavailable",Toast.LENGTH_SHORT).show(); }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQUEST_WIDGET_PICK) {
            int id=data==null?pendingWidgetId:data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId);
            if(resultCode!=RESULT_OK||id<0) { if(id>=0)appWidgetHost.deleteAppWidgetId(id);pendingWidgetId=-1;return; }
            pendingWidgetId=id;
            AppWidgetProviderInfo info=appWidgetManager.getAppWidgetInfo(id);
            if(info==null) { appWidgetHost.deleteAppWidgetId(id);pendingWidgetId=-1;return; }
            if(info.configure!=null) {
                try { appWidgetHost.startAppWidgetConfigureActivityForResult(this,id,0,REQUEST_WIDGET_CONFIGURE,null); }
                catch(Exception e) { appWidgetHost.deleteAppWidgetId(id);pendingWidgetId=-1;configuringNewWidget=false;Toast.makeText(this,"Widget setup unavailable",Toast.LENGTH_SHORT).show(); }
            } else finishWidgetSetup(id);
        } else if(requestCode==REQUEST_WIDGET_CONFIGURE) {
            if(resultCode==RESULT_OK&&pendingWidgetId>=0)finishWidgetSetup(pendingWidgetId);
            else if(configuringNewWidget&&pendingWidgetId>=0)appWidgetHost.deleteAppWidgetId(pendingWidgetId);
            pendingWidgetId=-1;
            configuringNewWidget=false;
        }
    }

    private void finishWidgetSetup(int id) {
        if(!widgetIds.contains(id))widgetIds.add(id);
        saveWidgetIds();pendingWidgetId=-1;
        configuringNewWidget=false;
        if(activeSurface!=SurfaceNavigation.HOME)navigateToSurface(SurfaceNavigation.HOME);
        renderWidgetShelf();
    }

    private void saveWidgetIds() {
        ArrayList<String> ids=new ArrayList<>();for(Integer id:widgetIds)ids.add(String.valueOf(id));
        storage.setOrder("widget_ids",ids);
    }

    private void renderWidgetShelf() {
        if(widgetShelf==null)return;
        widgetShelf.removeAllViews();
        for(Integer id:new ArrayList<>(widgetIds)) {
            AppWidgetProviderInfo info=appWidgetManager.getAppWidgetInfo(id);
            if(info==null){widgetIds.remove(id);try{appWidgetHost.deleteAppWidgetId(id);}catch(RuntimeException ignored){}continue;}
            AppWidgetHostView hostView;
            try {hostView=appWidgetHost.createView(this,id,info);hostView.setAppWidget(id,info);}
            catch(RuntimeException e){continue;}
            FrameLayout card=new FrameLayout(this);card.setBackground(Ui.panel(this,Ui.GLASS_LIGHT,20));
            int minHeightDp=Math.max(84,Math.round(info.minHeight/getResources().getDisplayMetrics().density));
            int heightDp=storage.getWidgetHeight(id,minHeightDp);
            int height=Ui.dp(this,Math.max(minHeightDp,Math.min(420,heightDp)));
            FrameLayout.LayoutParams widgetParams=new FrameLayout.LayoutParams(-1,height);
            card.addView(hostView,widgetParams);
            TextView more=text("···",20,Ui.primaryText(),true);more.setGravity(Gravity.CENTER);
            more.setBackground(Ui.panel(this,Ui.GLASS,18));
            FrameLayout.LayoutParams moreParams=new FrameLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,36),Gravity.RIGHT|Gravity.TOP);
            card.addView(more,moreParams);
            more.setOnClickListener(v->showWidgetMenu(id,info));
            Ui.attachTouchFeedback(more,storage.animations());
            LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,height);
            cardParams.bottomMargin=Ui.dp(this,8);widgetShelf.addView(card,cardParams);
        }
        saveWidgetIds();
        widgetShelf.setVisibility(widgetShelf.getChildCount()==0?View.GONE:View.VISIBLE);
    }

    private void showWidgetMenu(int id,AppWidgetProviderInfo info) {
        ArrayList<String> options=new ArrayList<>();
        if(info.configure!=null)options.add("Configure widget");
        options.add("Make taller");options.add("Make shorter");options.add("Remove widget");
        String[] actions=options.toArray(new String[0]);
        new AlertDialog.Builder(this).setItems(actions,(dialog,index)->{
            String action=actions[index];
            if("Configure widget".equals(action)) {
                pendingWidgetId=id;
                configuringNewWidget=false;
                try {appWidgetHost.startAppWidgetConfigureActivityForResult(this,id,0,REQUEST_WIDGET_CONFIGURE,null);}
                catch(Exception ignored){pendingWidgetId=-1;}
            } else if("Make taller".equals(action))resizeWidget(id,info,48);
            else if("Make shorter".equals(action))resizeWidget(id,info,-48);
            else {
                appWidgetHost.deleteAppWidgetId(id);widgetIds.remove(Integer.valueOf(id));saveWidgetIds();renderWidgetShelf();
            }
        }).show();
    }

    private void resizeWidget(int id,AppWidgetProviderInfo info,int changeDp) {
        int minDp=Math.max(84,Math.round(info.minHeight/getResources().getDisplayMetrics().density));
        int old=storage.getWidgetHeight(id,minDp);
        int next=Math.max(minDp,Math.min(420,old+changeDp));
        storage.setWidgetHeight(id,next);
        android.os.Bundle options=new android.os.Bundle();
        int widthDp=Math.max(1,Math.round(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-44);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,next);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,next);
        try {appWidgetManager.updateAppWidgetOptions(id,options);}catch(RuntimeException ignored){}
        renderWidgetShelf();
    }

    @Override protected void onStart() {
        super.onStart();
        try {appWidgetHost.startListening();widgetsListening=true;} catch(RuntimeException ignored){widgetsListening=false;}
        try {registerReceiver(batteryReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));batteryReceiverRegistered=true;}catch(Exception ignored){batteryReceiverRegistered=false;}
        if(pulseCapsule!=null)pulseCapsule.setText("PULSE  ·  "+batterySummary());
    }

    @Override protected void onStop() {
        if(torchController!=null)torchController.turnOff();
        if(widgetsListening)try {appWidgetHost.stopListening();}catch(RuntimeException ignored){}
        widgetsListening=false;
        if(batteryReceiverRegistered)try {unregisterReceiver(batteryReceiver);}catch(Exception ignored){}
        batteryReceiverRegistered=false;
        super.onStop();
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
        if(destination==SurfaceNavigation.HOME){setContentView(homeFrame);scheduleHomePageBuildFrame();return;}
        if(destination==SurfaceNavigation.NOTIFICATIONS)showNotifications();
        else if(destination==SurfaceNavigation.CONTROL)showControlCenter();
        else if(destination==SurfaceNavigation.SETTINGS)settings();
    }

    public boolean isSurfaceActive(int surface) { return activeSurface==surface; }

    private void releaseActiveSurface() {
        if(activeNotificationScreen!=null){activeNotificationScreen.onHidden();activeNotificationScreen=null;}
    }

    private void chooseWallpaper() {
        String[] names={"Dynamic dark gradient","Deep ocean","Violet dusk"};
        new AlertDialog.Builder(this).setTitle("WALLPAPER").setSingleChoiceItems(names,storage.getWallpaper(),(dialog,which)->{
            storage.setWallpaper(which);applyAppearance();
            dialog.dismiss();
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

    private void chooseIconPack() {
        List<IconPackManager.Pack> packs=iconPackManager.discover();
        ArrayList<String> labels=new ArrayList<>();ArrayList<String> packages=new ArrayList<>();
        labels.add("VIVO-LITE monochrome (system icons)");packages.add("");
        for(IconPackManager.Pack pack:packs){labels.add(pack.label);packages.add(pack.packageName);}
        int selected=packages.indexOf(storage.getIconPack());if(selected<0)selected=0;
        new AlertDialog.Builder(this).setTitle("APP ICON FAMILY")
            .setSingleChoiceItems(labels.toArray(new String[0]),selected,(dialog,which)->{
                storage.setIconPack(packages.get(which));iconCache.trim();loadApps();dialog.dismiss();
            }).setNegativeButton("CANCEL",null).show();
    }

    private String iconPackLabel() {
        String selected=storage.getIconPack();if(selected.isEmpty())return "VIVO-LITE";
        for(IconPackManager.Pack pack:iconPackManager.discover())if(pack.packageName.equals(selected))return pack.label;
        return "Unavailable · VIVO-LITE fallback";
    }

    private String wallpaperName(int index) {
        String[] names={"Dynamic dark gradient","Deep ocean","Violet dusk"};
        return names[Math.max(0,Math.min(index,names.length-1))];
    }

    private void rebuildHomeUi() {
        String query=search==null?"":search.getText().toString();
        int previousSurface=activeSurface;
        applyAppearance();
        buildHome();
        if(previousSurface==SurfaceNavigation.HOME)activeSurface=SurfaceNavigation.HOME;
        search.setText(query);
        if(searchRender!=null)searchRenderHandler.removeCallbacks(searchRender);
        render(query,false,true);
        if(previousSurface==SurfaceNavigation.NOTIFICATIONS)showNotifications();
        else if(previousSurface==SurfaceNavigation.CONTROL)showControlCenter();
        else if(previousSurface==SurfaceNavigation.SETTINGS)settings();
    }

    @Override protected void onResume() {
        super.onResume();
        activityResumed=true;
        scheduleHomePageBuildFrame();
        if(iconsTrimmed||packagesDirty||appLoadPaused) {
            iconsTrimmed=false;
            packagesDirty=false;
            appLoadPaused=false;
            loadApps();
        }
        if(activeNotificationScreen!=null)activeNotificationScreen.onShown();
    }

    @Override protected void onPause() {
        activityResumed=false;
        cancelHomePageBuildFrame();
        // Do not keep scanning installed apps or rasterizing icons while another app is foreground.
        // Invalidate even a completed worker whose UI result is still queued on the main thread.
        if(appLoadNeedsApply) {
            appLoadGeneration++;
            if(appLoadTask!=null)appLoadTask.cancel(true);
            appLoadNeedsApply=false;
            appLoadPaused=true;
        }
        super.onPause();
        try {((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}catch(Exception ignored){}
    }

    @Override public void onBackPressed() {

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
        // TRIM_MEMORY_UI_HIDDEN is sent on ordinary app launches. Keep the home icons
        // warm for fast return; release them when Android actually demotes this process
        // into the background under memory pressure.
        if(level>=TRIM_MEMORY_BACKGROUND&&iconCache!=null) {
            appLoadGeneration++;
            if(appLoadTask!=null)appLoadTask.cancel(true);
            DRAWABLES.clear();
            clearImageDrawables(root);
            if(activeFolderDialog!=null)activeFolderDialog.dismiss();
            if(activeSurfaceView!=null)clearImageDrawables(activeSurfaceView);

            if(pageSnap!=null)pageSnapHandler.removeCallbacks(pageSnap);
            cancelHomePageBuildFrame();
            releaseActiveSurface();
            activeSurfaceView=null;
            activeSurface=SurfaceNavigation.HOME;
            setContentView(homeFrame);
            scheduleHomePageBuildFrame();
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
        appLoadGeneration++;
        if(appLoadTask!=null)appLoadTask.cancel(true);
        appLoader.shutdownNow();
        packageRefreshHandler.removeCallbacks(packageRefresh);
        if(searchRender!=null)searchRenderHandler.removeCallbacks(searchRender);
        if(pageSnap!=null)pageSnapHandler.removeCallbacks(pageSnap);
        cancelHomePageBuildFrame();
        releaseActiveSurface();
        activeSurfaceView=null;
        if(activeFolderDialog!=null)activeFolderDialog.dismiss();
        if(widgetsListening)try {appWidgetHost.stopListening();}catch(RuntimeException ignored){}
        widgetsListening=false;
        if(batteryReceiverRegistered)try {unregisterReceiver(batteryReceiver);}catch(Exception ignored){}
        batteryReceiverRegistered=false;
        if(torchController!=null){torchController.turnOff();if(torchController.getParent() instanceof ViewGroup)((ViewGroup)torchController.getParent()).removeView(torchController);torchController=null;}
        if(receiverRegistered)try{unregisterReceiver(packageReceiver);}catch(Exception ignored){}
        DRAWABLES.clear();
        if(iconCache!=null)iconCache.trim();
        super.onDestroy();
    }
}
