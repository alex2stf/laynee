package com.arise.rapdroid.media.server.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.arise.core.tools.StringUtil;
import com.arise.rapdroid.media.server.R;
import com.arise.weland.dto.ContentInfo;

public class MediaIcon extends FrameLayout {


    private final ContentInfo mediaInfo;





    public void setBack(Bitmap bitmap){
        imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        requestLayout();
    }








    ImageView imageView;
    TextView textView;
    void init(int defaultRes, int size, String title){




        setLayoutParams(new FrameLayout.LayoutParams(size, size));

        imageView = new ImageView(getContext());
        imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));




        imageView.setBackgroundColor(Color.BLACK);
        imageView.setImageResource(defaultRes);
        textView = new TextView(getContext());
        textView.setText(minimize(title));
        textView.setGravity(Gravity.BOTTOM);
        textView.setTextColor(Color.WHITE);
        textView.setShadowLayer(1.5f, 2, 2, Color.BLACK);
        textView.setPadding(10, 0, 0, 10);

        addView(imageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(textView);
//        addView(textView,  new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public String minimize(String in){
        return in.length() < 18 ? in : (in.substring(0, 15) + "...");
    }


    public MediaIcon(Context ctx, ContentInfo mediaInfo, int defaultRes, int size) {
        super(ctx);
        this.mediaInfo = mediaInfo;
        String title = StringUtil.hasText(mediaInfo.getTitle()) ? mediaInfo.getTitle() : mediaInfo.getName();
        init(defaultRes, size, title);
    }


    public ContentInfo getMediaInfo() {
        return mediaInfo;
    }


}
