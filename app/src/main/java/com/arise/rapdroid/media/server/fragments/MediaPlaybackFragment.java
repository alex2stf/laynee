package com.arise.rapdroid.media.server.fragments;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arise.core.tools.AppCache;
import com.arise.core.tools.Mole;
import com.arise.core.tools.ThreadUtil;
import com.arise.rapdroid.components.ContextFragment;
import com.arise.rapdroid.components.MediaControls;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.MainActivity;
import com.arise.rapdroid.media.server.R;
import com.arise.rapdroid.media.server.views.MyVideoView;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.Playlist;
import com.arise.weland.utils.AppSettings;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.arise.rapdroid.media.server.AppUtil.PLAYBACK_STATE;
import static com.arise.rapdroid.media.server.AppUtil.contentInfoProvider;

public class MediaPlaybackFragment extends ContextFragment {





    private static final Mole log = Mole.getInstance(MediaPlaybackFragment.class);
    static int PLAYING = 1;
    static int STOPPED_BY_USER = 3;
    static int MEDIA_COMPLETED = 5;




//    ImageView musicImage;
    ThumbnailView thumbnailView;
    MyVideoView videoView;
    RelativeLayout root;
    ThreadUtil.TimerResult updateTimer;

    ContentInfo currentInfo;

    MediaControls mediaControls;
    Map<String, Drawable> arts = new HashMap<>();
    int controlsHideCnt = 0;



