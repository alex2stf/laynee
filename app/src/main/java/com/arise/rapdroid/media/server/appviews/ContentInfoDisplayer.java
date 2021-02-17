package com.arise.rapdroid.media.server.appviews;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.widget.AbsListView;
import android.widget.TextView;

import com.arise.core.tools.CollectionUtil;
import com.arise.core.tools.Mole;
import com.arise.core.tools.ThreadUtil;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.Icons;
import com.arise.weland.WelandClient;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.ContentPage;
import com.arise.rapdroid.media.server.views.MediaDisplayer;
import com.arise.rapdroid.media.server.views.MediaIcon;
import com.arise.weland.dto.RemoteConnection;

import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


//@Deprecated
public class ContentInfoDisplayer extends MediaDisplayer {
    private static final Mole log = Mole.getInstance(ContentInfoDisplayer.class);
    private static final int MIN_INIT_DATA_SIZE = 20;
    private static final int RETRY_TIMEOUT = 1000;
    private static final int MAX_RETRIES = 105 * RETRY_TIMEOUT;
    final Object worker;
    private final String playlistId;
    private final RemoteConnection remoteConnection;
    BlockingQueue<MediaIcon> icons = new LinkedBlockingQueue<>();
    volatile boolean checking = false;
    private Integer currentIndex = 0;
    private volatile boolean isFetching;



    private volatile int dataLoadedLength = 0;
    private volatile int connectRetries = 1000 * 5;



    public ContentInfoDisplayer(Context context, int defaultRes, RemoteConnection remoteConnection, String playlistId, String title) {
        super(context, defaultRes);
        this.remoteConnection = remoteConnection;
        this.worker = remoteConnection.getPayload();
        this.playlistId = playlistId;

        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                if (!isFetching && currentIndex != null){
                    fetchData();
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {

            }
        });

        fetchData();

        setTitle(title, Icons.tab1Background);




        addOption(new Option() {
            @Override
            public String getTitle(ContentInfo info) {
                return "Details";
            }

            @Override
            public void onClick(ContentInfo info, MediaDisplayer displayer) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(info.getTitle());
                TextView textView = new TextView(getContext());
                textView.setText(info.getPath() + "\n" + info.getThumbnailId());
                builder.setView(textView);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                 builder.create().show();
            }
        });


    }


    @Override
    public String getPlaylistId() {
        return playlistId;
    }

    @Override
    public RemoteConnection getRemoteConnection() {
        return remoteConnection;
    }

    public void retryFetch(){
        if (!isFetching && currentIndex != null){
            log.info("CLICK FETCH " + playlistId);
            fetchData();
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
//                log.info("CURRENT_INDEX = " + currentIndex + " from "
//                        + WelandClient.getWorkerId(worker) + playlistId + " data loaded: " + dataLoadedLength);
                connectRetries = (connectRetries - RETRY_TIMEOUT > RETRY_TIMEOUT ? connectRetries - RETRY_TIMEOUT : RETRY_TIMEOUT);
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
                    log.info("VALID BUT EMPTY LIST RESPONSE for " +playlistId +", retry in " + connectRetries + " millisecond");
                    try {
                        Thread.sleep(connectRetries);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    connectRetries+= RETRY_TIMEOUT;
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
                else if (data instanceof ConnectException && dataLoadedLength < MIN_INIT_DATA_SIZE && connectRetries < MAX_RETRIES){
                    try {
                        Thread.sleep(connectRetries);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    connectRetries+= RETRY_TIMEOUT;
                    fetchData();
                }
            }
        });
    }

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
