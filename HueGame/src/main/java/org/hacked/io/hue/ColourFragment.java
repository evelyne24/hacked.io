package org.hacked.io.hue;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static org.hacked.io.hue.Constants.*;

/**
 * Created by evelyne24 on 20/07/2013.
 */
public class ColourFragment extends Fragment {

    private static final int DURATION = 4000;
    private static final int TICK = 1000;
    
    public static ColourFragment getInstance(Bundle args) {
        ColourFragment fragment = new ColourFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private View colourView;

    private CountDownTimer countDownTimer = new CountDownTimer(DURATION, TICK) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            Intent scannerIntent = new Intent(MainActivity.ACTION_SCANNER_READY);
            scannerIntent.putExtra(EXTRA_TAG_ID, (String) null);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(scannerIntent);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.colour_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        colourView = view.findViewById(R.id.colour_square);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle args = getArguments();
        String hexColor = args.getString(EXTRA_SCANNED_COLOUR);
        colourView.setBackgroundColor(Color.parseColor(hexColor));
        countDownTimer.start();
    }
}
