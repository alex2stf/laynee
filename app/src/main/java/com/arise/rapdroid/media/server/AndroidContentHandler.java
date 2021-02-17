package com.arise.rapdroid.media.server;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;

import com.arise.astox.net.models.AbstractServer;
import com.arise.astox.net.models.http.HttpResponse;
import com.arise.core.tools.ContentType;
import com.arise.core.tools.Mole;
import com.arise.rapdroid.AndroidContentDecoder;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.Message;
import com.arise.weland.impl.ContentInfoProvider;
import com.arise.weland.model.ContentHandler;

import java.io.File;

public class AndroidContentHandler extends ContentHandler {


    private final Service service;
    private static final Mole log = Mole.getInstance(AndroidContentHandler.class);

//    private BackgroundPlayer player = BackgroundPlayer.INSTANCE;



    public AndroidContentHandler(Service service) {
        this.service = service;
    }


    @Override
    protected HttpResponse openInfo(ContentInfo info) {
        return openPath(info.getPath());
    }

    @Override
    public HttpResponse openPath(String path) {
        log.info("received open " + path);
//        if (isLocalMusic(path)){
//            player.play(path, 0);
//        }

        if (MainActivity.isUrl(path)){
            Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(path));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            service.startActivity(intent);
            return null;
        }



        Intent brodcastMsg = new Intent();
        brodcastMsg.setAction("weland.openFile");
        brodcastMsg.putExtra("path", path);
        service.sendBroadcast(brodcastMsg);
        return null;
    }

    @Override
    protected HttpResponse pause(String path) {
        return stop(path);
    }


    @Override
    public HttpResponse stop(String string) {
        log.info("received stop " + string);
        Intent brodcastMsg = new Intent();
        brodcastMsg.setAction("weland.closeFile");
        brodcastMsg.putExtra("path", string);
        service.sendBroadcast(brodcastMsg);
        return null;
    }


    @Override
    public void onMessageReceived(Message message) {
        log.info("received message " + message);
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
