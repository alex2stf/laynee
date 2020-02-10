package com.arise.rapdroid.media.server.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Layout;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

import com.arise.core.tools.CollectionUtil;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.RAPDUtils;
import com.arise.rapdroid.media.server.Icons;
import com.arise.rapdroid.media.server.R;
import com.arise.weland.dto.ContentInfo;
import com.arise.rapdroid.components.ui.Layouts;
import com.arise.rapdroid.components.ui.adapters.ListViewAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://live.rockfm.ro:8443/rockfm.aacp
 */
public class MediaDisplayer extends LinearLayout implements View.OnClickListener {

    private final Context context;
    private final int defaultRes;
    ListViewAdapter adapterContainer;
    protected List<ContentInfo> mediaInfos = new ArrayList<>();
    protected GridView gridView;
    PopupMenu popupMenu;
    TextView title;
    LinearLayout top;
    LinearLayout topLeft;

    public MediaDisplayer(Context context, int defaultRes) {
        super(context);
        this.context = context;
        this.defaultRes = defaultRes;
        adapterContainer = new ListViewAdapter();
        setOrientation(VERTICAL);


        top = new LinearLayout(context);
        top.setOrientation(HORIZONTAL);

        topLeft = new LinearLayout(context);
        topLeft.setOrientation(HORIZONTAL);
        topLeft.setGravity(Gravity.CENTER|Gravity.RIGHT);

        SearchView searchView = new SearchView(context);

        top.addView(searchView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));
        top.addView(topLeft, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));




        addView(top, Layouts.matchParentWrapContent());




        gridView = new GridView(context);
        gridView.setAdapter(adapterContainer);
        gridView.setNumColumns(GridView.AUTO_FIT);
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        gridView.setGravity(Gravity.CENTER);
        gridView.setColumnWidth(420);
        gridView.setVerticalSpacing(20);
        gridView.setHorizontalSpacing(20);
        addView(gridView, Layouts.matchParentWrapContent());


    }





    Map<MenuItem, OnMenuClickListener> actions = new HashMap<>();


    public MenuItem addMenu(String text, OnMenuClickListener onClickListener){
        MenuItem menuItem = popupMenu.getMenu().add(text);
        actions.put(menuItem, onClickListener);
        return menuItem;
    }




    public MediaDisplayer setMediaInfos(List<ContentInfo> infos){
        this.mediaInfos = infos;
        return this;
    }

    public MediaDisplayer setTitle(String text, int textColor) {
        title = new TextView(getContext());
        title.setText(text);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTextColor(textColor);

//        title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        title.setPadding(0, 0, 20, 0);

        topLeft.addView(title, Layouts.wrapContentMatchParent()); //TODO optional
        return this;
    }

    public MediaDisplayer enableMenu(int iconResource){
        ImageButton menuBtn = new ImageButton(context);
        menuBtn.setImageResource(R.drawable.ic_menu_light);
        menuBtn.setBackgroundColor(Color.TRANSPARENT);

        popupMenu = new PopupMenu(context, menuBtn);
        popupMenu.getMenu().add("Size");

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (actions.containsKey(menuItem)){
                    actions.get(menuItem).onClick(menuItem);
                }
                return false;
            }
        });

        /**\
         * optional:
         */


        menuBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                RAPDUtils.hideKeyboard(v);
                popupMenu.show();
            }
        });
        topLeft.addView(menuBtn);
        return this;
    }


    public  interface OnMenuClickListener {
        void onClick(MenuItem menuItem);
    }





    protected void runOnUiThread(Runnable runnable) {
        if (context instanceof Activity){
            ((Activity) context).runOnUiThread(runnable);
        }
    }


    MediaIcon buildIcon(ContentInfo mediaInfo){
        MediaIcon mediaIcon = new MediaIcon(context, mediaInfo, defaultRes, 420);
        mediaIcon.setOnClickListener(this::onClick);
        postIconBuild(mediaIcon);
        return mediaIcon;
    }

    protected void postIconBuild(MediaIcon mediaIcon) {

    }



    AlertDialog buildDialog(MediaIcon icon){
        String titles[] = new String[options.size()];
        for (int i = 0; i < titles.length; i++){
            titles[i] = options.get(i).getTitle(icon.getMediaInfo());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(icon.getMediaInfo().getTitle());
        builder.setItems(titles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                options.get(i).onClick(icon.getMediaInfo());
            }
        });
        AlertDialog dialog = builder.create();
        return dialog;
    }


    public MediaDisplayer addBatch(List<ContentInfo> infos, CompleteHandler<Object> completeHandler){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!CollectionUtil.isEmpty(infos)){
                    for (ContentInfo info: infos){
                        MediaIcon mediaIcon = buildIcon(info);
                        adapterContainer.add(mediaIcon);
                    }
                    adapterContainer.notifyDataSetChanged();
                }

                completeHandler.onComplete(this);
            }
        });
        return this;
    }







    private List<Option> options = new ArrayList<>();

    public void addOption(Option option) {
        options.add(option);
    }

    @Override
    public void onClick(View view) {
        if (!(view instanceof MediaIcon)){
            return;
        }
        MediaIcon icon = (MediaIcon) view;
        AlertDialog dialog = buildDialog(icon);
        dialog.show();
        onMediaIconClick(icon);
    }

    protected void onMediaIconClick(MediaIcon icon) {

    }

    public interface Option {
        String getTitle(ContentInfo info);
        void onClick(ContentInfo info);
    }
}
