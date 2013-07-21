package org.hacked.io.hue;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by evelyne24 on 20/07/2013.
 */
public class ScoreFragment extends Fragment {
    
    public static ScoreFragment getInstance(Bundle args) {
        ScoreFragment fragment = new ScoreFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.score_fragment, container, false);
    }
}
