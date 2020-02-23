package com.arise.rapdroid.media.server.appviews;

import android.view.MotionEvent;

public interface DrawingHandler {

    void onActionDown(DrawingView drawingView, MotionEvent event);

    void onMove(DrawingView drawingView, MotionEvent event);

    void onActionUp(DrawingView drawingView, MotionEvent event);

    void onConstruct();


}
