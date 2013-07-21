package org.hacked.io.hue;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by evelyne24 on 20/07/2013.
 */
public class BaseFragment extends Fragment {

    private static final String LOADING = "LOADING";

    protected RequestQueue requestQueue;
    protected LocalBroadcastManager broadcastManager;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        requestQueue = Volley.newRequestQueue(getActivity());
        broadcastManager = LocalBroadcastManager.getInstance(getActivity());
    }

    protected void showLoadingFragment(boolean show) {
        final FragmentManager fragmentManager = getChildFragmentManager();
        if (show) {
            fragmentManager.beginTransaction().replace(R.id.fragment_container, new LoadingFragment(), LOADING).commit();
        } else {
            Fragment fragment = fragmentManager.findFragmentByTag(LOADING);
            if (fragment != null) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }
    }
}
