package com.arise.rapdroid.media.server.appviews;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.arise.core.tools.AppCache;
import com.arise.core.tools.FileUtil;
import com.arise.core.tools.Mole;
import com.arise.core.tools.NetworkUtil;
import com.arise.core.tools.StringUtil;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.RAPDroidActivity;
import com.arise.rapdroid.components.ui.views.SmartLayout;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.MainActivity;
import com.arise.rapdroid.media.server.ServerService;
import com.arise.rapdroid.media.server.SpeechConverter;
import com.arise.rapdroid.media.server.fragments.MediaPlaybackFragment;
import com.arise.weland.WelandClient;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.DeviceStat;
import com.arise.weland.dto.RemoteConnection;
import com.arise.weland.utils.AppSettings;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.arise.core.tools.NetworkUtil.findFriendsForIp;
import static com.arise.core.tools.NetworkUtil.scanIPV4;
import static com.arise.rapdroid.media.server.AppUtil.FORCE_LANDSCAPE;



public class SettingsView extends ScrollView {
    private static final Mole log = Mole.getInstance(SettingsView.class);
    private final MainActivity activity;
    LinearLayout blueComps;
    LinearLayout netComps;
    Map<String, RemoteConnection> bluetoothConnections = new ConcurrentHashMap<>();
    Map<URI, RemoteConnection> httpConnections = new ConcurrentHashMap<>();
    SmartLayout smartLayout;
    //TODO deny from setting as remote name
    private HashSet<String> cache;
    private volatile boolean isScanning = false;

    private boolean speechDetectorAlways = false;

    private MediaPlaybackFragment mediaPlaybackFragment;

