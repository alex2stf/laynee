package com.arise.rapdroid.media.server;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

public class AppViewPager extends ViewPager {
    public AppViewPager(@NonNull Context context) {
        super(context);
    }
    private boolean touchEnabled = true;

    public AppViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
       if(this.touchEnabled){
           return super.onTouchEvent(ev);
       }
       return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.touchEnabled){
            return super.onInterceptTouchEvent(ev);
        }
        return false;
    }

    public AppViewPager setTouchEnabled(boolean enabled) {
        this.touchEnabled = enabled;
        setEnabled(enabled);
        return this;
    }

    public boolean isTouchEnabled() {
        return touchEnabled;
    }
}
