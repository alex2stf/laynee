package com.arise.rapdroid.media.server;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;

import com.arise.weland.dto.ContentInfo;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.ErrorListener;

public enum BackgroundPlayer {
    INSTANCE;

    volatile MediaPlayer mediaPlayer;
    Map<String, OnCompletionListener> completeMap = new ConcurrentHashMap<>();
    Map<String, OnPreparedListener> preparedMap = new ConcurrentHashMap<>();
    Set<String> completePending = Collections.synchronizedSet(new HashSet<>());
    Set<String> preparedPending = Collections.synchronizedSet(new HashSet<>());

    public BackgroundPlayer onCompletionListener(String id, OnCompletionListener onCompletionListener) {
        if (mediaPlayer == null){
            return this;
        }
        if (completePending.contains(id)){
            onCompletionListener.onCompletion(mediaPlayer);
        }
        else {
            completeMap.put(id, onCompletionListener);
        }
        return this;
    }

    public BackgroundPlayer onPreparedListener(String id, OnPreparedListener onPreparedListener) {
        if (mediaPlayer == null){
            return this;
        }
        if (preparedPending.contains(id)){
            onPreparedListener.onPrepared(mediaPlayer);
        }
        else {
            preparedMap.put(id, onPreparedListener);
        }
        return this;
    }

    MediaPlayer createInstance(){
        if (mediaPlayer == null){
            mediaPlayer = new MediaPlayer();
        }
        return mediaPlayer;
    }

    public void play(ContentInfo info) {
        play(info.getPath(), info.getPosition());
    }

    String currentId;


     /**
     * called by service
     * @param path
     */
    void play(String path, int position) {
        currentId = path;
        stop();
        createInstance();
        mediaPlayer.reset(); //Resets the MediaPlayer to its uninitialized state.
        preparedPending.clear();//clear pendinglisteners
        completePending.clear();//clear pending listeners

        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (completeMap.containsKey(path)){
                    completeMap.get(path).onCompletion(mediaPlayer);
                }
                completePending.add(path);
            }
        });
        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.seekTo(position);
                mediaPlayer.start();
                if (preparedMap.containsKey(path)){
                    preparedMap.get(path).onPrepared(mediaPlayer);
                    preparedMap.clear();
                }

                preparedPending.add(path);

            }
        });

        try {
            mediaPlayer.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.prepareAsync();
    }

    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()){
                mediaPlayer.stop();
            }
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void resume(){
        if (mediaPlayer != null){
            mediaPlayer.start();
        }
    }

    public void destroy(){
        if (mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();//If not released, too many MediaPlayer instances may result in an exception
            mediaPlayer = null;
        }
        completePending.clear();
        preparedPending.clear();
        completeMap.clear();
        preparedMap.clear();
        currentId = null;
    }

    public boolean isPlaying(){
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public boolean isPlaying(ContentInfo info){
        return mediaPlayer != null && mediaPlayer.isPlaying() && currentId.equals(info.getPath());
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }
}
