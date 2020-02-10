package com.arise.rapdroid.media.server;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import com.arise.astox.net.clients.JHttpClient;
import com.arise.astox.net.models.AbstractClient;
import com.arise.core.tools.SYSUtils;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.weland.Client;
import com.arise.weland.dto.AutoplayMode;
import com.arise.weland.dto.ContentPage;
import com.arise.weland.dto.DeviceStat;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.Message;
import com.arise.weland.dto.Playlist;
import com.arise.weland.dto.RemoteConnection;
import com.arise.rapdroid.BluetoothHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WelandClient {
    static String bluetoothUUID = "fa87c0d0-afac-11de-8a39-0800200c9a66";



//    static Client client = new Client();

    static Map<String, AbstractClient> workersCache = new ConcurrentHashMap<>();
    static Map<String, Client> clientsCache = new ConcurrentHashMap<>();

    public static String getWorkerId(Object worker){
        if (worker instanceof BluetoothDevice){
            return ((BluetoothDevice) worker).getName() + ((BluetoothDevice) worker).getAddress();
        }
        return worker.toString();
    }

    static CompleteHandler<Throwable> ERROR_HANDLER = new CompleteHandler<Throwable>() {
        @Override
        public void onComplete(Throwable data) {
            data.printStackTrace();
        }
    };

    static synchronized AbstractClient getWorker(Object worker){
        String id = getWorkerId(worker);
        if (workersCache.containsKey(id) && workersCache.get(id) != null){
            return workersCache.get(id);
        }
        AbstractClient abstractClient = null;
        if (worker instanceof BluetoothDevice){
            abstractClient = new BluetoothHttpClient((BluetoothDevice) worker)
                    .setUuid(bluetoothUUID).setErrorHandler(ERROR_HANDLER);
            workersCache.put(id, abstractClient);
        }
        else if (worker instanceof URI){
            abstractClient = new JHttpClient().setUri((URI) worker).setErrorHandler(ERROR_HANDLER);
            workersCache.put(id, abstractClient);
        }
        return abstractClient;
    }

    static synchronized Client getClient(Object worker){
        AbstractClient client = getWorker(worker);
        String id = client.getId();
        if (clientsCache.containsKey(id)){
            clientsCache.get(id).setClient(client);
            return clientsCache.get(id);
        }
        Client welandClient = new Client().setClient(client);
        clientsCache.put(id, welandClient);
        return welandClient;
    }

    public static void pingBluetooth(BluetoothDevice device, CompleteHandler<DeviceStat> handler, CompleteHandler<Throwable> errorHandler) {
        getClient(device).ping(handler, errorHandler);
    }

    public static void pingHttp(URI uri, CompleteHandler<DeviceStat> handler, CompleteHandler<Throwable> errorHandler){
        getClient(uri).ping(handler, errorHandler);
    }

    public static void pingHttp(String ip, int port, CompleteHandler<DeviceStat> handler, CompleteHandler<Throwable> errorHandler){
        try {
            URI uri = new URI("http://" + ip + "/" + port);
            getClient(uri).ping(handler, errorHandler);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }





    public static void mediaList(Object worker, String playlistId, Integer index, CompleteHandler<ContentPage> completeHandler, CompleteHandler onError){
        getClient(worker).mediaList(playlistId, index, completeHandler, onError);
    }



    public static void openInRemoteBrowser(Object worker, String url) {
        getClient(worker).openInBrowser(url);
    }



    public static void sendTextMessage(RemoteConnection remoteConnection, String text, Message.Type type, CompleteHandler<MessageResponse> onSucces, CompleteHandler onError) {
        Object worker = remoteConnection.getPayload();
        Client client = getClient(worker);
        String receiverId = client.clientId();
        String senderId = SYSUtils.getDeviceName();
        String messageId;

        String conversationId = remoteConnection.getDeviceStat().getConversationId();

        messageId = senderId + receiverId + conversationId + new Date();

        Message message = new Message();
        message.setId(messageId)
                .setConversationId(conversationId)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setType(type)
                .setText(text);
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.message = message;
        getClient(worker).sendMessage(message, new CompleteHandler<DeviceStat>() {
            @Override
            public void onComplete(DeviceStat data) {
                messageResponse.deviceStat = data;
                onSucces.onComplete(messageResponse);
            }
        }, onError);
    }

    public static void openFile(ContentInfo info, Object worker) {
        getClient(worker).openFile(info.getPath());
    }




    public static void findThumbnail( Object worker, String thumbnailId, CompleteHandler<byte[]> completeHandler) {
        getClient(worker).findThumbnail(thumbnailId, completeHandler, new CompleteHandler() {
            @Override
            public void onComplete(Object data) {
                System.out.println("error");
            }
        });
    }

    public static void shuffle(Object worker, String playlistId, CompleteHandler onComplete) {
        getClient(worker).shuffle(playlistId, onComplete);
    }

    public static void autoplay(Object worker, String playlistId, AutoplayMode autoplayMode, CompleteHandler onComplete) {
        getClient(worker).autoplay(Playlist.find(playlistId), autoplayMode, onComplete);
    }

    public static void stop(ContentInfo info, Object worker) {
        getClient(worker).close(info.getPath());
    }


    public static class MessageResponse {
       public Message message;
       public DeviceStat deviceStat;
    }
}
