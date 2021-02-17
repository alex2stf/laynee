package com.arise.rapdroid.media.server.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arise.core.tools.SYSUtils;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.Icons;
import com.arise.rapdroid.media.server.MainActivity;
import com.arise.rapdroid.media.server.R;
import com.arise.weland.WelandClient;
import com.arise.rapdroid.media.server.appviews.SettingsView;
import com.arise.rapdroid.media.server.appviews.WallView;
import com.arise.weland.dto.DTOUtil;
import com.arise.weland.dto.Message;
import com.arise.weland.dto.RemoteConnection;
import com.arise.weland.utils.WelandServerHandler;
import com.arise.rapdroid.RAPDroidActivity;
import com.arise.rapdroid.components.ContextFragment;
import com.arise.rapdroid.components.ui.NavView;
import com.arise.rapdroid.media.server.views.ConversationView;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


@Deprecated
public class ChatFragment extends ContextFragment implements ConversationView.SelectHandler, ConversationView.SendHandler
{


    Map<String, ConversationView> conversationViewMap = new HashMap<>();


    private MainActivity mainActivity;

    SettingsView settingsView;

    NavView root;

    int padding = 25;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (root == null){
            root = new NavView(getContext())
                    .setSelectedColor(Icons.tab3Foreground)
                    .setReleasedColor(Icons.tab3Background);
            root.setPadding(padding, padding, padding, 0);
            root.setBackgroundColor(Icons.tab3Background);

            root.addButton(R.drawable.ic_media_add, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AppUtil.showConnectOptions(getContext(), settingsView, new CompleteHandler<RemoteConnection>() {
                        @Override
                        public void onComplete(RemoteConnection data) {

                        }
                    });
                }
            });
            ConversationView wallView = new WallView(getContext(), settingsView)
                    .setConversationId(DTOUtil.WALL_RESERVED_ID)
                    //owner is always the receiver
                    .setOwnerId(DTOUtil.sanitize(SYSUtils.getDeviceId()));

            ImageButton img = new ImageButton(getContext());
            img.setImageResource(R.drawable.ic_image_chat);
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mainActivity.chooseFile(new RAPDroidActivity.FileChooseHandler() {
                        @Override
                        public void onFileSelected(File file) {
                            wallView.prepare(file);
                        }
                    });
                }
            });
            wallView.addExtra(img);

            conversationViewMap.put(DTOUtil.WALL_RESERVED_ID, wallView);


            root.addMenu(R.drawable.ic_all_out, R.drawable.ic_all_out, "Wall", wallView);

        }
        return root;
    }




    public void addConversation(RemoteConnection remoteConnection) {
//        if (root != null) {
//            ConversationView conversationView = new ConversationView(getContext(),
//                    R.drawable.ic_send,
//                    R.drawable.ic_block_black_24dp)
//                    .onSelect(this).onSend(this);
//            conversationView.setRemoteConnection(remoteConnection);
//            root.addMenu(R.drawable.ic_send, R.drawable.ic_send, "Conversation with" +
//                    remoteConnection.getDeviceStat().getDisplayName(), conversationView);
//
//            String conversationId = remoteConnection.getDeviceStat().getConversationId();
//            conversationViewMap.put(conversationId, conversationView);
//        }
    }

//    public ChatFragment setMainActivity(MainActivity mainActivity) {
//        this.mainActivity = mainActivity;
//        return this;
//    }

//    public ChatFragment setSettingsView(SettingsView settingsView) {
//        this.settingsView = settingsView;
//        return this;
//    }

    @Override
    public boolean onSend(String text, ConversationView conversationView) {
        WelandClient.sendTextMessage(
                conversationView.getRemoteConnection(),
                text,
                new CompleteHandler<WelandClient.MessageResponse>() {
                    @Override
                    public void onComplete(WelandClient.MessageResponse data) {
                        if (WelandServerHandler.MSG_RECEIVE_OK.equalsIgnoreCase(data.deviceStat.getServerStatus())){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    conversationView.addMessage(data.message);
                                }
                            });
                        }
                        else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    conversationView.enable();
                                }
                            });
                        }
                    }
                }, new CompleteHandler() {
                    @Override
                    public void onComplete(Object data) {
                        conversationView.enable();
                    }
                }
        );



        conversationView.clearInput();
        return true;
    }



    @Override
    public void onSelect(ConversationView conversationView) {
        if (mainActivity != null){
            mainActivity.chooseFile(new RAPDroidActivity.FileChooseHandler() {
                @Override
                public void onFileSelected(File file) {

                }
            });
        }
    }

    public void onMessageReceiver(Message msg) {
        conversationViewMap.get(msg.getConversationId()).addMessage(msg);
    }
}