    View.OnClickListener showControls = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showMediaControls();
        }
    };
    private int numErrors =  0;



    public void showVideoFragment(){
        if (getActivity() != null && getActivity() instanceof MainActivity){
            ((MainActivity) getActivity()).showVideoFragment();
        }
    }

    private RelativeLayout.LayoutParams getParams(){
        RelativeLayout.LayoutParams params;
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
        }
        //portrait
        else {
            params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
        }
        params.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
        params.addRule(RelativeLayout.FOCUSABLE, 0);
        params.addRule(RelativeLayout.FOCUSABLES_TOUCH_MODE, 0);
        return params;

    }

    public void showMediaControls(){
        if (mediaControls != null){
            mediaControls.setVisibility(View.VISIBLE);
        }
        controlsHideCnt = 0;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       if (root == null){
           root = new RelativeLayout(getContext());


           root.setBackgroundColor(Color.BLACK);
           root.setOnClickListener(showControls);
           videoView = new MyVideoView(getContext());
           videoView.setOnClickListener(showControls);
           thumbnailView = new ThumbnailView(getContext());

           root.addView(videoView);
           videoView.setLayoutParams(getParams());
           root.addView(thumbnailView, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


           mediaControls = new MediaControls(getContext(), 150);




           RelativeLayout.LayoutParams params =
                   new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                           ViewGroup.LayoutParams.WRAP_CONTENT);
           params.addRule(RelativeLayout.ALIGN_BOTTOM, 1);
           params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
           params.addRule(RelativeLayout.ALIGN_TOP, 0);
           root.addView(mediaControls, params);
           mediaControls.setBackgroundColor(Color.TRANSPARENT);


           mediaControls.setCentralButtonImage(R.drawable.ic_play);
           mediaControls.setLeftButtonImage(R.drawable.ic_next);
           mediaControls.setRightButtonImage(R.drawable.ic_prev);


           mediaControls.getCentralButton().setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {

                   int state = AppCache.getInt(PLAYBACK_STATE, MEDIA_COMPLETED);
                   if (PLAYING == state){
                       saveState();
                       stopPlayer();
                       AppCache.putInt(PLAYBACK_STATE, STOPPED_BY_USER);

                   }
                   else {
                       play(AppUtil.getSavedContentInfo());
                   }
                   showMediaControls();
               }
           });

           mediaControls.getLeftButton().setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   showMediaControls();
               }
           });

           mediaControls.getRightButton().setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   stopPlayer();
                   AppCache.putInt(PLAYBACK_STATE, MEDIA_COMPLETED);
                   startAutoPlayIfRequired();
                   showMediaControls();
               }
           });

           //this is only internal
           restoreMediaPlaybackFragment();
       }
       return root;
    }

    private void restoreMediaPlaybackFragment(){

        ContentInfo cInfo = AppUtil.getSavedContentInfo();
        int state = AppCache.getInt(PLAYBACK_STATE, MEDIA_COMPLETED);

        if (cInfo != null  && PLAYING == state ){
            play(cInfo);
            showPauseButton();
            updateSeekBar();
            showVideoFragment();
        }
        else {
           playNextOnRestore();
        }

    }

    private void playNextOnRestore(){
        if (AppCache.getInt(PLAYBACK_STATE, MEDIA_COMPLETED) == STOPPED_BY_USER){
            stopPlayer();
            return;
        }

        Playlist auto = AppSettings.getAutoPlaylist();
        if (auto != null) {
            ContentInfo next = contentInfoProvider.nextFile(auto);
            if (next == null && !contentInfoProvider.finishedToScanAtLeastOnce()){
                ThreadUtil.delayedTask(new Runnable() {
                    @Override
                    public void run() {
                        playNextOnRestore();
                    }
                }, 2000);
            }
            else if (next != null){
                play(next);
            }
        }
    }

    private void stopPlayer(){
        videoView.setOnCompletionListener(null);
        videoView.setOnPreparedListener(null);
        if (videoView != null && videoView.isPlaying()){
            videoView.stopPlayback();
        }
        ThreadUtil.closeTimer(updateTimer);
        showPlayButton();
        saveState();

    }

    private void showPauseButton(){
        if (mediaControls != null){
            mediaControls.setCentralButtonImage(R.drawable.ic_pause);
            mediaControls.getCentralButton().setEnabled(true);
        }
        updateSeekBar();
    }

    private void showPlayButton(){
        if(mediaControls != null) {
            mediaControls.setCentralButtonImage(R.drawable.ic_play);
            mediaControls.getCentralButton().setEnabled(true);
        }
        updateSeekBar();
    }

    private void clearStateAndPlayNextIfRequired(){
        AppUtil.contentInfoProvider.clearState();
        startAutoPlayIfRequired();
    }

    private void startAutoPlayIfRequired(){
        int state = AppCache.getInt(PLAYBACK_STATE, MEDIA_COMPLETED);
        if (state == PLAYING || state == STOPPED_BY_USER){
            return;
        }
        Playlist playlist = AppSettings.getAutoPlaylist();
        if (playlist != null){
            play(contentInfoProvider.nextFile(playlist));
        }
    }

    public void play(final ContentInfo info) {
        if (info == null){
            return;
        }
        File f = new File(info.getPath());
        if (!f.exists()){
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentInfo = null;
                stopPlayer();
                videoView.setVisibility(View.VISIBLE);
                videoView.setLayoutParams(getParams());
                int width = info.getWidth();
                int height = info.getHeight();

                log.info("play media size " + width + " " + height);
                if (width > 0 || height > 0){
                    videoView.setVideoSize(width, height);
                }

                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        info.setDuration(mediaPlayer.getDuration());
                        int w = mediaPlayer.getVideoWidth();
                        int h = mediaPlayer.getVideoHeight();
                        if (w > 0 || h > 0){
                            videoView.setVideoSize(w, h);
                        }
                        videoView.seekTo(info.getPosition());
                        videoView.start();
                        AppCache.putInt(PLAYBACK_STATE, PLAYING);
                        currentInfo = AppUtil.saveCurrentInfo(info);
                        setupThumbnailViewIfRequired(info);
                        showPauseButton();
                        saveState();
                        startUpdateThread();
                        showVideoFragment();
                    }
                });
                videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                        log.error("ERROR DETECTED");
                        numErrors++;
                        if (numErrors < 10){
                            clearStateAndPlayNextIfRequired();
                        } else {
                            stopPlayer();
                            AppCache.putInt(PLAYBACK_STATE, STOPPED_BY_USER);
                            numErrors = 0;
                        }
                        return true;
                    }
                });
                videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        stopPlayer();
                        updateSeekBar();
                        clearState();
                        showMediaControls();
                        numErrors = 0;
                        AppCache.putInt(PLAYBACK_STATE, MEDIA_COMPLETED);
                        startAutoPlayIfRequired();
                    }
                });
                videoView.setVideoPath(info.getPath());
                updateSeekBar();
            } //exit main thread runnable
        });

    }

    private void setupThumbnailViewIfRequired(ContentInfo info) {
        if (thumbnailView != null){
            try {
                thumbnailView.regenerate();
                thumbnailView.setDisplayText(info.getName());
            }catch (Exception ex){

            }
        }
    }

    private void startUpdateThread(){
        updateTimer = ThreadUtil.repeatedTask(new Runnable() {
            @Override
            public void run() {
                saveState();
            }
        }, 500);
    }





    private void updateSeekBar(){
        if (mediaControls == null){
            return;
        }
        if (currentInfo != null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (thumbnailView != null){
                       try {
                           thumbnailView.invalidate();
                       }catch (Throwable t){

                       }
                    }
                    if (currentInfo != null && currentInfo.getPosition() > 0){
                        try {
                            if (controlsHideCnt > 10 && currentInfo.isVideo()){
                                mediaControls.setVisibility(View.INVISIBLE);
                            }
                            else {
                                mediaControls.setSeekBarMax(videoView.getDuration());
                                mediaControls.setSeekBarProgress(videoView.getCurrentPosition());
                            }
                            controlsHideCnt++;
                        } catch (Throwable t){
                            //mightthrow java.lang.IllegalStateException
                        }
                    }
                }
            });

        }
    }





    public void saveState() {
        if (videoView != null && currentInfo != null){
            try {

               int newPosition = videoView.getCurrentPosition();

               if (newPosition > currentInfo.getPosition()){
                   AppUtil.saveCurrentInfo(currentInfo);
               }
                currentInfo.setPosition(newPosition);
            } catch (Throwable t){
                //may throw  java.lang.IllegalStateException
            }

        }
        updateSeekBar();
    }

    private void clearState() {
        AppUtil.contentInfoProvider.clearState();
    }










    public void stop() {
        stopPlayer();
        AppCache.putInt(PLAYBACK_STATE, STOPPED_BY_USER);
    }
}
