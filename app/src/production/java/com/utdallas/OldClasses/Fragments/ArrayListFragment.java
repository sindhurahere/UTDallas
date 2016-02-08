package com.utdallas.OldClasses.Fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.utdallas.R;

import java.util.ArrayList;

/**
 * Created by sxk159231 on 1/25/2016.
 */
public class ArrayListFragment extends ListFragment {
    int mNum;

    /**
     * Create a new instance of CountingFragment, providing "num"
     * as an argument.
     */

   static ArrayList<String> strings;


    public static ArrayListFragment newInstance(int num) {
        ArrayListFragment f = new ArrayListFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("num", num);
        f.setArguments(args);
        strings = new ArrayList<>();
        strings.add("String 1");
        strings.add("String 2");
        strings.add("String 3");
        strings.add("String 4");
        return f;
    }

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNum = getArguments() != null ? getArguments().getInt("num") : 1;
    }

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_pager_list, null);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction().add(R.id.fragment, new VoiceFragment());
        ft.commit();
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, strings));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.i("FragmentList", "Item clicked: " + id);
    }
}
