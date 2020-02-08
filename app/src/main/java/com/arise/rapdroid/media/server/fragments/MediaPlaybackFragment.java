package com.arise.rapdroid.media.server.fragments;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arise.core.tools.AppCache;
import com.arise.core.tools.Mole;
import com.arise.core.tools.StringUtil;
import com.arise.core.tools.ThreadUtil;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.BackgroundPlayer;
import com.arise.rapdroid.media.server.MainActivity;
import com.arise.rapdroid.media.server.R;
import com.arise.weland.dto.ContentInfo;
import com.arise.rapdroid.components.ContextFragment;
import com.arise.rapdroid.media.server.views.MyVideoView;
import com.arise.weland.dto.Playlist;

import java.util.HashMap;
import java.util.Map;

import static com.arise.rapdroid.media.server.AppUtil.AUTO_PLAY_VIDEOS;
import static com.arise.rapdroid.media.server.AppUtil.PLAYBACK_STATE;

public class MediaPlaybackFragment extends ContextFragment {





    private static final Mole log = Mole.getInstance(MediaPlaybackFragment.class);
    static int PLAYING_MUSIC = 1;
    static int PLAYING_VIDEO = 2;
    static int PAUSED_MUSIC = 3;
    static int PAUSED_VIDEO = 4;
    static int STOPPED = 5;

    private final BackgroundPlayer musicPlayer = BackgroundPlayer.INSTANCE;
    SeekBar seekBar;
    ImageButton playBtn;
    ImageView musicImage;
    MyVideoView videoView;
    RelativeLayout root;
    ThreadUtil.TimerResult updateTimer;
    ContentInfo currentInfo;
    int state;
    View mediaControls;

    MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer local) {
            videoView.stopPlayback();
            musicPlayer.stop();
            ThreadUtil.closeTimer(updateTimer);
            if (currentInfo != null) {
                currentInfo.setPosition(0);
            }
            clearState();
            state = STOPPED;
            AppCache.putInt("playback-state", state);
            if (AppCache.getBoolean(AUTO_PLAY_VIDEOS)){
                startVideosAutoplay();
            }
        }
    };
    Map<String, Drawable> arts = new HashMap<>();

    public void setMediaCenter(MediaCenterFragment mediaCenterFragment) {
        mediaCenterFragment.setMediaPlaybackFragment(this);
    }

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       if (root == null){
           root = new RelativeLayout(getContext());
           root.setBackgroundColor(Color.BLACK);
           videoView = new MyVideoView(getContext());
//           videoView.setOnCompletionListener(onCompletionListener);
           musicImage = new ImageView(getContext());
           musicImage.setImageResource(R.drawable.ic_tab_chat_disabled);
           musicImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
           root.addView(videoView);
           videoView.setLayoutParams(getParams());
           root.addView(musicImage, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


           mediaControls = inflater.inflate(R.layout.new_app_widget, null);

           RelativeLayout.LayoutParams params =
                   new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                           350);
           params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
           root.addView(mediaControls, params);
           mediaControls.setBackgroundColor(Color.TRANSPARENT);


           mediaControls.findViewById(R.id.thumbnail_preview).setVisibility(View.GONE);
           seekBar = mediaControls.findViewById(R.id.seek_bar);
           playBtn = mediaControls.findViewById(R.id.btn_play);
           playBtn.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   if (state == PLAYING_MUSIC || state == PLAYING_VIDEO){
                       pausePlayer();
                   }
                   else if (state == PAUSED_MUSIC || state == PAUSED_VIDEO || state == STOPPED){
                      resumePlayer();
                   }
                   else if (currentInfo != null){
                       play(currentInfo);
                   }
               }
           });
           //this is only internal
           restoreMediaPlaybackFragment();
       }
       return root;
    }

    private void restoreMediaPlaybackFragment(){
        currentInfo = AppUtil.contentInfoProvider.getCurrentInfo();
        state = AppCache.getInt("playback-state", STOPPED);
        if (currentInfo == null){
            return;
        }

        if (currentInfo.isMusic()){
            if (state == PLAYING_MUSIC && musicPlayer.isPlaying(currentInfo)) {
                play(currentInfo); //load handlers
                showPauseButton();
                updateSeekBar();
                showVideoFragment();
            }
            else { //background music stopped by itself
                state = STOPPED;
                placeThumbnails(currentInfo);
                showPlayButton();
            }
        }
        else if(currentInfo.isVideo() ){
            if (state == PLAYING_VIDEO || AppCache.getBoolean(AUTO_PLAY_VIDEOS)) {
                play(currentInfo);
                showVideoFragment();
            }
        }
        else {
            System.out.println("CURRENT INFO CANNOT BE PLAYED");
        }
    }

    private void resumePlayer() {
        ThreadUtil.closeTimer(updateTimer);
        if (state == PAUSED_VIDEO) {
            videoView.resume();
            state = PLAYING_VIDEO;
            showPauseButton();
            startUpdateThread();
        }
        else if (state == PAUSED_MUSIC) {
            musicPlayer.resume();
            state = PLAYING_VIDEO;
            showPauseButton();
            startUpdateThread();
        }
        else if (state == STOPPED && currentInfo != null){
            play(currentInfo);
        }
        else {
            log.error("!!!!!!INVALID playback state " + state);
            return;
        }
    }

    private void pausePlayer(){

        if (videoView.isPlaying()){
            videoView.pause();
            state = PAUSED_VIDEO;
        } else {
            musicPlayer.pause();
            state = PAUSED_MUSIC;
        }
        AppCache.putInt(PLAYBACK_STATE, state);
        ThreadUtil.closeTimer(updateTimer);
        showPlayButton();
        saveState();
    }

    private void showPauseButton(){
        AppCache.putInt(PLAYBACK_STATE, state);
        playBtn.setImageResource(android.R.drawable.ic_media_pause);
        playBtn.setEnabled(true);
        updateSeekBar();
    }

    private void showPlayButton(){
        AppCache.putInt(PLAYBACK_STATE, state);
        if(playBtn != null) {
            playBtn.setImageResource(android.R.drawable.ic_media_play);
            playBtn.setEnabled(true);
        }
        updateSeekBar();
    }

    private void setupMusicPlayerListeners(ContentInfo info){
        musicPlayer.onCompletionListener(info.getPath(), new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onCompletionListener.onCompletion(mediaPlayer);
                    }
                });
            }
        });

        musicPlayer.onPreparedListener(info.getPath(), new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                currentInfo.setDuration(mediaPlayer.getDuration());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        state = PLAYING_MUSIC;
                        showPauseButton();
                        saveState();
                        startUpdateThread();
                    }
                });
            }
        });
    }

    public void play(ContentInfo info) {
        if (info == null){
            return;
        }
        currentInfo = info;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //first stop videoView
                videoView.stopPlayback();

                //music player:
                if (info.isMusic()){
                    if(!musicPlayer.isPlaying(info)){
                        musicPlayer.play(info);
                    }
                    musicImage.setVisibility(View.VISIBLE);
                    placeThumbnails(info);
                    setupMusicPlayerListeners(info);
                    log.info("setup music preview");
                    return;
                }

                //video player:
                musicImage.setVisibility(View.INVISIBLE);
                musicPlayer.destroy(); //destroy background music player
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
                        currentInfo.setDuration(mediaPlayer.getDuration());
                        int w = mediaPlayer.getVideoWidth();
                        int h = mediaPlayer.getVideoHeight();
                        if (w > 0 || h > 0){
                            videoView.setVideoSize(w, h);
                        }
                        videoView.seekTo(currentInfo.getPosition());
                        videoView.start();
                        state = PLAYING_VIDEO;
                        showPauseButton();
                        saveState();
                        startUpdateThread();
                    }
                });
                videoView.setOnCompletionListener(onCompletionListener);
                videoView.setVideoPath(info.getPath());
                updateSeekBar();
            } //exit main thread runnable
        });

    }

    private void startUpdateThread(){
        updateTimer = ThreadUtil.repeatedTask(new Runnable() {
            @Override
            public void run() {
                saveState();
            }
        }, 1000);
    }

    private void placeThumbnails(ContentInfo info) {
        String artId = info.getThumbnailId();
        if (!StringUtil.hasText(artId)){
            return;
        }

        Drawable art = arts.get(artId);

        if (art == null){
            Bitmap bitmap = AppUtil.DECODER.getBitmapById(artId);
            if (bitmap == null){
                return;
            }
            art = new BitmapDrawable(getResources(), bitmap);
            arts.put(artId, art);
        }

        musicImage.setImageDrawable(art);

    }



    private void updateSeekBar(){
        if (seekBar == null){
            return;
        }
        if (currentInfo != null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currentInfo != null && currentInfo.getPosition() > 0){
                        try {
                            seekBar.setMax(currentInfo.getDuration());
                            seekBar.setProgress(currentInfo.getPosition());
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
               int newPosition = state == PLAYING_VIDEO ? videoView.getCurrentPosition() : musicPlayer.getCurrentPosition();
               if (newPosition > currentInfo.getPosition()){
                   currentInfo.setPosition(newPosition);
                   AppUtil.contentInfoProvider.saveState(currentInfo);
               }
            } catch (Throwable t){
                //may throw  java.lang.IllegalStateException
            }

        }
        updateSeekBar();
    }

    private void clearState() {
        AppUtil.contentInfoProvider.clearState();
    }


    public void playAdvice(ContentInfo contentInfo) {
        if (videoView != null && !videoView.isPlaying() && state == STOPPED && !AppCache.getBoolean(AUTO_PLAY_VIDEOS)){
            log.trace(" received advice " + contentInfo.getPath());
            play(contentInfo);
        }
        else {
            log.trace("skip advice " + contentInfo.getPath());
        }
    }


    public void startVideosAutoplay() {
        if (state != PLAYING_VIDEO) {
            ContentInfo contentInfo = AppUtil.contentInfoProvider.next(Playlist.VIDEOS);
            if (contentInfo != null) {
                play(contentInfo);
            }
        }
        showVideoFragment();
    }


}