    public SettingsView(MainActivity activity) {
        super(activity);


        this.activity = activity;
        readCache();


        smartLayout = new SmartLayout(activity);
        smartLayout.addTextView("Remote connections:", View.TEXT_ALIGNMENT_TEXT_START);
        smartLayout.setPadding(10, 10, 10, 10);


        blueComps = smartLayout.addLinearLayout();

        smartLayout.addButton("Scan network", new SmartLayout.ButtonClickListener() {
            @Override
            public void onClick(Button view, SmartLayout self) {
                scanNet();
            }
        });

        smartLayout.addButton("Scan bluetooth", new SmartLayout.ButtonClickListener() {
            @Override
            public void onClick(Button view, SmartLayout self) {
                scanBluetooth();
            }
        });


        smartLayout.addButton("Force shutdown", new SmartLayout.ButtonClickListener() {
            @Override
            public void onClick(Button view, SmartLayout self) {
                activity.stopService(new Intent(activity, ServerService.class));
                activity.finish();
            }
        });


        speechConverter = new SpeechConverter(activity);
        Button[] speechButton = new Button[]{null};

        speechConverter.setDataFoundListener(new CompleteHandler<ArrayList<String>>() {
            @Override
            public void onComplete(ArrayList<String> data) {
                    log.info("Speech found " + StringUtil.join(data, "\n"));
                     speechConverter.stopListen();
                     speechButton[0].setText("start listen");
                ContentInfo contentInfo = AppUtil.contentInfoProvider.searchByKeyWord(data);

                if (contentInfo != null && mediaPlaybackFragment != null){
                    mediaPlaybackFragment.play(contentInfo);
                }
            }
        });
        speechButton[0] = smartLayout.addButton("start listen", new SmartLayout.ButtonClickListener() {
            @Override
            public void onClick(Button view, SmartLayout self) {
                if (speechConverter.isListening()){
                    speechConverter.stopListen();
                    speechButton[0].setText("start listen");
                }
                else {
                    speechConverter.startListen();
                    speechButton[0].setText("end listen");
                }
            }
        });


//        smartLayout.addCheckBox("")


        CheckBox checkBox = smartLayout.addCheckBox(getLandscapeText());
        checkBox.setChecked(AppCache.getBoolean(FORCE_LANDSCAPE));

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                AppCache.putBoolean(FORCE_LANDSCAPE, b);
                checkBox.setText(getLandscapeText());
            }
        });


        final CheckBox[] autoplayVidBox = new CheckBox[]{smartLayout.addCheckBox("Autoplay videos")};
        final CheckBox[] autoplayMusic = new CheckBox[]{smartLayout.addCheckBox("Autoplay music")};
        autoplayVidBox[0].setChecked(AppSettings.isAutoplayVideos());
        autoplayVidBox[0].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b && AppSettings.isAutoplayMusic()){
                    autoplayMusic[0].setChecked(false);
                    AppSettings.setMusicAutoplay(false);
                }
                AppSettings.setVideosAutoplay(b);
            }
        });

        autoplayMusic[0] = smartLayout.addCheckBox("Autoplay music");
        autoplayMusic[0].setChecked(AppSettings.isAutoplayMusic());
        autoplayMusic[0].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b && AppSettings.isAutoplayVideos()){
                    autoplayVidBox[0].setChecked(false);
                    AppSettings.setVideosAutoplay(false);
                }
                AppSettings.setMusicAutoplay(b);
            }
        });

        netComps = smartLayout.addLinearLayout();
        TextView textView = new TextView(activity);
        textView.setText("external ips:");
        netComps.addView(textView);





        addView(smartLayout);


        for (String key: cache){
            if (key.startsWith("BTH")){
                continue;
            }
            try {
                URI uri = new URI(key);
                if (AppUtil.workerIsLocalhost(uri)){
                    httpConnections.put(uri, new RemoteConnection(uri, DeviceStat.getInstance()));
                } else {
                    WelandClient.pingHttp(uri, new CompleteHandler<DeviceStat>() {
                        @Override
                        public void onComplete(DeviceStat data) {
                            httpConnections.put(uri, new RemoteConnection(uri, data));
                            saveCache();
                        }
                    }, new CompleteHandler<Throwable>() {
                        @Override
                        public void onComplete(Throwable data) {
                            ;;
                        }
                    });
                }

            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        NetworkUtil.scanIPV4(new NetworkUtil.IPIterator() {
            @Override
            public void onFound(String ip) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        smartLayout.addTextView(ip, View.TEXT_ALIGNMENT_CENTER);
                    }
                });
            }
        });
    }

    String getLandscapeText(){
        boolean landscape = AppCache.getBoolean(FORCE_LANDSCAPE);
        if (landscape){
            return "Disable force landscape";
        }
        return "Force landscape";
    }

    void saveCache(){
        for (URI uri: httpConnections.keySet()){
            cache.add(uri.toString());
        }
        FileUtil.serializableSave(cache, new File(AppUtil.DECODER.getAppDir(), "connx"));
    }

    void readCache (){
        cache = FileUtil.serializableRead(new File(AppUtil.DECODER.getAppDir(), "connx"));
        if (cache == null) {
            cache = new HashSet<>();
        }
        String localhost = "http://localhost:8221";
        if (!cache.contains(localhost)){
            cache.add(localhost);
        }
    }

    public SettingsView scanBluetooth(){
        if (isScanning){
            return this;
        }
        bluetoothConnections.clear();
        activity.getBluetoothBondedDevices(new RAPDroidActivity.BluetoothBondedHandler() {
            @Override
            public void onFound(BluetoothAdapter adapter, Set<BluetoothDevice> bondedDevices) {
                synchronized (adapter){
                    for (BluetoothDevice device: adapter.getBondedDevices()){
                        isScanning = true;
                        if (device != null){
                            WelandClient.pingBluetooth(device, new CompleteHandler<DeviceStat>() {
                                @Override
                                public void onComplete(DeviceStat data) {
                                    RemoteConnection remoteConnection = new RemoteConnection(device, data);
                                    addBluetooth(remoteConnection);
                                    isScanning = false;
                                    saveCache();
                                }
                            }, new CompleteHandler<Throwable>() {
                                @Override
                                public void onComplete(Throwable data) {
                                    log.info("Bluetooth [" + ("" + device.getName()).toUpperCase() + "] failed to connect");
                                    isScanning = false;
                                }
                            });
                        }
                    }
                    adapter.cancelDiscovery();
                }

            }
        });


        return this;
    }

    private void addBluetooth(RemoteConnection connection){
        bluetoothConnections.put(connection.getName(), connection);
    }

    public RemoteConnection getConnection(String name){

        for (RemoteConnection conn: httpConnections.values()){
            if (name.equals(conn.getDeviceStat().getDeviceName())
                || name.equals(conn.getDeviceStat().getDisplayName())
            ){
                return conn;
            }
        }

        //TODO improve this
        return bluetoothConnections.get(name);
    }



    public SettingsView scanNet() {

        scanIPV4(new NetworkUtil.IPIterator() {
            @Override
            public void onComplete(String[] ips) {
                for (String ip: ips){

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView textView = new TextView(activity);
                            textView.setText(ip);
                            netComps.addView(textView);
                        }
                    });
                    for (String possibleIp: findFriendsForIp(ip, 0)){
                        WelandClient.pingHttp(possibleIp, 8221, new CompleteHandler<DeviceStat>() {
                            @Override
                            public void onComplete(DeviceStat data) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Button button = new Button(activity);
                                        button.setText(data.getDisplayName());
                                    }
                                });
                            }
                        }, new CompleteHandler<Throwable>() {
                            @Override
                            public void onComplete(Throwable data) {
                                log.info("Ping failed for " + possibleIp + "| cause " + (data != null ? data.getMessage() : " unknown "));
                            }
                        });
                    }
                }
            }
        });
        return this;
    }





    public Set<RemoteConnection> getConnections() {
        Set<RemoteConnection> remoteConnections = new HashSet<>();
        for (RemoteConnection conn: httpConnections.values()){
            remoteConnections.add(conn);
        }
        for (RemoteConnection conn: bluetoothConnections.values()){
            remoteConnections.add(conn);
        }
        return remoteConnections;
    }





    public void setServerInfo(boolean serviceRunning, String connectString) {

    }

    public String[] getConnectionNames() {
        Set<RemoteConnection> connections = getConnections();
        String [] names = new String[connections.size()];
        int i = 0;
        for (RemoteConnection conn: connections){
            names[i] = conn.getName();
            i++;
        }
        return names;
    }




    public RemoteConnection getHttpConnection(URI uri, DeviceStat data) {

        if (httpConnections.containsKey(uri) && httpConnections.get(uri) != null){
            return httpConnections.get(uri);
        }
        RemoteConnection remoteConnection = new RemoteConnection(uri, data);
        httpConnections.put(uri, remoteConnection);
        return remoteConnection;
    }

    public boolean hasBluetoothConnections() {
        return !bluetoothConnections.isEmpty();
    }


    public void register(RemoteConnection connection) {
        Object payload = connection.getPayload();
        if (payload instanceof URI){
            URI u = (URI) payload;
            httpConnections.put(u, connection);
            saveCache();
        }
    }


    SpeechConverter speechConverter;
    public void activityDestroyed() {
        speechConverter.destroy();
    }

    public SettingsView setMediaPlaybackFragment(MediaPlaybackFragment mediaPlaybackFragment) {
        this.mediaPlaybackFragment = mediaPlaybackFragment;
        return this;
    }
}
