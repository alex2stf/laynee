package com.arise.rapdroid.media.server.fragments;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arise.core.tools.Mole;
import com.arise.rapdroid.components.ContextFragment;
import com.arise.rapdroid.components.ui.Layouts;
import com.arise.rapdroid.media.server.Icons;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class LogFragment extends ContextFragment implements Mole.Appender {


    TextView root;
    ScrollView scrollView;
    Set<String> lines = new HashSet<>();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (scrollView == null){
            scrollView = new ScrollView(getContext());
            scrollView.setBackgroundColor(Icons.tab7Background);
            root = new TextView(getContext());
            root.setText(new Date() + "\n\n");
            root.setGravity(Gravity.BOTTOM);
            root.setPadding(20, 20, 20, 20);
            root.setMovementMethod(new ScrollingMovementMethod());
            for (String s: lines){
                root.append(s);
            }
            root.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            root.setPadding(10, 10, 10, 10);
            scrollView.addView(root, Layouts.Linear.matchParentMatchParent());
        }
        return scrollView;
    }




    @Override
    public void append(String id, Mole.Bag bag, String text) {
        String ps[] = id.split("\\.");
        id = ps[ps.length - 1];
        String line = id + ":" + bag.name() + "] " + text + "\n";
        if (root != null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    root.append(line);
                    root.post(new Runnable() {
                        @Override
                        public void run() {
                            if (root.getLayout() == null){
                                return;
                            }
                            int scrollAmount = root.getLayout().getLineTop(root.getLineCount()) - root.getHeight();
                            root.scrollTo(0, scrollAmount);
                        }
                    });

                    scrollView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    }, 1000);
                }
            });
        } else {
            lines.add(line);
        }
    }
}
