package com.arise.rapdroid.media.server.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arise.rapdroid.components.ContextFragment;
import com.arise.rapdroid.media.server.Icons;
import com.arise.rapdroid.media.server.appviews.SettingsView;

public class SettingsFragment extends ContextFragment {


    private SettingsView settingsView;

    ScrollView root;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (root == null){
            root = new ScrollView(getContext());
            root.setBackgroundColor(Icons.tab4Background);
            if (settingsView != null){
                root.addView(settingsView);

                settingsView.setBackgroundColor(Icons.tab4Background);
            }
        }
        return root;
    }

    public SettingsFragment setSettingsView(SettingsView settingsView) {
        this.settingsView = settingsView;
        return this;
    }
}
