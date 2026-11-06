package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

/**
 * Compact home-screen app cell. One View replaces the former nested
 * LinearLayout + ImageView + TextView hierarchy without changing gestures.
 */
final class LauncherAppCellView extends View {
    private final Paint platePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect iconBounds=new Rect();
    private final String label;
    private final Drawable icon;
    private final float density;

    LauncherAppCellView(Context context,String label,Drawable icon) {
        super(context);
        this.label=label==null?"":label;
        Drawable.ConstantState state=icon==null?null:icon.getConstantState();
        this.icon=state==null?icon:state.newDrawable(context.getResources());
        density=getResources().getDisplayMetrics().density;
        platePaint.setStyle(Paint.Style.FILL);
        platePaint.setColor(Ui.GLASS_LIGHT);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(1f,density));
        borderPaint.setColor(Ui.subtleBorder());
        labelPaint.setStyle(Paint.Style.FILL);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(10f*density);
        labelPaint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        labelPaint.setColor(Ui.primaryText());
        setContentDescription(this.label);
        setFocusable(true);
        setClickable(true);
        setWillNotDraw(false);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width=getWidth();
        float height=getHeight();
        float plateSize=Math.min(Ui.dp(getContext(),Ui.ICON_DP),width-Ui.dp(getContext(),10));
        plateSize=Math.max(Ui.dp(getContext(),32),plateSize);
        float left=(width-plateSize)*0.5f;
        float top=Ui.dp(getContext(),8);
        float radius=Ui.dp(getContext(),15);
        canvas.drawRoundRect(left,top,left+plateSize,top+plateSize,radius,radius,platePaint);
        canvas.drawRoundRect(left,top,left+plateSize,top+plateSize,radius,radius,borderPaint);

        if(icon!=null) {
            int inset=Ui.dp(getContext(),3);
            iconBounds.set((int)left+inset,(int)top+inset,
                    (int)(left+plateSize)-inset,(int)(top+plateSize)-inset);
            icon.setBounds(iconBounds);
            icon.draw(canvas);
        }

        float labelY=height-Ui.dp(getContext(),8);
        CharSequence fitted=TextUtils.ellipsize(label,labelPaint,
                Math.max(0,width-Ui.dp(getContext(),8)),TextUtils.TruncateAt.END);
        canvas.drawText(fitted.toString(),width*0.5f,labelY,labelPaint);
    }
}
