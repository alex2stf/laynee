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
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

import com.arise.core.tools.CollectionUtil;
import com.arise.core.tools.ContentType;
import com.arise.core.tools.Mole;
import com.arise.core.tools.StringUtil;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.rapdroid.RAPDUtils;
import com.arise.rapdroid.media.server.AppUtil;
import com.arise.rapdroid.media.server.Icons;
import com.arise.rapdroid.media.server.R;
import com.arise.weland.dto.ContentInfo;
import com.arise.rapdroid.components.ui.Layouts;
import com.arise.rapdroid.components.ui.adapters.ListViewAdapter;
import com.arise.weland.dto.RemoteConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * https://live.rockfm.ro:8443/rockfm.aacp
 */
public abstract class MediaDisplayer extends LinearLayout {

    private final Context context;
    private final int defaultRes;
    ListViewAdapter adapterContainer;
    protected Set<MediaIcon> mediaIcons = new HashSet<>();
    protected GridView gridView;
    PopupMenu popupMenu;
    TextView title;
    LinearLayout top;
    LinearLayout topLeft;

    public Object getWorker(){
        return getRemoteConnection().getPayload();
    };


    public MediaDisplayer(Context context, int defaultRes) {
        super(context);
        this.context = context;
        this.defaultRes = defaultRes;
        createTopBar();
        createGridView();
    }

    private void createTopBar(){

        adapterContainer = new ListViewAdapter();
        setOrientation(VERTICAL);


        top = new LinearLayout(context);
        top.setOrientation(HORIZONTAL);

        topLeft = new LinearLayout(context);
        topLeft.setOrientation(HORIZONTAL);
        topLeft.setGravity(Gravity.CENTER|Gravity.RIGHT);

        SearchView searchView = new SearchView(context);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                search(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                backgroundPrepareSearch(s);
                return false;
            }
        });

        top.addView(searchView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));
        top.addView(topLeft, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));
        addView(top, Layouts.matchParentWrapContent());

    }

    private void createGridView(){
        gridView = new GridView(context);
        gridView.setAdapter(adapterContainer);
        gridView.setNumColumns(GridView.AUTO_FIT);
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        gridView.setGravity(Gravity.CENTER);
        gridView.setColumnWidth(420);
        gridView.setVerticalSpacing(20);
        gridView.setHorizontalSpacing(20);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
               showOptions(view);
            }
        });
        addView(gridView, Layouts.matchParentWrapContent());
    }

    private void showOptions(View view) {
        if (!(view instanceof MediaIcon)){
            log.info("view is not media icon: " + (view != null ? view.getClass() : "null"));
            return;
        }
        MediaIcon icon = (MediaIcon) view;
        AlertDialog dialog = buildDialog(icon);
        dialog.show();
        onMediaIconClick(icon);
    }


    //    String searchQuery;
    Map<String, List<String>> params = new HashMap<>();
    private void backgroundPrepareSearch(String query){
        StringUtil.decodeQuery(query, params);

        if (params.containsKey("group")){

        }
    }

    private volatile boolean searchInProgress = false;

    private void search(String s){

        if ("ALL".equals(s)){

        }

        searchInProgress = true;
        synchronized (mediaIcons){
            List<View> results = new ArrayList<>();
            for (MediaIcon icon: mediaIcons){
                if (icon.getMediaInfo().getPath().toLowerCase().indexOf(s) > -1){
                    results.add(icon);
                }
                else if ("ALL".equalsIgnoreCase(s)){
                    results.add(icon);
                }
            }
            adapterContainer.setViews(results);
            adapterContainer.notifyDataSetChanged();
            searchInProgress = false;

        }

        System.out.println(params);
        System.out.println(s);
        params.clear();
    }



    Map<MenuItem, OnMenuClickListener> actions = new HashMap<>();


    public MenuItem addMenu(String text, OnMenuClickListener onClickListener){
        MenuItem menuItem = popupMenu.getMenu().add(text);
        actions.put(menuItem, onClickListener);
        return menuItem;
    }






    public MediaDisplayer setTitle(String text, int textColor) {
       // text = text.length() > 7 ? text.substring(0, 7) : text;
        title = new TextView(getContext());
        title.setText(text);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTextColor(textColor);

//        title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        title.setPadding(0, 0, 20, 0);

        topLeft.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f)); //TODO optional
        return this;
    }

    public MediaDisplayer enableMenu(int iconResource){
        ImageButton menuBtn = new ImageButton(context);
        menuBtn.setImageResource(R.drawable.ic_menu_light);
        menuBtn.setBackgroundColor(Color.TRANSPARENT);

        popupMenu = new PopupMenu(context, menuBtn);


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
        menuBtn.setPadding(0, 0, 20, 0);
        topLeft.addView(menuBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));
        return this;
    }

    public abstract String getPlaylistId();

    public abstract RemoteConnection getRemoteConnection();


    public  interface OnMenuClickListener {
        void onClick(MenuItem menuItem);
    }





    protected void runOnUiThread(Runnable runnable) {
        if (context instanceof Activity){
            ((Activity) context).runOnUiThread(runnable);
        }
    }

    private static final Mole log = Mole.getInstance(MediaDisplayer.class);

    MediaIcon buildIcon(ContentInfo mediaInfo){
        int icon = defaultRes;
        ContentType contentType = ContentType.search(mediaInfo.getExt());
        if (contentType != null && contentType.getResId() > 0){
            icon = contentType.getResId();
        }

        MediaIcon mediaIcon = new MediaIcon(context, mediaInfo, icon, 420);
//        mediaIcon.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showOptions(view);
//            }
//        });
        postIconBuild(mediaIcon);
        return mediaIcon;
    }

    protected void postIconBuild(MediaIcon mediaIcon) {

    }



    AlertDialog buildDialog(MediaIcon icon){
        final MediaDisplayer self = this;
        String titles[] = new String[options.size()];
        for (int i = 0; i < titles.length; i++){
            titles[i] = options.get(i).getTitle(icon.getMediaInfo());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(icon.getMediaInfo().getTitle());
        builder.setItems(titles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                options.get(i).onClick(icon.getMediaInfo(), self);
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

                        synchronized (mediaIcons){
                            if (!mediaIcons.contains(mediaIcon)){
                                mediaIcons.add(mediaIcon);
                            }
                        }

                        if (!searchInProgress){
                            adapterContainer.add(mediaIcon);
                        }
                    }
                    if (!searchInProgress){
                        adapterContainer.notifyDataSetChanged();
                    }
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



    protected void onMediaIconClick(MediaIcon icon) {

    }

    public interface Option {
        String getTitle(ContentInfo info);
        void onClick(ContentInfo info, MediaDisplayer displayer);
    }
}
