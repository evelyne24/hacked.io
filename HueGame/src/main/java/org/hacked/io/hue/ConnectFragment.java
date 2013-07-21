package org.hacked.io.hue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;

import java.text.MessageFormat;

import static org.hacked.io.hue.Constants.*;

/**
 * Created by evelyne24 on 20/07/2013.
 */
public class ConnectFragment extends BaseFragment implements View.OnClickListener, TextWatcher {

    private static final String TAG = "HueGame";
    private static final String CONNECT_URL = "/hello/{0}";
    private EditText serverEditText;
    private View connectButton;
    private String baseServerUrl;
    private String serverUrl;

    public static ConnectFragment getInstance(Bundle args) {
        ConnectFragment fragment = new ConnectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.connect_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        serverEditText = (EditText) view.findViewById(R.id.server_url);
        connectButton = view.findViewById(R.id.connect);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        serverEditText.addTextChangedListener(this);
        connectButton.setOnClickListener(this);
    }

    // TODO File a bug in Volley when the url is not valid it throws NPE!!!
    @Override
    public void onClick(View view) {
        final int viewId = view.getId();
        switch (viewId) {
            case R.id.connect:
                if (validateData()) {
                    doRegisterDevice(serverUrl);
                }
                break;

            default:
                break;
        }
    }

    private boolean validateData() {
        baseServerUrl = serverEditText.getText().toString();
        String deviceId = Utils.getDeviceId(getActivity());

        if (TextUtils.isEmpty(baseServerUrl)) {
            serverEditText.setError(getString(R.string.game_server_empty_url));
            return false;
        }

        serverUrl = baseServerUrl + MessageFormat.format(CONNECT_URL, deviceId);
        if (Uri.parse(serverUrl).getHost() == null) {
            serverEditText.setError(getString(R.string.game_server_invalid_url));
            return false;
        }

        if (TextUtils.isEmpty(deviceId)) {
            Log.e(TAG, "Device ID is empty!");
            return false;
        }

        return true;
    }

    private void doRegisterDevice(String serverUrl) {
        showLoadingFragment(true);
        connectButton.setEnabled(false);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, serverUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "Response OK: " + response.toString());
                onDeviceConnectSuccess();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Response ERROR: " + error.getMessage());
                onDeviceConnectError();
            }
        });

        requestQueue.add(request);
    }

    private void onDeviceConnectSuccess() {
        showLoadingFragment(false);
        connectButton.setEnabled(true);


        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_SERVER_URL, baseServerUrl).commit();
        sharedPreferences.edit().putString(KEY_DEVICE_ID, Utils.getDeviceId(getActivity())).commit();

        Intent connectedIntent = new Intent(MainActivity.ACTION_DEVICE_CONNECTED);
        connectedIntent.putExtras(getArguments());
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(connectedIntent);
    }

    private void onDeviceConnectError() {
        showLoadingFragment(false);
        connectButton.setEnabled(true);

        Toast.makeText(getActivity(), R.string.game_server_error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (serverEditText.getError() != null) {
            serverEditText.setError(null);
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {}
}
