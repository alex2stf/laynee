package com.arise.rapdroid.media.server.appviews;

import android.content.Context;
import android.graphics.Color;
import android.widget.ImageButton;

import com.arise.core.tools.StringUtil;
import com.arise.rapdroid.media.server.R;
import com.arise.rapdroid.media.server.views.ConversationView;

public class WallView extends ConversationView {
    public WallView(Context context) {
        super(context, R.drawable.ic_send, R.drawable.ic_all_out);

        onSend(new SendHandler() {
            @Override
            public boolean onSend(String text, ConversationView conversationView) {
                if (!StringUtil.hasText(text)){
                    enable();
                    return false;
                }
                return false;
            }
        });

        ImageButton imageButton = new ImageButton(context);
        imageButton.setBackgroundColor(Color.TRANSPARENT);
        imageButton.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_send_states));

        this.setSubmitButton(imageButton);
    }
}
