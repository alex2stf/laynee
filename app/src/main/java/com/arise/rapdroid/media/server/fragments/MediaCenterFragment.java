package com.arise.rapdroid.media.server.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.R;
import com.arise.rapdroid.media.server.appviews.ContentInfoDisplayer;
import com.arise.rapdroid.media.server.Icons;
import com.arise.rapdroid.media.server.MainActivity;
import com.arise.rapdroid.media.server.appviews.SettingsView;
import com.arise.weland.dto.Playlist;
import com.arise.weland.dto.RemoteConnection;
import com.arise.rapdroid.components.ContextFragment;
import com.arise.rapdroid.components.ui.NavView;

import java.net.URI;
import java.net.URISyntaxException;

public class MediaCenterFragment extends ContextFragment {
    private MediaPlaybackFragment mediaPlaybackFragment;



    private SettingsView settingsView;

    public MediaCenterFragment(){

    }

    NavView root;











    ContentInfoDisplayer localMusic;
    ContentInfoDisplayer localVideos;
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

            localMusic = new ContentInfoDisplayer(getContext(), Icons.getDefaultInfoThumbnail(), uri, "music");
            localMusic.setTitle("Music");
            localVideos = new ContentInfoDisplayer(getContext(), Icons.getDefaultInfoThumbnail(), uri, "videos");

            localVideos.setTitle("Movies");


            root.addMenu(R.drawable.ic_local_music, R.drawable.ic_local_music_disabled, "Music", localMusic);
            root.addMenu(R.drawable.ic_local_video, R.drawable.ic_local_video_disabled, "Videos", localVideos);



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
                                           root.addMenu(R.drawable.ic_local_music, R.drawable.ic_send, data.getDeviceStat().getDisplayName(),
                                                   new ContentInfoDisplayer(getContext(), Icons.getDefaultInfoThumbnail(), data.getPayload(), names[i])
                                           );
                                       }
                                   });
                                   builder.create().show();
                               }
                           });

                       }
                   });
                }
            });
//            root.addMenu(Icons.getLocalMedia(), "Video", localVideos);
//            root.show(0);


        }

        return root;
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
