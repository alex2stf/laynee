package com.arise.rapdroid.media.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
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
import com.arise.weland.dto.DeviceStat;
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

    private int cameraIndex = 0;
    private int lightMode = 0;



    public ServerService() {

        serverHandler = new WelandServerHandler().setContentProvider(AppUtil.contentInfoProvider);
        requestBuilder = new WelandRequestBuilder(new IDeviceController() {
            @Override
            public void digestBytes(byte[] x) {

            }
        });

        try {
            ContentType.loadDefinitions();
            log.info("Successfully loaded content-type definitions");
        } catch (Exception e) {
            log.error("Failed to load content-type definitions", e);
        }

        serverHandler.setLiveJpeg(jpegOfferResponse);
        serverHandler.setLiveMjpegStream(mjpegResponse);
        serverHandler.setLiveWav(wavRecorderResponse);


        serverHandler.beforeLiveWAV(new WelandServerHandler.Handler<HttpRequest>() {
            @Override
            public HttpResponse handle(HttpRequest request) {
                boolean shouldStop = "false".equalsIgnoreCase(
                        request.getQueryParamString("camEnabled", "false")
                );
                if (shouldStop){
                    wavRecorderResponse.stopRecording();
                    return null;
                }

                if (!wavRecorderResponse.isRecording()) {
                    wavRecorderResponse.startRecord();
                }
                return null;
            }
        });

        WelandServerHandler.Handler<HttpRequest> cameraStreamHandler = new WelandServerHandler.Handler<HttpRequest>() {
            @Override
            public HttpResponse handle(HttpRequest request) {
                int newCameraIndex = request.getQueryParamInt("camIndex", 0);
                int newLightMode = request.getQueryParamInt("lightMode", 0);
                boolean shouldStop = "false".equalsIgnoreCase(
                        request.getQueryParamString("camEnabled", "false")
                );
                if (shouldStop){
                    camStreamResponse.stop();
                    wavRecorderResponse.stopRecording();
                    return null;
                }

                if (newLightMode != lightMode){
                    camStreamResponse.stop();
                    lightMode = newLightMode;
                    cameraIndex = newCameraIndex;
                    camStreamResponse.setCameraIndex(cameraIndex);
                    camStreamResponse.setLightMode(lightMode);

                }
                else if (newCameraIndex != cameraIndex){
                    cameraIndex = newCameraIndex;
                    camStreamResponse.stop();
                    camStreamResponse.setCameraIndex(cameraIndex);
                }


                if (!camStreamResponse.isRecording()) {
                    camStreamResponse.startStream();
                }
                return null;
            }
        };

        serverHandler.beforeLiveJPEG(cameraStreamHandler);
        serverHandler.beforeLiveMJPEG(cameraStreamHandler);

        serverHandler.setContentHandler(new AndroidContentHandler(this));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            log("onStartCommand from system");
        } else {
            log("onStartCommand from app");
        }
        Util.registerContext(getApplicationContext());
        Util.registerContext(getApplication());
        return android.app.Service.START_STICKY;
    }

    private void startServer() {


        if (camStreamResponse != null) {
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





    public void notify(Context context) {

        NotificationOps notificationOps = new NotificationOps()
                .setSmallIcon(R.drawable.ic_logo_no_back)
                .setTitle("Laynee service runnig")
                .setText("started at " + new Date())
                .setChannelId(CHANNEL_ID)
                .setChannelDescription("Laynee channel for server support")
                .setId(NOTIFICATION_ID)
                .setFlags(Notification.FLAG_FOREGROUND_SERVICE)
                ;

        Notification notification = RAPDUtils.createNotification(context, notificationOps);


        startForeground(NOTIFICATION_ID, notification);

//        NotificationManager notificationManager = getSystemService(NotificationManager.class);



    }


    IntentFilter batteryFilter = null;
    BroadcastReceiver batteryReceiver = null;

    SensorManager sensorManager = null;
    SensorEventListener sensorEventListener = null;

    /**
     * this is called only once
     */
    @Override
    public void onCreate() {
        if (AppUtil.contentInfoProvider.noFilesScanned()) {
            AppUtil.contentInfoProvider.get();
        }

        try {
            if (batteryFilter == null){
                batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

                if (batteryReceiver == null){
                    batteryReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent i) {
                            int s = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                            int l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                            DeviceStat.getInstance().setBatteryLevel(l).setBatteryScale(s);
                        }
                    };
                }
                getApplicationContext().registerReceiver(batteryReceiver, batteryFilter);
            }

            if (sensorManager == null){
                sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);

            }

            if (sensorEventListener == null){
                sensorEventListener = new SensorReader();
            }
            SensorReader.readInit(sensorManager, sensorEventListener);


        } catch (Exception e){
            log.e(e);
        }

        startServer();
        log("ON CREATE");


        super.onCreate();
    }


    @Override
    public void onDestroy() {
        log("onDestroy");

        if (wavRecorderResponse != null) {
            wavRecorderResponse.stopRecording();
        }

        if (camStreamResponse != null) {
            camStreamResponse.stop();
        }
        if (server != null) {
            server.stop();
        }
        if (batteryFilter != null){
            try {
                getApplicationContext().unregisterReceiver(batteryReceiver);
            }catch (Exception e){

            }
        }

        if (sensorManager != null){
            sensorManager.unregisterListener(sensorEventListener);
        }


        Toast.makeText(this, "Server destroyed", Toast.LENGTH_LONG);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);

//        BackgroundPlayer.INSTANCE.stop();
        super.onDestroy();
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {

        super.onTaskRemoved(rootIntent);
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) getSystemService(ns);
        nMgr.cancelAll();
    }

    private void log(String text) {
        log.info(".........................................SRVLOG\n " + text + "\n\n\n\n");
    }

}
