package com.arise.rapdroid.media.server.appviews;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.arise.core.tools.AppCache;
import com.arise.core.tools.CollectionUtil;
import com.arise.core.tools.FileUtil;
import com.arise.core.tools.Mole;
import com.arise.core.tools.NetworkUtil;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.MainActivity;
import com.arise.rapdroid.media.server.ServerService;
import com.arise.rapdroid.media.server.WelandClient;
import com.arise.weland.dto.DeviceStat;
import com.arise.weland.dto.Playlist;
import com.arise.weland.dto.RemoteConnection;
import com.arise.rapdroid.RAPDroidActivity;
import com.arise.rapdroid.components.ui.views.SmartLayout;
import com.arise.weland.utils.WelandServerHandler;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.arise.core.tools.NetworkUtil.findFriendsForIp;
import static com.arise.core.tools.NetworkUtil.scanIPV4;

public class SettingsView extends ScrollView {
    private final MainActivity activity;
    private String connectString;
    TextView serverStatus;
    Button serverBtn;
    LinearLayout blueComps;
    LinearLayout netComps;
    private boolean serverRunning = false;
    private static final Mole log = Mole.getInstance(SettingsView.class);

    //TODO deny from setting as remote name
    private RemoteConnection localConnection;

//    Set<RemoteConnection> connections = new HashSet<>();
    private String currentHttpUrl =  AppCache.getString("currentHttpUrl", "http://localhost:8221/");

    Map<String, RemoteConnection> bluetoothConnections = new ConcurrentHashMap<>();
    Map<URI, RemoteConnection> httpConnections = new ConcurrentHashMap<>();




    HashSet<String> cachedBluetooths;

    void saveCache(){
        FileUtil.serializableSave(cachedBluetooths, new File(AppUtil.DECODER.getAppDir(), "bths"));
    }

    void readCache (){
        cachedBluetooths = FileUtil.serializableRead(new File(AppUtil.DECODER.getAppDir(), "bths"));
        if (cachedBluetooths == null) {
            cachedBluetooths = new HashSet<>();
        }
    }


    public SettingsView(MainActivity activity) {
        super(activity);

        try {
            URI uri = new URI("http://localhost:8221/");
            localConnection = new RemoteConnection(uri, DeviceStat.getInstance());
            httpConnections.put(uri, localConnection);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        this.activity = activity;
        readCache();




        SmartLayout smartLayout = new SmartLayout(activity);
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

        netComps = smartLayout.addLinearLayout();
        TextView textView = new TextView(activity);
        textView.setText("external ips:");
        netComps.addView(textView);


        serverStatus = smartLayout.addTextView("Server running: " + serverRunning, View.TEXT_ALIGNMENT_CENTER);
        serverBtn = smartLayout.addButton("", new SmartLayout.ButtonClickListener() {
            @Override
            public void onClick(Button view, SmartLayout self) {
                if (!serverRunning){
                    activity.startServerService();
                }
            }
        });
        refreshUI();


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
        addView(smartLayout);
    }


    private volatile boolean isScanning = false;

    public SettingsView scanBluetooth(){
        if (isScanning){
            return this;
        }
        bluetoothConnections.clear();
        cachedBluetooths.clear();
        activity.getBluetoothBondedDevices(new RAPDroidActivity.BluetoothBondedHandler() {
            @Override
            public void onFound(BluetoothAdapter adapter, Set<BluetoothDevice> bondedDevices) {
//                if (!adapter.isEnabled()){
//                    adapter.enable();
//                }
//                if (!adapter.isDiscovering()){
//                    adapter.startDiscovery();
//                }
                synchronized (adapter){
                    for (BluetoothDevice device: adapter.getBondedDevices()){
                        isScanning = true;
                        if (device != null){
                            WelandClient.pingBluetooth(device, new CompleteHandler<DeviceStat>() {
                                @Override
                                public void onComplete(DeviceStat data) {
                                    RemoteConnection remoteConnection = new RemoteConnection(device, data);
                                    addBluetooth(remoteConnection);
                                    cachedBluetooths.add(device.getName());
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
//                    adapter.disable();
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
//                                        netComps.addView(textView);
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


    public void refreshUI(){
        if (serverStatus != null && serverBtn != null){
            if (!serverRunning){
                serverStatus.setText("Localhost closed");
                serverBtn.setText("Start server");
                serverBtn.setVisibility(View.VISIBLE);
                serverBtn.setEnabled(true);
            }
            else {
                serverStatus.setText("Server running at [" + connectString + "]");
                serverBtn.setVisibility(View.INVISIBLE);
            }
        }
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
        this.serverRunning = serviceRunning;
        this.connectString = connectString;
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

    public boolean hasConnections() {
        return !CollectionUtil.isEmpty(bluetoothConnections) || !CollectionUtil.isEmpty(httpConnections);
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
}
