package com.arise.rapdroid.media.server;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.arise.core.serializers.parser.Groot;
import com.arise.core.tools.AppCache;
import com.arise.core.tools.ContentType;
import com.arise.core.tools.Mole;
import com.arise.rapdroid.media.server.appviews.SettingsView;
import com.arise.rapdroid.media.server.fragments.BrowserFragment;
import com.arise.rapdroid.media.server.fragments.ChatFragment;
import com.arise.rapdroid.media.server.fragments.LogFragment;
import com.arise.rapdroid.media.server.fragments.MediaCenterFragment;
import com.arise.rapdroid.media.server.fragments.MediaPlaybackFragment;
import com.arise.rapdroid.media.server.fragments.SettingsFragment;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.Message;
import com.arise.weland.dto.RemoteConnection;
import com.arise.rapdroid.RAPDroidActivity;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.CHANGE_WIFI_MULTICAST_STATE;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.VIBRATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.arise.rapdroid.media.server.AppUtil.TAB_POSITION;

public class MainActivity extends RAPDroidActivity {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public SettingsFragment settingsFragment;
    MediaPlaybackFragment mediaPlaybackFragment;
    ChatFragment chatFragment;
    MediaCenterFragment mediaCenterFragment;
    SettingsView settingsView;
    LogFragment logFragment;
    BrowserFragment browserFragment;
    AppViewPager viewPager;
    int tabPosition = 0;
    Object[][] pages;
    int previousPosition = 0;
    private volatile boolean tabInit = false;

    BroadcastReceiver onStartServerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            settingsView.setServerInfo(isServerRunning(), intent.getStringExtra("http-connect"));
            settingsView.refreshUI();
        }
    };

    BroadcastReceiver onMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Message msg = Message.fromMap((Map<String, Object>) Groot.decodeBytes(message));
            chatFragment.onMessageReceiver(msg);
        }
    };

    BroadcastReceiver onOpenFileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String path = intent.getStringExtra("path");
            File file = new File(path);
            if (file.exists()){
                ContentInfo contentInfo = AppUtil.DECODER.decodeFile(file);
                //TODO check content type
                if (contentInfo != null){
                    mediaPlaybackFragment.play(contentInfo);
                    showVideoFragment();
                }
            }
            else if (isUrl(path)){
                browserFragment.loadUrl(path);
                showBrowserFragment();
            }

        }
    };









    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences worker = getSharedPreferences("prefs", MODE_PRIVATE);
        AppCache.setWorker(worker);


        tabPosition = AppCache.getInt(TAB_POSITION, 0);
        //start server

        startServerService();

        if (AppUtil.isAutoplayVideos()){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            //enter full screen
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }




        //apply a custom theme based on preferred tab
//        getTheme().applyStyle();
        this.hideTitle();

        ContentType.VIDEO_MP4.setResId(R.mipmap.icon_mp4);

        //logging adapter
        logFragment = new LogFragment();

        Mole.addAppender(logFragment);


        settingsView = new SettingsView(this)
