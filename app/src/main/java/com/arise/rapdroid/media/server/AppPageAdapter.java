package com.arise.rapdroid.media.server;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class AppPageAdapter extends FragmentPagerAdapter {

    final Context ctx;
    private final List<Fragment> mList = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();

    public AppPageAdapter(FragmentManager fm, Context ctx) {
        super(fm);
        this.ctx = ctx;
    }

    @Override
    public Fragment getItem(int position) {
        return mList.get(position);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    public void add(Fragment fragment, String title) {
        mList.add(fragment);
        titles.add(title);
        notifyDataSetChanged();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titles.get(position);
    }


}
