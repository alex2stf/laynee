package com.arise.rapdroid.media.server.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.VideoView;

public class MyVideoView extends VideoView {

    private int mVideoWidth;
    private int mVideoHeight;

    public MyVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MyVideoView(Context context) {
        super(context);
    }

    public void setVideoSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        setMeasuredDimension(width, height);
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        // Log.i("@@@", "onMeasure");
//        int screenWidth = View.getDefaultSize(mVideoWidth, widthMeasureSpec);
//        int screenHeight = View.getDefaultSize(mVideoHeight, heightMeasureSpec);
//        int calcWidth = screenWidth;
//        int calcHeight = screenHeight;
////        String screenMode = screenWidth > screenHeight ? "LANDSCAPE" : "PORTRAIT";
////        String videoMode = mVideoWidth > mVideoHeight ? "LANDSCAPE" : "PORTRAIT";
//
//
//        if (mVideoWidth > 0 && mVideoHeight > 0) {
//
//
//
//            if (mVideoWidth * calcHeight > calcHeight * mVideoHeight) {
//                // Log.i("@@@", "image too tall, correcting");
//                calcHeight = screenHeight;
//                calcWidth = calcHeight * mVideoHeight / mVideoWidth;
//                System.out.println(calcWidth);
//            }
//
//            if (mVideoWidth * calcHeight < calcWidth * mVideoHeight) {
//                // Log.i("@@@", "image too wide, correcting");
//                calcWidth = calcHeight * mVideoWidth / mVideoHeight;
//            }
//        }
//        // Log.i("@@@", "setting size: " + width + 'x' + height);
//        setMeasuredDimension(calcWidth, calcHeight);
//    }
}
