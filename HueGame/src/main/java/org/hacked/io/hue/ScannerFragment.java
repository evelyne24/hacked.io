package org.hacked.io.hue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;

import static org.hacked.io.hue.Constants.*;

/**
 * Created by evelyne24 on 20/07/2013.
 */
public class ScannerFragment extends BaseFragment {

    private static final String SEND_TAG_URL = "/tag";

    private TextView scannerTextView;

    public static ScannerFragment getInstance(Bundle args) {
        ScannerFragment fragment = new ScannerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.scanner_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scannerTextView = (TextView) view.findViewById(R.id.scan_label);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String tagId = getArguments().getString(EXTRA_TAG_ID);
        if (!TextUtils.isEmpty(tagId)) {
            scannerTextView.setText(getString(R.string.your_tag_id_is, tagId));
            doSendTagId(tagId);
        } else {
            scannerTextView.setText(R.string.scan_your_tag);
        }
    }

    private void doSendTagId(String tagId) {
        showLoadingFragment(true);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String serverUrl = sharedPreferences.getString(KEY_SERVER_URL, null) + SEND_TAG_URL;
        String deviceId = sharedPreferences.getString(KEY_DEVICE_ID, null);

        try {
            JSONObject jsonObject = buildPayload(tagId, deviceId);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, serverUrl, jsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(APP_TAG, "Response OK: " + response.toString());
                    onTagScannedSuccess(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(APP_TAG, "Response ERROR: " + error.getMessage());
                    onTagScanError();
                }
            });

            requestQueue.add(request);

        } catch (JSONException e) {
            Log.e(APP_TAG, "JSON malformed!", e);
        }

    }

    private JSONObject buildPayload(String tagId, String deviceId) throws JSONException{
        JSONObject json = new JSONObject();
        json.put(JSON_TAG_ID, tagId);
        json.put(JSON_DEVICE_ID, deviceId);
        return json;
    }

    private void onTagScannedSuccess(JSONObject response) {
        showLoadingFragment(false);

        try {
            String type = response.getString(JSON_TYPE);
            String data = response.getString(JSON_DATA);

            if(TYPE_NEW_USER.equals(type)) {
               onNewUserJoined(data);
            }
            else  if(TYPE_EXISTING_USER.equals(type)) {
                onScannerReady();
            }
            else if(TYPE_ERROR.equals(type)) {
                onScannerReady();
            }

        } catch (JSONException e) {
            Log.e(APP_TAG, "JSON exception!", e);
        }
    }

    private void onNewUserJoined(String data) {
        Intent scannedIntent = new Intent(MainActivity.ACTION_PLAYER_JOINED);
        scannedIntent.putExtra(EXTRA_SCANNED_COLOUR, data);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(scannedIntent);
    }

    private void onScannerReady() {
        Intent scannerReadyIntent = new Intent(MainActivity.ACTION_SCANNER_READY);
        scannerReadyIntent.putExtra(EXTRA_TAG_ID, (String) null);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(scannerReadyIntent);
    }

    private void onTagScanError() {
        showLoadingFragment(false);
        Toast.makeText(getActivity(), R.string.game_server_error, Toast.LENGTH_LONG).show();
    }
}
