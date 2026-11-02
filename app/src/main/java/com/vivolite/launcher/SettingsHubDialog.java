package com.vivolite.launcher;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Lightweight in-dialog settings hub with category navigation and actionable rows. */
public final class SettingsHubDialog extends Dialog {
    public interface Listener {
        String[] options(int category);
        String[] descriptions(int category);
        void onSelect(int category,int option);
    }

    private final Context context;
    private final String[] categories;
    private final String[] subtitles;
    private final Listener listener;
    private final LinearLayout panel, rows;
    private final TextView title, back;
    private int activeCategory=-1;

    public SettingsHubDialog(Context context,String[] categories,String[] subtitles,boolean animations,Listener listener) {
        super(context);
        this.context=context;this.categories=categories;this.subtitles=subtitles;this.listener=listener;
        panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(context,18),Ui.dp(context,16),Ui.dp(context,18),Ui.dp(context,14));
        panel.setBackground(Ui.panel(context,Ui.GLASS,24));
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        back=text("‹",28,Ui.primaryText(),false);back.setGravity(Gravity.CENTER);
        header.addView(back,new LinearLayout.LayoutParams(Ui.dp(context,42),Ui.dp(context,42)));
        title=text("SETTINGS",13,Ui.primaryText(),true);title.setLetterSpacing(0.12f);
        header.addView(title,new LinearLayout.LayoutParams(0,Ui.dp(context,42),1));
        panel.addView(header);
        ScrollView scroll=new ScrollView(context);scroll.setVerticalScrollBarEnabled(false);
        rows=new LinearLayout(context);rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows,new ScrollView.LayoutParams(-1,-2));
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(panel);
        Window window=getWindow();if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(Math.min(Ui.dp(context,400),context.getResources().getDisplayMetrics().widthPixels-Ui.dp(context,28)),
                    Math.min(Ui.dp(context,620),context.getResources().getDisplayMetrics().heightPixels-Ui.dp(context,56)));
            window.setWindowAnimations(animations?com.vivolite.launcher.R.style.VivoDialogAnimation:0);
        }
        back.setOnClickListener(v->{if(activeCategory>=0)showCategories();else dismiss();});
        showCategories();
    }

    private void showCategories() {
        activeCategory=-1;back.setVisibility(android.view.View.INVISIBLE);title.setText("SETTINGS");rows.removeAllViews();
        for(int i=0;i<categories.length;i++) {
            final int category=i;
            addRow(categoryIcon(i)+"  "+categories[i],subtitles[i],()->showOptions(category));
        }
    }

    private String categoryIcon(int category) {
        switch(category) {
            case 0:return "◉";
            case 1:return "▦";
            case 2:return "◌";
            case 3:return "▤";
            default:return "⚙";
        }
    }

    @Override public void onBackPressed() {
        if(activeCategory>=0)showCategories();
        else super.onBackPressed();
    }

    private void showOptions(int category) {
        activeCategory=category;back.setVisibility(android.view.View.VISIBLE);title.setText(categories[category].toUpperCase());rows.removeAllViews();
        String[] options=listener.options(category);
        String[] descriptions=listener.descriptions(category);
        for(int i=0;i<options.length;i++) {
            final int option=i;
            addRow(options[i],i<descriptions.length?descriptions[i]:"",()->{listener.onSelect(category,option);dismiss();});
        }
    }

    private void addRow(String heading,String detail,Runnable action) {
        LinearLayout card=new LinearLayout(context);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(context,14),Ui.dp(context,11),Ui.dp(context,14),Ui.dp(context,11));
        card.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,16));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.bottomMargin=Ui.dp(context,8);rows.addView(card,params);
        TextView name=text(heading,13,Ui.primaryText(),true);card.addView(name);
        TextView description=text(detail,10,Ui.secondaryText(),false);
        if(detail==null||detail.isEmpty())description.setVisibility(android.view.View.GONE);
        LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=Ui.dp(context,3);card.addView(description,dp);
        card.setOnClickListener(v->action.run());
    }

    private TextView text(String value,int size,int color,boolean bold) {
        TextView view=new TextView(context);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return view;
    }
}
