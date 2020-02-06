package com.arise.rapdroid.media.server.appviews;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.MenuItem;
import android.widget.AbsListView;

import com.arise.core.tools.CollectionUtil;
import com.arise.core.tools.Mole;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.WelandClient;
import com.arise.weland.dto.AutoplayMode;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.ContentPage;
import com.arise.rapdroid.media.server.views.MediaDisplayer;
import com.arise.rapdroid.media.server.views.MediaIcon;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ContentInfoDisplayer extends MediaDisplayer {
    private final Object worker;
    private final String playlistId;
    private static final Mole log = Mole.getInstance(ContentInfoDisplayer.class);
    private Integer currentIndex = 0;
    private volatile boolean isFetching;
    private volatile int dataLoadedLength = 0;
    private static final int MIN_INIT_DATA_SIZE = 20;

    private AutoplayMode autoplayMode = AutoplayMode.off;
    MenuItem autoplayMenuItem;

    public ContentInfoDisplayer(Context context, int defaultRes, Object worker, String id) {
        super(context, defaultRes);
        this.worker = worker;
        this.playlistId = id;

        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                if (!isFetching && currentIndex != null){
                    log.info("SCROLL FETCH " + playlistId);
                    fetchData();
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {

            }
        });

        fetchData();



        addOption(new Option() {
            @Override
            public String getTitle(ContentInfo info) {
                return "Play";
            }

            @Override
            public void onClick(ContentInfo info) {
                WelandClient.openFile(info, worker);
                if (!isFetching && currentIndex != null){
                    log.info("CLICK FETCH " + playlistId);
                    fetchData();
                }
            }
        });

        addMenu("Shuffle", new OnMenuClickListener() {
            @Override
            public void onClick(MenuItem menuItem) {
                WelandClient.shuffle(worker, playlistId, new CompleteHandler() {
                    @Override
                    public void onComplete(Object data) {
                        log.info("shuffle complete");
                    }
                });
            }
        });



        autoplayMenuItem = addMenu("Autoplay on", new OnMenuClickListener() {
            @Override
            public void onClick(MenuItem view) {
               AutoplayMode toSend;
               if (AutoplayMode.off.equals(autoplayMode)){
                   toSend = AutoplayMode.on;
               }
               else {
                   toSend = AutoplayMode.off;
               }
               WelandClient.autoplay(worker, playlistId, toSend, new CompleteHandler() {
                    @Override
                    public void onComplete(Object data) {
                        log.info("Autoplay response: " + data);
                        autoplayMode = toSend;
                        updateAutoplayText();
                    }
               });
            }
         });
         updateAutoplayText();
    }



    private void updateAutoplayText(){
        if (autoplayMenuItem != null){
           runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   String text = autoplayMode.equals(AutoplayMode.on) ? "Stop autoplay" : "Start autoplay";
                   autoplayMenuItem.setTitle(text);
               }
           });
        }
    }





    @Override
    protected void onMediaIconClick(MediaIcon icon) {
        if (!isFetching && currentIndex != null){
            log.info("CLICK FETCH");
            fetchData();
        }
    }




    private void fetchData(){
        isFetching = true;
        WelandClient.mediaList(worker, playlistId, currentIndex, new CompleteHandler<ContentPage>() {
            @Override
            public void onComplete(ContentPage data) {
                List<ContentInfo> batch = data.getData();
                currentIndex = data.getIndex();
                dataLoadedLength += batch.size();
                if (!data.getAutoplayMode().equals(autoplayMode)){
                    autoplayMode = data.getAutoplayMode();
                    updateAutoplayText();
                }
                log.info("CURRENT_INDEX = " + currentIndex + " from "
                        + WelandClient.getWorkerId(worker) + "/" + playlistId + " data loaded: " + dataLoadedLength +
                        " autoplay mode " + data.getAutoplayMode());
                if (!CollectionUtil.isEmpty(batch)){

                    addBatch(batch, new CompleteHandler<Object>() {
                        @Override
                        public void onComplete(Object data) {
                            if (currentIndex != null  && dataLoadedLength < MIN_INIT_DATA_SIZE){
                                log.info("CONTINUE LOAD AUTO " + playlistId);
                                fetchData();
                            }
                            else {
                                isFetching = false;
                            }
                        }
                    });
                }
                else if (currentIndex != null && dataLoadedLength < MIN_INIT_DATA_SIZE){
                    log.info("VALID BUT EMPTY LIST RESPONSE for " +playlistId +", retry in 1 millisecond");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    fetchData();
                }
                else {
                    isFetching = false;
                }

            }
        }, new CompleteHandler() {
            @Override
            public void onComplete(Object data) {
                log.error("Failed to fetch " + data);
                isFetching = false;
                if(dataLoadedLength < MIN_INIT_DATA_SIZE && "RETRY".equals(data)){
                    fetchData();
                }
            }
        });
    }


    BlockingQueue<MediaIcon> icons = new LinkedBlockingQueue<>();

    @Override
    protected void postIconBuild(MediaIcon mediaIcon) {
        super.postIconBuild(mediaIcon);
        ContentInfo info = mediaIcon.getMediaInfo();



        AppUtil.DECODER.getPreview(info, new CompleteHandler<Bitmap>() {
            @Override
            public void onComplete(Bitmap data) {
                if (data != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mediaIcon.setBack(data);
                        }
                    });
                } else {
                    if (!AppUtil.workerIsLocalhost(worker)){
                        synchronized (icons){
                            log.info("adding icon request for " + mediaIcon.getMediaInfo().getPath());
                            icons.add(mediaIcon);
                            checkIcons();
                        }
                    }
                }
            }
        });
    }

    volatile boolean checking = false;

    private synchronized void checkIcons() {

        synchronized (icons){
            if (checking){
                return;
            }
            if (icons.isEmpty()){
                checking = false;
                return;
            }
            MediaIcon icon = icons.remove();
            String binaryId = icon.getMediaInfo().getThumbnailId();

            WelandClient.findThumbnail(worker, binaryId, new CompleteHandler<byte[]>() {
                @Override
                public void onComplete(byte[] data) {
                    if (data != null){
                        Bitmap bitmap = AppUtil.DECODER.getMinifiedBitmap(binaryId, data);
                        if (bitmap != null){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    icon.setBack(bitmap);
                                    checking = false;
                                    checkIcons();
                                }
                            });
                        } else {
                            checking = false;
                            checkIcons();
                        }
                    }
                }
            });
        }
    }


}
