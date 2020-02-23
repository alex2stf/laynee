package com.arise.rapdroid.media.server.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arise.core.tools.ThreadUtil;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.components.MediaControls;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.MainActivity;
import com.arise.rapdroid.media.server.R;
import com.arise.rapdroid.media.server.WelandClient;
import com.arise.rapdroid.media.server.appviews.ContentInfoDisplayer;
import com.arise.rapdroid.media.server.Icons;
import com.arise.rapdroid.media.server.appviews.TouchPadView;
import com.arise.rapdroid.media.server.appviews.SettingsView;
import com.arise.rapdroid.media.server.views.MediaDisplayer;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.Playlist;
import com.arise.weland.dto.RemoteConnection;
import com.arise.rapdroid.components.ContextFragment;
import com.arise.rapdroid.components.ui.NavView;

import java.net.URI;
import java.net.URISyntaxException;



public class MediaCenterFragment extends ContextFragment {
    private MediaPlaybackFragment mediaPlaybackFragment;



    private SettingsView settingsView;
    private MainActivity mainActivity;

    public MediaCenterFragment(){

    }

    NavView root;











    ContentInfoDisplayer localMusic;
    ContentInfoDisplayer localVideos;
    MenuItem autoVideoItem;
    MenuItem autoMusicItem;


    String getAutoplayVideosText(){
        return AppUtil.isAutoplayVideos() ? "Autoplay off" : "Autoplay on";
    }

    String getAutoplayMusicText(){
        return AppUtil.isAutoplayMusic() ? "Autoplay music off" : "Autoplay music on";
    }

    //TODO meke this once:
    static String[] playlists = Playlist.displayableNames();
    static String[] names = new String[playlists.length + 1];
    static {
        for(int i = 0; i < playlists.length; i++){
            names[i] = playlists[i];
        }
        names[names.length - 1] = "CONTROL";
    }

    private void updateMenuItems(){
        if (autoVideoItem != null){
            autoVideoItem.setTitle(getAutoplayVideosText());
        }
        if (autoMusicItem != null){
            autoMusicItem.setTitle(getAutoplayMusicText());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (root == null) {
            root = new NavView(getContext())
                .setSelectedColor(Icons.tab1Foreground)
                .setReleasedColor(Icons.tab1Background);

            URI uri = null;
            try {
                uri = new URI("http://localhost:8221/");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            //todo ping before request

            localMusic = new ContentInfoDisplayer(getContext(), Icons.getDefaultInfoThumbnail(), uri, "music", "Music");

            localVideos = new ContentInfoDisplayer(getContext(), Icons.getDefaultInfoThumbnail(), uri, "videos", "Videos");


            localMusic.enableMenu(R.drawable.ic_menu_light)
                    .addMenu(getAutoplayMusicText(), new MediaDisplayer.OnMenuClickListener() {
                        @Override
                        public void onClick(MenuItem menuItem) {
                            autoMusicItem = menuItem;
                            if (AppUtil.isAutoplayMusic()){
                                AppUtil.setMusicAutoplay(false);
                            }
                            else {
                                AppUtil.setMusicAutoplay(true);
                                mediaPlaybackFragment.startMusicAutoplay();
                            }
                            updateMenuItems();

                        }
                    });

            localVideos.enableMenu(R.drawable.ic_menu_light)
                    .addMenu(getAutoplayVideosText(), new MediaDisplayer.OnMenuClickListener() {
                        @Override
                        public void onClick(MenuItem menuItem) {
                            autoVideoItem = menuItem;
                            if (AppUtil.isAutoplayVideos()){
                                AppUtil.setVideosAutoplay(false);
                            }
                            else {
                                AppUtil.setVideosAutoplay(true);
                                mediaPlaybackFragment.startVideosAutoplay();
                            }
                            updateMenuItems();

                        }
                    });




            root.addMenu(R.drawable.ic_local_music, R.drawable.ic_local_music_disabled, "Music", localMusic);
            root.addMenu(R.drawable.ic_local_video, R.drawable.ic_local_video_disabled, "Videos", localVideos);
            root.addMenu(R.drawable.ic_local_stream,
                    R.drawable.ic_local_stream_disabled, "Streams",
                    new ContentInfoDisplayer(getContext(), Icons.getDefaultInfoThumbnail(), uri, Playlist.STREAMS.name(), "Streams")
            );



            root.addButton(R.drawable.ic_media_add, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                   AppUtil.showConnectOptions(getContext(), settingsView, new CompleteHandler<RemoteConnection>() {
                       @Override
                       public void onComplete(RemoteConnection data) {

                           runOnUiThread(new Runnable() {
                               @Override
                               public void run() {
                                   AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                   builder.setTitle("Select list");


                                   builder.setItems(names, new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialogInterface, int i) {
                                           if (data.getDeviceStat() == null){
                                               System.out.println("WTF???");
                                           }
                                           if ("CONTROL".equals(names[i])){
                                                addRemoteControlTab(data);
                                           }
                                           else {
                                               addRemoteTab(data, names[i]);
                                           }
                                       }
                                   });
                                   builder.create().show();
                               }
                           });

                       }
                   });
                }
            });
        }

        return root;
    }


    TouchPadView touchPadView;

    public void addRemoteControlTab(RemoteConnection connection){

        if (this.touchPadView != null){
            this.touchPadView.setConnection(connection);
            lockTabLayout();
            touchPadView.unLockedIcon();
            return;
        }
        touchPadView = new TouchPadView(getContext());
        touchPadView.setConnection(connection);
        lockTabLayout();
        touchPadView.unLockedIcon();
        touchPadView.onButtonClick(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainActivity.isViewPagerLocked()) {
                    mainActivity.unlockViewPager();
                    ((ImageButton)view).setImageResource(R.drawable.ic_lock);
                }
                else {
                    mainActivity.lockViewPager();
                    ((ImageButton)view).setImageResource(R.drawable.ic_unlock);
                }
            }
        });
        root.addMenu(R.drawable.ic_touchpad, R.drawable.ic_touchpad_disabled, "Remote Ctrl", touchPadView);

    }

    void lockTabLayout(){
        if (mainActivity != null){
            mainActivity.lockViewPager();
        }
    }




    public void addRemoteTab(RemoteConnection data, String playlistName){

        ContentInfoDisplayer displayer =
                new ContentInfoDisplayer(getContext(), Icons.getDefaultInfoThumbnail(), data.getPayload(), playlistName, data.getName() + " " + playlistName);
        displayer.enableMenu(R.drawable.ic_menu_light);
        displayer.addMenu("Remove", new MediaDisplayer.OnMenuClickListener() {
            @Override
            public void onClick(MenuItem menuItem) {
                root.removeTab(displayer);
            }
        });

        displayer.addOption(new MediaDisplayer.Option() {
            @Override
            public String getTitle(ContentInfo info) {
                return "Stop";
            }

            @Override
            public void onClick(ContentInfo info) {
                WelandClient.stop(info, data.getPayload());
            }
        });
        root.addMenu(R.drawable.ic_local_music, R.drawable.ic_send, data.getDeviceStat().getDisplayName(), displayer);

    }










    public void updateAutoplay(){

    }







    public void setMediaPlaybackFragment(MediaPlaybackFragment mediaPlaybackFragment) {
        this.mediaPlaybackFragment = mediaPlaybackFragment;
    }




    public MediaCenterFragment setNeworkRefreshView(SettingsView settingsView) {
        this.settingsView = settingsView;
        return this;
    }

    public MediaCenterFragment setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        return this;
    }
}
