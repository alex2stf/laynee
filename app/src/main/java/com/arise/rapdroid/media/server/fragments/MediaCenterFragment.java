package com.arise.rapdroid.media.server.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arise.core.tools.AppCache;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.R;
import com.arise.rapdroid.media.server.appviews.ContentInfoDisplayer;
import com.arise.rapdroid.media.server.Icons;
import com.arise.rapdroid.media.server.appviews.SettingsView;
import com.arise.rapdroid.media.server.views.MediaDisplayer;
import com.arise.weland.dto.Playlist;
import com.arise.weland.dto.RemoteConnection;
import com.arise.rapdroid.components.ContextFragment;
import com.arise.rapdroid.components.ui.NavView;

import java.net.URI;
import java.net.URISyntaxException;

import static com.arise.rapdroid.media.server.AppUtil.AUTO_PLAY_VIDEOS;

public class MediaCenterFragment extends ContextFragment {
    private MediaPlaybackFragment mediaPlaybackFragment;



    private SettingsView settingsView;

    public MediaCenterFragment(){

    }

    NavView root;











    ContentInfoDisplayer localMusic;
    ContentInfoDisplayer localVideos;
    boolean autoplayVideos = false;


    String getText(){
        return autoplayVideos ? "Autoplay off" : "Autoplay on";
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

            autoplayVideos = AppCache.getBoolean(AUTO_PLAY_VIDEOS);

            localVideos.enableMenu(R.drawable.ic_menu_light)
                    .addMenu(getText(), new MediaDisplayer.OnMenuClickListener() {
                        @Override
                        public void onClick(MenuItem menuItem) {
                                 if (autoplayVideos){
                                     autoplayVideos = false;
                                 }
                                 else {
                                     autoplayVideos = true;
                                     mediaPlaybackFragment.startVideosAutoplay();
                                 }
                                 menuItem.setTitle(getText());
                                 AppCache.putBoolean(AUTO_PLAY_VIDEOS, autoplayVideos);
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
                                   String[] names = Playlist.displayableNames();
                                   builder.setItems(names, new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialogInterface, int i) {
                                           if (data.getDeviceStat() == null){
                                               System.out.println("WTF???");
                                           }
                                           addRemoteTab(data, names[i]);

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
}
