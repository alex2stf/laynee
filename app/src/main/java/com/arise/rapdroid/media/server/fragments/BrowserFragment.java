package com.arise.rapdroid.media.server.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arise.core.tools.AppCache;
import com.arise.core.tools.Mole;
import com.arise.rapdroid.SmartWebView;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.Icons;
import com.arise.rapdroid.media.server.appviews.SettingsView;
import com.arise.rapdroid.media.server.R;

import java.util.UUID;

public class BrowserFragment extends Fragment {
    private static final Mole log = Mole.getInstance(BrowserFragment.class);

    private SettingsView settingsView;



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    SmartWebView smartWebView;


    private String[][] predefined = new String[][]{
            {"youtube", "https://www.youtube.com/"},
            {"9gag", "https://9gag.com/"},
            {"facebook", "https://www.facebook.com/"}
    };


    private String currentUrl = "http://localhost:8221/health";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        currentUrl = AppCache.getString("currentUrl", "http://localhost:8221/health");

        if (smartWebView == null){
            SmartWebView.Resources res = new SmartWebView.Resources();
            res.menuButtonImage = R.drawable.ic_menu_light;
            res.menuBackgroundColor = Icons.tab2Background;
            res.searchBarColor = Icons.tab2Background;
            res.searchTextColor = Color.GRAY;

            smartWebView = new SmartWebView(getContext(), res);

            PopupMenu popupMenu = smartWebView.addSearchBar();
            Menu root = popupMenu.getMenu();
            root.add("Navigate");
            root.add("Send to device");
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getTitle().toString()){
                        case "Send to device":
                            AppUtil.showSendUrlOptions(getContext(), settingsView, smartWebView.url());
                            break;
                        case "Navigate":
                            showNavigationOptions();
                            break;
                    }
                    return false;
                }
            });
            smartWebView.init();
            smartWebView.setId(UUID.randomUUID().version());
            smartWebView.loadUrl(currentUrl);
//            smartWebView.loadUrl("https://live.rockfm.ro:8443/rockfm.aacp");
        }
        return smartWebView;
    }

    private void showNavigationOptions(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select page");
        String names[] = new String[predefined.length];
        for (int i = 0; i < predefined.length; i++){
            names[i] = predefined[i][0];
        }
        builder.setItems(names, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                smartWebView.loadUrl(predefined[i][1]);
            }
        });
        builder.create().show();
    }



    public BrowserFragment setSettingsView(SettingsView settingsView) {
        this.settingsView = settingsView;
        return this;
    }

    public void saveState() {
        if (smartWebView != null && smartWebView.getCurrentUri() != null) {
            AppCache.putString("currentUrl", smartWebView.getCurrentUri());
        }
    }

    public void goBack() {
        if (smartWebView != null){
            smartWebView.goToPrevious();
        }
    }
}
