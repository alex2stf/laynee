package com.arise.rapdroid.media.server.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.arise.weland.dto.Message;
import com.arise.weland.dto.RemoteConnection;
import com.arise.rapdroid.components.ui.Layouts;

public class ConversationView extends LinearLayout {

    private final Context context;


    private SelectHandler selectHandler;
    private SendHandler sendHandler;
    private RemoteConnection remoteConnection;
    LinearLayout page;
    ScrollView scrollView;
    SubmitMessageView bottom;

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
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    if (sendHandler != null){
                        sendHandler.onSend(bottom.getText(), self);
                        bottom.disable();
                    }
                }
            });

            scrollView = new ScrollView(context);

            LinearLayout extra = new LinearLayout(context);
            extra.setOrientation(LinearLayout.HORIZONTAL);


            extra.setPadding(0, 0, 0, 0);
            extra.setBackgroundColor(Color.GREEN);




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


    public ConversationView(Context context, int submitRes, int blockIconRes) {
        super(context);
        this.context = context;
        setGravity(Gravity.BOTTOM);
        setOrientation(VERTICAL);
    }

    public ConversationView setRemoteConnection(RemoteConnection remoteConnection) {
        this.remoteConnection = remoteConnection;
        return this;
    }

    public RemoteConnection getRemoteConnection() {
        return remoteConnection;
    }

    public void addSenderMsg(Message message) {
        if (context instanceof Activity){
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = new TextView(context);
                    textView.setText(message.getText());
                    page.addView(textView);
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

    public void enable() {
        bottom.enable();
    }

    public interface SendHandler{
        boolean onSend(String text, ConversationView conversationView);
    }


    public interface SelectHandler {
        void onSelect(ConversationView conversationView);
    }

    public void clearInput(){
        bottom.clearInput();
    }


}
