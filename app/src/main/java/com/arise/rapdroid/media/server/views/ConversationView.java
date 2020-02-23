package com.arise.rapdroid.media.server.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.arise.rapdroid.RAPDUtils;
import com.arise.rapdroid.media.server.R;
import com.arise.weland.dto.Message;
import com.arise.weland.dto.RemoteConnection;
import com.arise.rapdroid.components.ui.Layouts;

import java.io.File;

public class ConversationView extends LinearLayout {

    private final Context context;
    LinearLayout page;
    ScrollView scrollView;
    SubmitMessageView bottom;
    private SelectHandler selectHandler;
    private SendHandler sendHandler;
    private RemoteConnection remoteConnection;

    protected LinearLayout extra;
    private String conversationId;
    private String ownerId;

    public ConversationView setConversationId(String conversationId) {
        this.conversationId = conversationId;
        return this;
    }

    public ConversationView setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public ConversationView(Context context, int submitRes, int blockIconRes) {
        super(context);
        this.context = context;
        setGravity(Gravity.BOTTOM);
        setOrientation(VERTICAL);
    }

    public ConversationView onSelect(SelectHandler selectHandler) {
        this.selectHandler = selectHandler;
        return this;
    }

    public ConversationView onSend(SendHandler sendHandler) {
        this.sendHandler = sendHandler;
        return this;
    }


    private ConversationView init(){
        if (bottom == null){

            final ConversationView self = this;
            bottom = new SubmitMessageView(context);
            bottom.onSubmit(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    RAPDUtils.hideKeyboard(v);
                    if (sendHandler != null){
                        sendHandler.onSend(bottom.getText(), self);
                        bottom.disable();
                    }
                }
            });

            scrollView = new ScrollView(context);


            extra = new LinearLayout(context);
            extra.setOrientation(LinearLayout.HORIZONTAL);


            extra.setPadding(0, 0, 0, 0);




            page = new LinearLayout(context);
            page.setOrientation(VERTICAL);

            scrollView.addView(page);

            addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0.8f));
            addView(extra, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(bottom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        }

        return this;
    }

    public ConversationView setSubmitButton(View view){
        init();
        bottom.setSubmitButton(view);
        return this;
    }

    public ConversationView addExtra(View view){
        init();
        this.extra.addView(view);
        return this;
    }

    public RemoteConnection getRemoteConnection() {
        return remoteConnection;
    }

    public ConversationView setRemoteConnection(RemoteConnection remoteConnection) {
        this.remoteConnection = remoteConnection;
        return this;
    }



    public void addMessage(Message message) {
        if (context instanceof Activity){
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    page.addView(buildMessageView(message));
                    scrollView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    }, 1000);
                    enable();
                }
            });
        }
    }


    protected String getOwnerId(){
        return ownerId;
    }

    protected View buildMessageView(Message message){


        LinearLayout.LayoutParams viewParams = Layouts.Linear.wrapContentWrapContent();
        viewParams.setMargins(20, 20, 20, 20);
        TextView textView = new TextView(context);
        textView.setText(message.getText());

        textView.setTextColor(Color.BLACK);


        if (message.getSenderId().equals(getOwnerId())){
            viewParams.gravity = Gravity.LEFT;
            textView.setBackgroundResource(com.arise.rapdroid.media.server.R.drawable.st_rounded_corners_sender);
        }
        else {
            viewParams.gravity = Gravity.RIGHT;
            textView.setBackgroundResource(com.arise.rapdroid.media.server.R.drawable.st_rounded_corners_receiver);
        }
        textView.setLayoutParams(viewParams);
        return textView;
    }

    public void enable() {
        bottom.enable();
    }

    public void clearInput(){
        bottom.clearInput();
    }

    public void prepare(File file) {
        bottom.setText(file.toURI().toString());
        bottom.enable();
    }


    public interface SendHandler{
        boolean onSend(String text, ConversationView conversationView);
    }

    public interface SelectHandler {
        void onSelect(ConversationView conversationView);
    }


}
