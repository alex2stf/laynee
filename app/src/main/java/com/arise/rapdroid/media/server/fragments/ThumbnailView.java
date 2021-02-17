package com.arise.rapdroid.media.server.fragments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.arise.core.tools.ProgressiveRGBGenerator;
import com.arise.core.tools.StringUtil;

import org.w3c.dom.Text;

import java.util.Random;

public class ThumbnailView extends View {
    Random random = new Random();

    private boolean mSizeChanged;
    private int width = 1;
    private int height = 1;
    private String displayText;


    public ThumbnailView(Context context) {
        super(context);
        setupDrawing();
    }

    public ThumbnailView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }


    public ThumbnailView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupDrawing();
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ThumbnailView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setupDrawing();
    }



    private void setupDrawing() {
        regenerate();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
    }


    ProgressiveRGBGenerator lineGenerator = new ProgressiveRGBGenerator(255, 145);



    static  int BARS_TOP = 0;
    static  int BARS_BOTTOM = 1;
    static  int BARS_MIDDLE_UNIFORM = 2;
    static  int BARS_MIDDLE_RANDOM = 3;

    int mode = BARS_BOTTOM;

    int colorStepFactor = 2;
    int textXPosition = 0;

    @Override
    protected void onDraw(Canvas canvas) {


//        ProgressiveRGBGenerator.RGB back = backGenerator.next();


//        canvas.drawColor(Color.argb(255, back.R(), back.G(), back.B()));
        canvas.drawColor(Color.BLACK);


        int strokeWidth = 20;

        int lines = width / strokeWidth;
        int wStep = width / lines;



        for (int i = 1; i < lines; i++){

            ProgressiveRGBGenerator.RGB rgb = lineGenerator.next(colorStepFactor);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(
                    Color.argb(255, rgb.R(), rgb.G(), rgb.B())
            );
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(strokeWidth - 4);


            float from = 0;
            float to = 0;
            if (mode == BARS_MIDDLE_UNIFORM){
                int middle = height / 4;
                int rand = random.nextInt(middle);
                from = middle - rand;
                to = middle + rand;
            }
            else if(mode == BARS_MIDDLE_RANDOM){
                int middle = height / 4;
                from = middle - random.nextInt(middle);
                to = middle + random.nextInt(middle);
            }
            else if (mode == BARS_BOTTOM){
                to = height / 2;
                from = random.nextInt(height / 3);
            }
            else {
                from = 0;
                to = random.nextInt(height / 2);
            }

            canvas.drawLine(wStep * i, from, wStep * i , to, paint);
        }

        if (StringUtil.hasText(displayText)) {
//            textXPosition = textXPosition - 10;
//
//            if (textXPosition < -width){
//                textXPosition = width;
//            }

            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(40);
            canvas.drawText(displayText, textXPosition, (height / 2) + 42, paint);
        }



        super.onDraw(canvas);
//        mHandler.postDelayed(this, 100);
    }


    public void regenerate() {
        int max = 8;
        int min = 2;
        colorStepFactor =  random.nextInt((max - min) + 1) + min;
        textXPosition = 0;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }
}
