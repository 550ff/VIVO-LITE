package com.vivolite.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.TextPaint;
import android.view.View;

/**
 * Compact home-screen app cell. One View replaces the former nested
 * LinearLayout + ImageView + TextView hierarchy without changing gestures.
 */
final class LauncherAppCellView extends View {
    private final Paint platePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Rect iconBounds=new Rect();
    private final RectF plateBounds=new RectF();
    private final RectF glowBounds=new RectF();
    private float plateRadius;
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
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(1f,density));
        glowPaint.setStyle(Paint.Style.FILL);
        sheenPaint.setStyle(Paint.Style.FILL);
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

    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight) {
        super.onSizeChanged(width,height,oldWidth,oldHeight);
        float plateSize=Math.min(Ui.dp(getContext(),Ui.ICON_DP),width-Ui.dp(getContext(),10));
        plateSize=Math.max(Ui.dp(getContext(),32),plateSize);
        float left=(width-plateSize)*0.5f;
        float top=Ui.dp(getContext(),8);
        plateBounds.set(left,top,left+plateSize,top+plateSize);
        float glowInset=Ui.dp(getContext(),5);
        glowBounds.set(plateBounds.left-glowInset,plateBounds.top-glowInset,
                plateBounds.right+glowInset,plateBounds.bottom+glowInset);
        plateRadius=Ui.dp(getContext(),15);
        int accent=Ui.ACCENT;
        int glow=Color.argb(48,Color.red(accent),Color.green(accent),Color.blue(accent));
        int edge=Color.argb(92,Color.red(accent),Color.green(accent),Color.blue(accent));
        glowPaint.setShader(new RadialGradient(plateBounds.centerX(),plateBounds.centerY(),
                plateSize*0.82f,new int[]{glow,Color.TRANSPARENT},
                new float[]{0f,1f},Shader.TileMode.CLAMP));
        int topColor=blend(Ui.GLASS_LIGHT,Color.WHITE,0.10f);
        platePaint.setShader(new LinearGradient(0,plateBounds.top,0,plateBounds.bottom,
                topColor,Ui.GLASS_LIGHT,Shader.TileMode.CLAMP));
        sheenPaint.setShader(new LinearGradient(0,plateBounds.top,0,
                plateBounds.top+plateSize*0.72f,0x24FFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP));
        borderPaint.setColor(edge);
    }

    private static int blend(int from,int to,float amount) {
        float inverse=1f-amount;
        return Color.argb(Color.alpha(from),
                Math.round(Color.red(from)*inverse+Color.red(to)*amount),
                Math.round(Color.green(from)*inverse+Color.green(to)*amount),
                Math.round(Color.blue(from)*inverse+Color.blue(to)*amount));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width=getWidth();
        float height=getHeight();
        canvas.drawRoundRect(glowBounds,plateRadius+Ui.dp(getContext(),5),
                plateRadius+Ui.dp(getContext(),5),glowPaint);
        canvas.drawRoundRect(plateBounds,plateRadius,plateRadius,platePaint);
        canvas.drawRoundRect(plateBounds,plateRadius,plateRadius,sheenPaint);
        canvas.drawRoundRect(plateBounds,plateRadius,plateRadius,borderPaint);

        if(icon!=null) {
            int inset=Ui.dp(getContext(),3);
            iconBounds.set((int)plateBounds.left+inset,(int)plateBounds.top+inset,
                    (int)plateBounds.right-inset,(int)plateBounds.bottom-inset);
            icon.setBounds(iconBounds);
            icon.draw(canvas);
        }

        float labelY=height-Ui.dp(getContext(),8);
        CharSequence fitted=TextUtils.ellipsize(label,labelPaint,
                Math.max(0,width-Ui.dp(getContext(),8)),TextUtils.TruncateAt.END);
        canvas.drawText(fitted.toString(),width*0.5f,labelY,labelPaint);
    }
}
