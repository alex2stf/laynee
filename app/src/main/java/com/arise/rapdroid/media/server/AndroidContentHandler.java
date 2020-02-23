package com.arise.rapdroid.media.server;

import android.app.Service;
import android.content.Intent;

import com.arise.astox.net.models.http.HttpResponse;
import com.arise.core.tools.ContentType;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.Message;
import com.arise.weland.impl.ContentInfoProvider;
import com.arise.weland.model.ContentHandler;

import java.io.File;

public class AndroidContentHandler extends ContentHandler {


    private final Service service;

    private BackgroundPlayer player = BackgroundPlayer.INSTANCE;


    public AndroidContentHandler(Service service) {

        this.service = service;
    }



    @Override
    public HttpResponse play(String path) {
        if (isLocalMusic(path)){
            player.play(path, 0);
        }

        Intent brodcastMsg = new Intent();
        brodcastMsg.setAction("openFile");
        brodcastMsg.putExtra("path", path);
        service.sendBroadcast(brodcastMsg);
        return null;
    }

    @Override
    public HttpResponse stop(String string) {
        return null;
    }

    @Override
    public HttpResponse pause(String string) {
        return null;
    }

    @Override
    public void onMessageReceived(Message message) {
        Intent brodcastMsg = new Intent();
        brodcastMsg.setAction("onMessage");
        brodcastMsg.putExtra("message", message.toJson());
        service.sendBroadcast(brodcastMsg);

        //TODO save wall messages
    }

    private boolean isLocalMusic(String path){
        File file = new File(path);
        return file.exists() && ContentType.isMusic(file);
    }
}
