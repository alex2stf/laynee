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
import com.arise.astox.net.servers.draft_6455.WSDraft6455;
import com.arise.astox.net.servers.io.IOServer;
import com.arise.core.tools.ContentType;
import com.arise.core.tools.Mole;
import com.arise.core.tools.SYSUtils;
import com.arise.core.tools.ThreadUtil;
import com.arise.core.tools.Util;
import com.arise.rapdroid.NotificationOps;
import com.arise.rapdroid.RAPDUtils;
import com.arise.rapdroid.net.CamStreamResponse;
import com.arise.rapdroid.net.WavRecorderResponse;
import com.arise.weland.impl.IDeviceController;
import com.arise.weland.impl.WelandRequestBuilder;
import com.arise.weland.utils.JPEGOfferResponse;
import com.arise.weland.utils.MJPEGResponse;
import com.arise.weland.utils.WelandServerHandler;

import java.util.Date;
import java.util.UUID;

//import static com.arise.weland.Main.start;

public class ServerService extends Service {

    static final String CHANNEL_ID = "LAYNEE_CHANNEL";
    static final int NOTIFICATION_ID = (int) System.currentTimeMillis();
    private static final Mole log = Mole.getInstance(ServerService.class);
    final MJPEGResponse mjpegResponse = new MJPEGResponse();
    final JPEGOfferResponse jpegOfferResponse = new JPEGOfferResponse();
   final WelandServerHandler serverHandler;
   final WelandRequestBuilder requestBuilder;
    WavRecorderResponse wavRecorderResponse = new WavRecorderResponse();
    AbstractServer server;
//    private volatile AbstractServer server;
    private volatile CamStreamResponse camStreamResponse;




    public ServerService(){

        serverHandler = new WelandServerHandler().setContentProvider(AppUtil.contentInfoProvider);
        requestBuilder = new WelandRequestBuilder(new IDeviceController() {
            @Override
            public void digestBytes(byte[] x) {

            }
        });

        try {
            ContentType.loadDefinitions();
            log.info("Successfully loaded content-type definitions");
        } catch (Exception e){
            log.error("Failed to load content-type definitions", e);
        }

        serverHandler.setLiveJpeg(jpegOfferResponse);
        serverHandler.setLiveMjpegStream(mjpegResponse);
        serverHandler.setLiveWav(wavRecorderResponse);



        serverHandler.beforeLiveWAV(new WelandServerHandler.Handler<HttpRequest>() {
            @Override
            public HttpResponse handle(HttpRequest data) {
                if (!wavRecorderResponse.isRecording()){
                    wavRecorderResponse.startRecord();
                }
                return null;
            }
        });

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


        serverHandler.setContentHandler(new AndroidContentHandler(this));







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

                server = new IOServer()
                        .setPort(8221)
                        .setName("DR_" + SYSUtils.getDeviceName())
                        .setUuid(UUID.randomUUID().toString())
                        .setRequestBuilder(requestBuilder)
                        .addDuplexDraft(new WSDraft6455())
                        .setHost("localhost")
                        .setStateObserver(serverHandler)
                        .setRequestHandler(serverHandler);
                try {
                    server.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Failed to start server", e);
                }

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

        if (wavRecorderResponse != null){
            wavRecorderResponse.stopRecording();
        }

        if (camStreamResponse != null){
            camStreamResponse.stop();
        }
        if (server != null){
            server.stop();
        }


        Toast.makeText(this, "Server destroyed", Toast.LENGTH_LONG);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
        super.onDestroy();
    }

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
