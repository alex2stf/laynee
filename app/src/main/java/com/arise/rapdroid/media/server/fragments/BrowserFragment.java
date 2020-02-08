package com.arise.rapdroid.media.server.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arise.core.tools.AppCache;
import com.arise.core.tools.Mole;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.SmartWebView;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.Icons;
import com.arise.rapdroid.media.server.appviews.SettingsView;
import com.arise.rapdroid.media.server.R;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.Playlist;
import com.arise.weland.utils.URLBeautifier;

import java.net.URLEncoder;
import java.util.UUID;

public class BrowserFragment extends Fragment {
    private static final Mole log = Mole.getInstance(BrowserFragment.class);

    private SettingsView settingsView;



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    SmartWebView smartWebView;




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
            root.add("Send to device");
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getTitle().toString()){
                        case "Send to device":
                            AppUtil.showSendUrlOptions(getContext(), settingsView, smartWebView.url());
                            break;
                    }
                    return false;
                }
            });
            smartWebView.init();
            smartWebView.setId(UUID.randomUUID().version());



            loadUrl(currentUrl);
        }
        return smartWebView;
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

    public void loadUrl(String path) {
        if (smartWebView != null){
            ContentInfo contentInfo = AppUtil.contentInfoProvider.findByPath(path);
            if (contentInfo != null){

                if (contentInfo.isMusic() && Playlist.STREAMS.equals(contentInfo.getPlaylist())){
                    //TODO configurable
                    path = "http://localhost:8221/player?imgSrc=" + contentInfo.getThumbnailId()
                            + "&audioSrc=" + contentInfo.getPath();
                }




                System.out.println(path);

            }


            smartWebView.loadUrl(path);
            saveState();
        }
    }


}
