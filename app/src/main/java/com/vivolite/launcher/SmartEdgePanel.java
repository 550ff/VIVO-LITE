package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** A launcher-owned edge drawer for direct, local quick actions. */
final class SmartEdgePanel extends LinearLayout {
    interface Listener { void onAction(String action); }

    SmartEdgePanel(Context context,List<LauncherItem> favorites,Listener listener,Runnable close) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(Ui.dp(context,18),Ui.dp(context,24),Ui.dp(context,14),Ui.dp(context,18));
        setBackground(Ui.panel(context,Ui.GLASS,26));
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels=new LinearLayout(context);labels.setOrientation(VERTICAL);
        TextView title=text(context,"SMART EDGE",13,Ui.primaryText(),true);title.setLetterSpacing(0.12f);labels.addView(title);
        labels.addView(text(context,"Quick tools",10,Ui.secondaryText(),false));
        header.addView(labels,new LayoutParams(0,Ui.dp(context,46),1));
        TextView closeButton=text(context,"×",24,Ui.primaryText(),false);closeButton.setGravity(Gravity.CENTER);
        header.addView(closeButton,new LayoutParams(Ui.dp(context,42),Ui.dp(context,42)));closeButton.setOnClickListener(v->close.run());
        Ui.attachTouchFeedback(closeButton,true);
        addView(header,new LayoutParams(-1,Ui.dp(context,48)));
        TextView toolsLabel=text(context,"TOOLS",9,Ui.secondaryText(),true);toolsLabel.setLetterSpacing(0.12f);
        LayoutParams section=new LayoutParams(-1,Ui.dp(context,28));section.topMargin=Ui.dp(context,12);addView(toolsLabel,section);
        addAction(context,"◷","Set timer","timer",listener);
        addAction(context,"＋","Calculator","calculator",listener);
        addAction(context,"▤","Quick note","note",listener);
        TextView appsLabel=text(context,"FAVOURITES",9,Ui.secondaryText(),true);appsLabel.setLetterSpacing(0.12f);
        LayoutParams favSection=new LayoutParams(-1,Ui.dp(context,28));favSection.topMargin=Ui.dp(context,16);addView(appsLabel,favSection);
        ScrollView scroll=new ScrollView(context);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout appRows=new LinearLayout(context);appRows.setOrientation(VERTICAL);scroll.addView(appRows,new ScrollView.LayoutParams(-1,-2));
        for(LauncherItem app:favorites) {
            TextView row=text(context,app.title,13,Ui.primaryText(),false);row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(context,12),0,Ui.dp(context,8),0);row.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,14));
            LayoutParams p=new LayoutParams(-1,Ui.dp(context,44));p.bottomMargin=Ui.dp(context,6);appRows.addView(row,p);
            row.setOnClickListener(v->listener.onAction("app:"+app.id));
            Ui.attachTouchFeedback(row,true);
        }
        if(favorites.isEmpty()) {
            TextView empty=text(context,"Favourite apps appear here after you add them from the home screen.",11,Ui.secondaryText(),false);
            empty.setPadding(Ui.dp(context,10),Ui.dp(context,8),Ui.dp(context,10),Ui.dp(context,8));appRows.addView(empty);
        }
        addView(scroll,new LayoutParams(-1,0,1f));
        TextView footer=text(context,"VIVO-LITE  ·  TOOLS",8,Ui.secondaryText(),true);footer.setGravity(Gravity.CENTER_VERTICAL);
        addView(footer,new LayoutParams(-1,Ui.dp(context,28)));
    }

    private void addAction(Context context,String glyph,String label,String action,Listener listener) {
        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,14));
        TextView icon=text(context,glyph,18,Ui.primaryText(),false);icon.setGravity(Gravity.CENTER);
        row.addView(icon,new LayoutParams(Ui.dp(context,40),Ui.dp(context,44)));
        TextView name=text(context,label,12,Ui.primaryText(),false);row.addView(name,new LayoutParams(0,Ui.dp(context,44),1));
        LayoutParams p=new LayoutParams(-1,Ui.dp(context,44));p.bottomMargin=Ui.dp(context,6);addView(row,p);
        row.setOnClickListener(v->listener.onAction(action));
        Ui.attachTouchFeedback(row,true);
    }

    private TextView text(Context context,String value,int size,int color,boolean bold) {
        TextView view=new TextView(context);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return view;
    }
}
