package com.arise.rapdroid.media.server.appviews;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.arise.core.tools.Mole;
import com.arise.core.tools.StringUtil;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.components.ui.Layouts;
import com.arise.rapdroid.media.server.R;
import com.arise.weland.WelandClient;
import com.arise.rapdroid.media.server.views.ConversationView;
import com.arise.weland.dto.Message;
import com.arise.weland.dto.RemoteConnection;
import com.arise.weland.utils.WelandServerHandler;

import java.util.UUID;

public class WallView extends ConversationView {
    private final SettingsView settingsView;

    static final Mole log = Mole.getInstance(WallView.class);

    public WallView(Context context, SettingsView settingsView) {
        super(context, R.drawable.ic_send, com.arise.rapdroid.media.server.R.drawable.ic_all_out);
        this.settingsView = settingsView;


        onSend(new SendHandler() {
            @Override
            public boolean onSend(String text, ConversationView conversationView) {
                if (!StringUtil.hasText(text)){
                    enable();
                }
                else {
                    if (!settingsView.hasBluetoothConnections()){
                        showConfirmScanDialog(new CompleteHandler() {
                            @Override
                            public void onComplete(Object data) {
                                distribute(text);
                            }
                        });
                    }
                    else {
                        distribute(text);
                    }
                }
                return false;
            }
        });

        ImageButton imageButton = new ImageButton(context);
        imageButton.setBackgroundColor(Color.TRANSPARENT);
        imageButton.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_send_states));

        this.setSubmitButton(imageButton);

    }



    private void showConfirmScanDialog(CompleteHandler completeHandler) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Scan bluetooth?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                completeHandler.onComplete(true);
                settingsView.scanBluetooth();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                completeHandler.onComplete(true);
            }
        });
        builder.create().show();
    }

    CompleteHandler<WelandClient.MessageResponse> onSuccess =new CompleteHandler<WelandClient.MessageResponse>() {
        @Override
        public void onComplete(WelandClient.MessageResponse data) {
            log.info("received " + data);
            enable();
        }
    };

    CompleteHandler onError = new CompleteHandler() {
        @Override
        public void onComplete(Object data) {
            enable();
        }
    };

    private void distribute(String text) {
        Message message = new Message()
                .setText(text)
                .setId(UUID.randomUUID().toString())
                .setSenderId(WelandServerHandler.deviceStat.getDisplayName())
                .wallMessage()
                ;

        for (RemoteConnection connection: settingsView.getConnections()){
            WelandClient.sendMessage(connection, message, onSuccess, onError);
        }
        enable();

    }

    @Override
    protected View buildMessageView(Message message) {
        LinearLayout.LayoutParams viewParams = Layouts.Linear.matchParentWrapContentWithMargin(20);
        TextView textView = new TextView(getContext());
        textView.setText(message.getText());
        textView.setTextColor(Color.BLACK);
        viewParams.gravity = Gravity.CENTER_HORIZONTAL;

        if (message.getSenderId().equals(getOwnerId())){
            textView.setBackgroundResource(com.arise.rapdroid.media.server.R.drawable.st_rounded_corners_sender);
        }
        else {

            textView.setBackgroundResource(com.arise.rapdroid.media.server.R.drawable.st_rounded_corners_receiver);
        }
        textView.setLayoutParams(viewParams);
        return textView;
    }
}
