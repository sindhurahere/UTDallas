package com.utdallas.OldClasses.Adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.utdallas.OldClasses.Fragments.VoiceFragment;

/**
 * Created by sxk159231 on 1/25/2016.
 */
public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    int no_items;
    Context context;

    public ViewPagerAdapter(FragmentManager fm, int num_items, Context context) {
        super(fm);
        no_items = num_items;
        this.context = context;
    }

    @Override
    public int getCount() {
        return no_items;
    }

    @Override

    public Fragment getItem(int position) {
        return new VoiceFragment(context);
    }
}
