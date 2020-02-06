package com.arise.rapdroid.media.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.IBinder;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.arise.astox.net.models.AbstractServer;
import com.arise.astox.net.models.http.HttpRequest;
import com.arise.astox.net.models.http.HttpResponse;
import com.arise.core.tools.Mole;
import com.arise.core.tools.ThreadUtil;
import com.arise.core.tools.Util;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.NotificationOps;
import com.arise.rapdroid.RAPDUtils;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.Message;
import com.arise.weland.model.ContentHandler;
import com.arise.weland.utils.Boostrap;
import com.arise.weland.utils.WelandServerHandler;
import com.arise.weland.utils.JPEGOfferResponse;
import com.arise.weland.utils.MJPEGResponse;
import com.arise.rapdroid.net.CamStreamResponse;
import com.arise.rapdroid.net.WavRecorderResponse;

import java.util.Date;

import static com.arise.weland.utils.Boostrap.startHttpServer;

//import static com.arise.weland.Main.start;

public class ServerService extends Service {

    private static final Mole log = Mole.getInstance(ServerService.class);

//    private volatile AbstractServer server;
    private volatile CamStreamResponse camStreamResponse;
    final MJPEGResponse mjpegResponse = new MJPEGResponse();
    final JPEGOfferResponse jpegOfferResponse = new JPEGOfferResponse();
    WavRecorderResponse wavRecorderResponse = new WavRecorderResponse();

    WelandServerHandler serverHandler = Boostrap.buildHandler(new String[]{}, AppUtil.contentInfoProvider);;
    AbstractServer server;
    public ServerService(){

        serverHandler.setLiveJpeg(jpegOfferResponse);
        serverHandler.setLiveMjpegStream(mjpegResponse);
        serverHandler.setLiveWav(wavRecorderResponse);




        serverHandler.beforeLiveJPEG(new WelandServerHandler.Handler<HttpRequest>() {
            @Override
            public HttpResponse handle(HttpRequest request) {
                if (!camStreamResponse.isRecording()){
                    camStreamResponse.startStream();
                }
                return null;
            }
        });

        serverHandler.beforeLiveMJPEG(new WelandServerHandler.Handler<HttpRequest>() {
            @Override
            public HttpResponse handle(HttpRequest request) {
                if (!camStreamResponse.isRecording()){
                    camStreamResponse.startStream();
                }
                return null;
            }
        });

        serverHandler.onMessageReceived(new WelandServerHandler.Handler<Message>() {
            @Override
            public HttpResponse handle(Message message) {
                System.out.printf("SERVER RECEIVED MESSAGE " + message);
                Intent brodcastMsg = new Intent();
                brodcastMsg.setAction("onMessage");
                brodcastMsg.putExtra("message", message.toJson());
                sendBroadcast(brodcastMsg);
                return null;
            }
        });

        serverHandler.setContentHandler(new AndroidContentHandler(this));




        serverHandler.onPlayAdvice(new CompleteHandler<ContentInfo>() {
            @Override
            public void onComplete(ContentInfo data) {
                if (data == null){
                    return;
                }
                Intent brodcastMsg = new Intent();
                brodcastMsg.setAction("playAdvice");
                brodcastMsg.putExtra("path", data.getPath());
                sendBroadcast(brodcastMsg);
            }
        });


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null){
            log("onStartCommand from system");
        }
        else {
            log("onStartCommand from app");
        }
        Util.registerContext(getApplicationContext());
        Util.registerContext(getApplication());
        return android.app.Service.START_STICKY;
    }

    private void startServer(){

        if (camStreamResponse != null){
            camStreamResponse.stop();
        }

        log.info("Server service started");


        camStreamResponse = new CamStreamResponse(mjpegResponse, jpegOfferResponse);

        //surface view refresh required
        SurfaceView sv = new SurfaceView(getApplicationContext());
        SurfaceTexture surfaceTexture = new SurfaceTexture(10);

        camStreamResponse.setPreview(surfaceTexture);

        ThreadUtil.fireAndForget(new Runnable() {
            @Override
            public void run() {

                server = startHttpServer(serverHandler);

                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("onStart");
                broadcastIntent.putExtra("http-connect", server.getConnectionPath());
                sendBroadcast(broadcastIntent);
            }
        });





        //TODO remove this
//        camStreamResponse.startStream();
        //Get a surface
        SurfaceHolder sHolder = sv.getHolder();
        //tells Android that this surface will have its data constantly replaced
        sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        notify(this);

    }




    @Override
    public void onDestroy() {
        log("onDestroy");
        Toast.makeText(this, "Server destroyed", Toast.LENGTH_LONG);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
        super.onDestroy();
    }


//    void startNativeVideo(String path){
//        Intent intent = new Intent(Intent.ACTION_VIEW );
//        intent.setDataAndType(Uri.parse(path), "video/*");
//        startActivity(intent);
//    }



    static final String CHANNEL_ID = "LAYNEE_CHANNEL";
    static final int NOTIFICATION_ID = (int) System.currentTimeMillis();

    public void notify(Context context) {

        NotificationOps notificationOps = new NotificationOps()
                .setSmallIcon(R.drawable.ic_all_out)
                .setTitle("Laynee service runnig")
                .setText("started at " + new Date())
                .setChannelId(CHANNEL_ID)
                .setChannelDescription("Laynee channel for server support")
                .setId(NOTIFICATION_ID)
                .setUncloseable();

        Notification notification = RAPDUtils.createNotification(context, notificationOps);

        startForeground(NOTIFICATION_ID, notification);
    }






    /**
     * this is called only once
     */
    @Override
    public void onCreate() {
        startServer();
        log("ON CREATE");


        super.onCreate();
    }


    private void log(String text){
        log.info(".........................................SRVLOG\n " + text + "\n\n\n\n");
    }

}
