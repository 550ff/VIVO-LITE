package com.vivolite.launcher;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.ArrayList;

public class FolderDialog extends Dialog {
    private final List<ImageView> iconViews = new ArrayList<>();
    public interface Listener {
        void onChanged(String title, List<String> children);
        void onLaunch(String key);
    }

    public FolderDialog(Context context, LauncherItem folder, List<LauncherItem> apps,
                        IconCache cache, boolean animations, Listener listener) {
        super(context);
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(context,20),Ui.dp(context,18),Ui.dp(context,20),Ui.dp(context,18));
        box.setBackground(Ui.panel(context, Ui.GLASS, 26));

        EditText title = new EditText(context);
        title.setSingleLine(true);
        title.setText(folder.title);
        title.setTextColor(Ui.primaryText());
        title.setTextSize(19);
        title.setBackgroundColor(Color.TRANSPARENT);
        title.setHintTextColor(Ui.secondaryText());
        title.setHint("Folder name");
        box.addView(title, new LinearLayout.LayoutParams(-1,Ui.dp(context,48)));

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(3);
        for (String child : folder.children) {
            LauncherItem found = find(apps, child);
            if (found == null) continue;
            LinearLayout cell = new LinearLayout(context);
            cell.setGravity(Gravity.CENTER);
            cell.setOrientation(LinearLayout.VERTICAL);
            ImageView icon = new ImageView(context);
            iconViews.add(icon);
            Drawable drawable = MainActivity.iconFor(found.id);
            if (drawable != null) icon.setImageDrawable(drawable);
            else icon.setImageResource(android.R.drawable.sym_def_app_icon);
            int iconSize=Math.min(Ui.ICON_DP,42);
            cell.addView(icon,new LinearLayout.LayoutParams(Ui.dp(context,iconSize),Ui.dp(context,iconSize)));
            TextView label = new TextView(context);
            label.setText(found.title); label.setTextColor(Ui.primaryText()); label.setTextSize(10);
            label.setGravity(Gravity.CENTER); label.setSingleLine(true);
            cell.addView(label,new LinearLayout.LayoutParams(Ui.dp(context,78),Ui.dp(context,24)));
            cell.setOnClickListener(v -> listener.onLaunch(found.id));
            Ui.attachTouchFeedback(cell,animations);
            cell.setOnLongClickListener(v -> {
                folder.children.remove(found.id);
                listener.onChanged(title.getText().toString(), folder.children);
                dismiss(); return true;
            });
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width=Ui.dp(context,82); p.height=Ui.dp(context,78); grid.addView(cell,p);
        }
        box.addView(grid,new LinearLayout.LayoutParams(-1,-2));

        TextView note = new TextView(context);
        note.setText("Tap an app to open · Long-press to remove");
        note.setTextColor(Ui.secondaryText()); note.setTextSize(11);
        note.setPadding(0,Ui.dp(context,8),0,Ui.dp(context,10));
        box.addView(note);

        TextView done = new TextView(context);
        done.setText("DONE"); done.setTextColor(Ui.primaryText()); done.setGravity(Gravity.CENTER);
        done.setBackground(Ui.panel(context,Ui.GLASS_LIGHT,18));
        box.addView(done,new LinearLayout.LayoutParams(-1,Ui.dp(context,44)));
        done.setOnClickListener(v -> {
            listener.onChanged(title.getText().toString().trim(), folder.children);
            dismiss();
        });
        Ui.attachTouchFeedback(done,animations);
        setContentView(box);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setLayout(Ui.dp(context,300),-2);
    }

    private static LauncherItem find(List<LauncherItem> apps,String id) {
        for (LauncherItem item:apps) if(item.id.equals(id)) return item;
        return null;
    }

    @Override public void dismiss() {
        for (ImageView icon : iconViews) icon.setImageDrawable(null);
        iconViews.clear();
        super.dismiss();
    }
}
