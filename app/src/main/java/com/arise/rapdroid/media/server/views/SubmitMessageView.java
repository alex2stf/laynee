package com.arise.rapdroid.media.server.views;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.arise.rapdroid.components.ui.Layouts;

public class SubmitMessageView extends LinearLayout {
    private final Context context;
    private OnClickListener submitClickListener;
    EditText editText;
    View submitBtn;

    public SubmitMessageView(Context context) {
        super(context);
        this.context = context;
        setOrientation(LinearLayout.HORIZONTAL);
    }

    public SubmitMessageView setSubmitButton(View submitBtn){
        removeAllViews();
        submitBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (submitClickListener != null){
                    submitClickListener.onClick(v);
                }
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
        editText = new EditText(context);
        addView(editText, Layouts.matchParentMatchParent02f());
        addView(submitBtn, Layouts.matchParentMatchParent08f());
        this.submitBtn = submitBtn;
        return this;
    }




    public void onSubmit(OnClickListener submitClickListener){
        this.submitClickListener = submitClickListener;
    }


    public String getText() {
        return editText.getText().toString();
    }

    public void clearInput() {
        if (editText != null) {
            editText.setText("");
        }
    }

    public void disable() {
        editText.setEnabled(false);
        submitBtn.setEnabled(false);
    }

    public void enable() {
        editText.setEnabled(true);
        submitBtn.setEnabled(true);
    }
}
