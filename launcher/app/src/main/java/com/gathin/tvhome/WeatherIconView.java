package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Lightweight vector-style weather icon; no fonts or downloaded image assets required. */
public final class WeatherIconView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int code = -1;
    private boolean day;
    public WeatherIconView(Context context, AttributeSet attrs) { super(context, attrs); }
    void setWeather(int code, boolean day) { this.code = code; this.day = day; invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save(); canvas.scale(getWidth() / 32f, getHeight() / 32f);
        paint.setStrokeWidth(1.6f); paint.setStrokeCap(Paint.Cap.ROUND);
        if (code < 0) {
            paint.setColor(0xFF94A3B8); paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(16, 16, 9, paint);
            canvas.restore(); return;
        }
        if (code <= 2) {
            if (day) {
                paint.setColor(0xFFFFD879); paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(13, 13, code == 0 ? 6 : 5, paint);
                for (int i=0;i<8;i++) {
                    double a=i*Math.PI/4;
                    canvas.drawLine(13+(float)Math.cos(a)*8,13+(float)Math.sin(a)*8,
                            13+(float)Math.cos(a)*10.5f,13+(float)Math.sin(a)*10.5f,paint);
                }
            } else {
                Path moon = new Path();
                moon.addCircle(13, 13, 8, Path.Direction.CW);
                Path cut = new Path(); cut.addCircle(17, 9, 8, Path.Direction.CW);
                moon.op(cut, Path.Op.DIFFERENCE);
                paint.setStyle(Paint.Style.FILL); paint.setColor(0xFFE3EFFF); canvas.drawPath(moon,paint);
            }
        }
        if (code != 0) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(0xFFE2EAF5);
            canvas.drawCircle(11, 19, 5, paint); canvas.drawCircle(18, 15, 7, paint);
            canvas.drawCircle(25, 19, 4.5f, paint); canvas.drawRoundRect(8,18,28,24,3,3,paint);
        }
        if (code >= 51) {
            paint.setColor(0xFF8ACDFF); paint.setStyle(Paint.Style.STROKE);
            if (code >= 71 && code <= 77 || code == 85 || code == 86) {
                for(int x=12;x<=24;x+=6) {
                    canvas.drawLine(x,26,x,31,paint);canvas.drawLine(x-2,27,x+2,30,paint);canvas.drawLine(x-2,30,x+2,27,paint);
                }
            } else if (code >= 95) {
                Path bolt=new Path();bolt.moveTo(19,23);bolt.lineTo(14,28);bolt.lineTo(19,28);bolt.lineTo(16,32);
                paint.setColor(0xFFFFD879);canvas.drawPath(bolt,paint);
            } else {
                for(int x=12;x<=24;x+=6)canvas.drawLine(x,26,x-2,30,paint);
            }
        }
        canvas.restore();
    }
}