//                .scanBluetooth()
        ;

        browserFragment = new BrowserFragment()
                .setSettingsView(settingsView);

        settingsFragment = new SettingsFragment().setSettingsView(settingsView);


        mediaPlaybackFragment = new MediaPlaybackFragment();
        chatFragment = new ChatFragment()
                .setSettingsView(settingsView)
                .setMainActivity(this);

        mediaCenterFragment = new MediaCenterFragment()
                        .setNeworkRefreshView(settingsView)
                        .setMainActivity(this);


        //binding
        mediaPlaybackFragment.setMediaCenter(mediaCenterFragment);

        viewPager = new AppViewPager(this){};
        viewPager.setId(viewPager.hashCode());



        pages = new Object[][]{
                {mediaCenterFragment, "", Icons.tab1Background, R.drawable.ic_tab_playlists, R.drawable.ic_tab_playlists_disabled},
                {browserFragment, "", Icons.tab2Background, R.drawable.ic_tab_web, R.drawable.ic_tab_web_disabled },
                {chatFragment, "", Icons.tab3Background, R.drawable.ic_tab_chat, R.drawable.ic_tab_chat_disabled },
                {settingsFragment, "", Icons.tab4Background, R.drawable.ic_tab_settings, R.drawable.ic_tab_settings_disabled },
                {mediaPlaybackFragment, "", Color.BLACK, R.drawable.ic_tab_media, R.drawable.ic_tab_media_disabled },
                {logFragment, "!", Icons.tab7Background}
        };

        AppPageAdapter appPageAdapter;
        appPageAdapter = new AppPageAdapter(getSupportFragmentManager(), this);
        for (int i = 0; i < pages.length; i++){
            androidx.fragment.app.Fragment fragment = (androidx.fragment.app.Fragment) pages[i][0];
            String title = (String) pages[i][1];
            appPageAdapter.add(fragment, title);
        }
        viewPager.setOffscreenPageLimit(appPageAdapter.getCount());
        viewPager.setAdapter(appPageAdapter);



        this.registerReceiver(onStartServerReceiver, new IntentFilter("onStart"));
        this.registerReceiver(onMessageReceiver, new IntentFilter("onMessage"));
        this.registerReceiver(onOpenFileReceiver, new IntentFilter("openFile"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        TabLayout tabLayout = new TabLayout(this);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.setTabTextColors(Color.GRAY, Color.GRAY);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                System.out.println(" PAGE SELECTED: " + position);
                tryUpdateTabColors(tabLayout, position, pages);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        tabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                System.out.println(tab);
                tryUpdateTabColors(tabLayout, tab.getPosition(), pages);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                previousPosition = tab.getPosition();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                System.out.println("tab onTabReselected" + tab);
                tryUpdateTabColors(tabLayout, tab.getPosition(), pages);
            }
        });

        tryUpdateTabColors(tabLayout, tabPosition, pages);


        root.addView(tabLayout, com.arise.rapdroid.components.ui.Layouts.matchParentWrapContent());
        root.addView(viewPager, com.arise.rapdroid.components.ui.Layouts.Linear.matchParentMatchParent());
        setContentView(root, com.arise.rapdroid.components.ui.Layouts.Linear.matchParentMatchParent());




        new SamsungDevice(this).discover();

        autoGoToTab(tabPosition);
    }




    private int restColors[][] = new int[][]{
            {Color.WHITE , Color.parseColor("#5e5796"), Color.parseColor("#817bb3"), Color.parseColor("#a5a1c9"), Color.parseColor("#c6c3de"), Color.parseColor("#e1dfed")}, //tab1
            {Color.parseColor("#8f8f8f"), Color.WHITE , Color.parseColor("#8f8f8f"), Color.parseColor("#adadad"), Color.parseColor("#c4c4c4"), Color.parseColor("#dbdbdb")}, //tab2
            {Color.parseColor("#65a6a3"), Color.parseColor("#2a7370"), Color.WHITE, Color.parseColor("#2a7370"), Color.parseColor("#65a6a3"), Color.parseColor("#abccca")}, //tab3
            {Color.parseColor("#7d729c"), Color.parseColor("#a295c4"), Color.parseColor("#cdc5e3"), Color.WHITE, Color.parseColor("#cdc5e3"), Color.parseColor("#a295c4")}, //tab4
            {Color.parseColor("#4d4d4d"), Color.parseColor("#3b3b3b"), Color.parseColor("#292929"), Color.parseColor("#1f1f1f"), Color.WHITE, Color.parseColor("#1f1f1f")}, //tab5
            {Color.parseColor("#c7b793"), Color.parseColor("#d4c6a7"), Color.parseColor("#e3d8bf"), Color.parseColor("#ede6d5"), Color.parseColor("#faf4e6"), Color.WHITE} //tab6
    };

    private void tryUpdateTabColors(TabLayout tabLayout, int position, Object[][] sources){
//        previousPosition = tabPosition;
        tabPosition = position;
        for (int i = 0; i < sources.length; i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (i == position){
                trySetTabColor(tab, (int) sources[i][2]);
                if (sources[i].length > 3) {
                    tab.setIcon((int) sources[i][3]);
                }
            }
            else {
                if (sources[i].length > 4) {
                   try {
                       tab.setIcon((int) sources[i][4]);
                   }catch (Throwable t){
                       System.out.println("RESOURCE NOT FOUND EXCEPTION");
                       t.printStackTrace();
                   }
                }
                trySetTabColor(tab, restColors[position][i]);

            }
        }


    }


    private void trySetTabColor(TabLayout.Tab tab, int xx){
        try {
            Field field = tab.getClass().getField("view");
            field.setAccessible(true);
            View view = (View) field.get(tab);
            view.setBackgroundColor(xx);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void startServerService(){
        if (!isServerRunning()){
            Intent intent = new Intent(this, ServerService.class);
            ContextCompat.startForegroundService(this, intent);
        }
    }

    public boolean isServerRunning(){
        return isMyServiceRunning(ServerService.class);
    }







    @Override
    protected String[] permissions() {
        return new String[]{
                INTERNET,
                BLUETOOTH,
                BLUETOOTH_ADMIN,
                READ_EXTERNAL_STORAGE,
                CHANGE_WIFI_MULTICAST_STATE,
                CAMERA,
                ACCESS_WIFI_STATE,
                VIBRATE,
                WRITE_EXTERNAL_STORAGE,
                RECORD_AUDIO};
    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("Service status", "Running");
                return true;
            }
        }
        Log.i ("Service status", "Not running");
        return false;
    }

    @Override
    protected void onPause() {
        mediaPlaybackFragment.saveState();
        browserFragment.saveState();
        AppCache.putInt(TAB_POSITION, tabPosition);
        super.onPause();
    }

    @Override
    protected void onStop() {
        AppCache.putInt(TAB_POSITION, tabPosition);
        mediaPlaybackFragment.saveState();
        browserFragment.saveState();
        safeUnregisterReceiver(onStartServerReceiver);
        safeUnregisterReceiver(onMessageReceiver);
        safeUnregisterReceiver(onOpenFileReceiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        AppCache.putInt(TAB_POSITION, tabPosition);
        mediaPlaybackFragment.saveState();
        browserFragment.saveState();
        safeUnregisterReceiver(onStartServerReceiver);
        safeUnregisterReceiver(onMessageReceiver);
        safeUnregisterReceiver(onOpenFileReceiver);
        super.onDestroy();
    }

    private void safeUnregisterReceiver(BroadcastReceiver receiver){
        try {
            unregisterReceiver(receiver);
        }catch (Throwable t){

        }
    }




    public void addConversation(RemoteConnection remoteConnection) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (chatFragment != null){
                    chatFragment.addConversation(remoteConnection);
                }
            }
        });
    }


    public void showVideoFragment() {
        autoGoToTab(4);
    }

    boolean isUrl(String s){
        URL url;
        try {
            url = new URL(s);
        } catch (MalformedURLException e) {
            return false;
        }
        return url != null;
    }

    private void showBrowserFragment() {
        autoGoToTab(1);
    }


    public synchronized void autoGoToTab(int index){
        if (tabInit){
            return;
        }
        tabInit = true;
        previousPosition = tabPosition;

        viewPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                tabPosition = index;
                AppCache.putInt(TAB_POSITION, tabPosition);
                viewPager.setCurrentItem(index, true);
                tabInit = false;
            }
        }, 100);
    }


    @Override
    public void onBackPressed() {
        if (tabInit){
            return;
        }
        System.out.println("BACK PRESSED");
        switch (tabPosition){
            case 1: //browser fragment
                if (browserFragment != null){
                    browserFragment.goBack();
                }
                return;
        }

        autoGoToTab(previousPosition);
    }

    public void lockViewPager() {
        if (viewPager != null){
            viewPager.setTouchEnabled(false);
        }
    }

    public boolean isViewPagerLocked() {
        if (viewPager != null){
            return !viewPager.isTouchEnabled();
        }
        return true;
    }

    public void unlockViewPager() {
        if (viewPager != null){
            viewPager.setTouchEnabled(true);
        }
    }
}
