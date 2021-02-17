package com.arise.rapdroid.media.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.widget.EditText;
import android.widget.TextView;

import com.arise.core.tools.AppCache;
import com.arise.core.tools.FileUtil;
import com.arise.core.tools.Mole;
import com.arise.core.tools.StringUtil;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.RAPDUtils;
import com.arise.rapdroid.media.server.appviews.SettingsView;
import com.arise.weland.WelandClient;
import com.arise.weland.dto.ContentInfo;
import com.arise.weland.dto.DeviceStat;
import com.arise.weland.dto.RemoteConnection;
import com.arise.weland.impl.ContentInfoProvider;
import com.arise.rapdroid.AndroidContentDecoder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class AppUtil {



    public static ContentInfo saveCurrentInfo(ContentInfo contentInfo){
        contentInfoProvider.getDecoder().saveState(contentInfo);
        return contentInfo;
    }

    public static ContentInfo getSavedContentInfo(){
        return contentInfoProvider.getDecoder().getSavedState();
    }




    public static final String TAB_POSITION = "tab-position";
    public static final String PLAYBACK_STATE = "playback-state";


    public static final String FORCE_LANDSCAPE = "lnd2702";


    public static final AndroidContentDecoder DECODER = new AndroidContentDecoder();


    public static final ContentInfoProvider contentInfoProvider
            = new ContentInfoProvider(DECODER)
            .addRoot(FileUtil.findMusicDir())
            .addRoot(FileUtil.getUploadDir())
            .addRoot(FileUtil.findMoviesDir())
//            .get()
            ;



    public static void showConnectOptions(Context context, SettingsView settingsView, CompleteHandler<RemoteConnection> completeHandler) {

       String lastUri = AppCache.getString("currentHttpUrl", "http://localhost:8221");
       AlertDialog.Builder builder = new AlertDialog.Builder(context);
       builder.setTitle("Select connection");

       final EditText input = new EditText(context);
       input.setText(lastUri);

       builder.setPositiveButton("Manually Connect", new DialogInterface.OnClickListener() {
           @Override
           public void onClick(DialogInterface dialogInterface, int i) {
               RAPDUtils.hideKeyboard(settingsView);
               String text = input.getText().toString();

               try {
                   URI uri = new URI(text);

                   WelandClient.pingHttp(uri, new CompleteHandler<DeviceStat>() {
                       @Override
                       public void onComplete(DeviceStat data) {
                           RemoteConnection remoteConnection = settingsView.getHttpConnection(uri, data);
                           if (remoteConnection == null){
                               System.out.println("WTF??");
                           }

                           else {
                               completeHandler.onComplete(remoteConnection);
                               AppCache.putString("currentHttpUrl", uri.toString());
                               settingsView.register(remoteConnection);
                           }

                       }
                   }, new CompleteHandler<Throwable>() {
                       @Override
                       public void onComplete(Throwable data) {
                           showFailure(context, data, settingsView, completeHandler);
                       }
                   });
               } catch (Exception e) {
                  showFailure(context, e, settingsView, completeHandler);
               }
           }
       });


       final String[] names = settingsView.getConnectionNames();
       builder.setItems(names, new DialogInterface.OnClickListener() {
           @Override
           public void onClick(DialogInterface dialogInterface, int i) {
               completeHandler.onComplete(settingsView.getConnection(names[i]));
           }
       });


       builder.setNeutralButton("Scan bluetooth", new DialogInterface.OnClickListener() {
           @Override
           public void onClick(DialogInterface dialogInterface, int i) {
               settingsView.scanBluetooth();
           }
       });




       builder.setView(input);
       Dialog dialog = builder.create();
//       dialog.setOnShowListener(new DialogInterface.OnShowListener() {
//           @Override
//           public void onShow(DialogInterface dialogInterface) {
//               dialog.getActionBar().set
//           }
//       });
       dialog.show();



    }




    public static final Mole log = Mole.getInstance(AppUtil.class);

    public static void showFailure(Context context, Throwable error,
                                   SettingsView settingsView,
                                   CompleteHandler<RemoteConnection> completeHandler){
        log.error("Error", error);
        if (error == null || ! (context instanceof Activity)){
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Failed to connect");
        final TextView view = new TextView(context);


        view.setText("E" + error.getMessage());

        builder.setView(view);
        builder.setPositiveButton("Try again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showConnectOptions(context, settingsView, completeHandler);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//                completeHandler.onComplete(null);
                ;;
            }
        });
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.create().show();
                RAPDUtils.hideKeyboard(settingsView);
            }
        });
    }


    public static boolean workerIsLocalhost(Object worker){
        if (worker != null && worker instanceof URI){
            URI uri = (URI) worker;
            return "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equalsIgnoreCase(uri.getHost());
        }
        return false;
    }








}
