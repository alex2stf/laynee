package com.arise.rapdroid.media.server.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arise.rapdroid.components.ContextFragment;


public class MediaCenterFragment extends ContextFragment {


    private WebView root;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (root == null){
            root = new WebView(getContext());
            root.getSettings().setJavaScriptEnabled(true);
            root.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

            root.loadUrl("http://localhost:8221/app");
        }

        return root;
    }
}
