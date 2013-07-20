package org.hacked.io.hue;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import org.ndeftools.Message;
import org.ndeftools.util.activity.NfcReaderActivity;

import java.text.MessageFormat;
import java.util.List;


public class MainActivity extends NfcReaderActivity implements View.OnClickListener {

    private static final String TAG = "HueGame";
    private static final String DIALOG = "DIALOG";
    private static final String CONNECT_URL = "/hello/{0}";
    private EditText serverUrlInput;
    private View connectButton;
    private RequestQueue requestQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this);

        serverUrlInput = (EditText) findViewById(R.id.server_url);
        connectButton = findViewById(R.id.connect);
        connectButton.setOnClickListener(this);

        // Start detecting NDEF messages using foreground mode
        setDetecting(true);
    }

    @Override
    protected void onNfcStateEnabled() {
        Log.d(TAG, "NFC State enabled.");
    }

    @Override
    protected void onNfcStateDisabled() {
        Log.d(TAG, "NFC State disabled.");
        showDialog(new NfcNotEnabledDialog());
    }

    @Override
    protected void onNfcStateChange(boolean enabled) {
        Log.d(TAG, "NFC State changed: enabled = " + enabled);
    }

    @Override
    protected void onNfcFeatureNotFound() {
        Log.w(TAG, "NFC Not Supported.");
        showDialog(new NfcNotSupportedDialog());
    }

    @Override
    protected void readNdefMessage(Message message) {
        Log.i(TAG, "Read NDEF message.");
    }

    @Override
    protected void readEmptyNdefMessage() {
        Log.i(TAG, "Read empty NDEF message.");
    }

    @Override
    protected void readNonNdefMessage() {
        Log.i(TAG, "Read non-NDEF message.");
        Intent intent = getIntent();
        if (intent != null) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                byte[] tagId = tag.getId();
                if (tagId != null) {
                    String tagStr = Utils.bytesToHex(tagId);
                    Log.i(TAG, "TAG ID: " + tagStr);
                }
            }
        }
    }

    // TODO File a bug in Volley when the url is not valid it throws NPE!!!
    @Override
    public void onClick(View view) {
        if (connectButton == view) {
            String serverUrl = serverUrlInput.getText().toString();
            String deviceId = Utils.getDeviceId(this);

            if (TextUtils.isEmpty(serverUrl)) {
                serverUrlInput.setError(getString(R.string.game_server_empty));
            } else if (TextUtils.isEmpty(deviceId)) {
                Log.e(TAG, "Device ID is empty!");
            } else {
                String url = serverUrl + MessageFormat.format(CONNECT_URL, deviceId);
                Log.d(TAG, "POST to " + url);
                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Response OK: " + response.toString());
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Response ERROR: " + error.getStackTrace());
                    }
                });
                requestQueue.add(request);
            }
        }
    }

    private void showDialog(DialogFragment dialogFragment) {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment prevDialogFragment = (DialogFragment) fm.findFragmentByTag(DIALOG);
        if (prevDialogFragment != null) {
            prevDialogFragment.dismiss();
        }
        dialogFragment.show(fm, DIALOG);
    }

    public static class NfcNotSupportedDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.nfc_not_supported_title).setMessage(R.string.nfc_not_supported_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            NfcNotSupportedDialog.this.dismiss();
                        }
                    });
            return builder.create();
        }
    }

    public static class NfcNotEnabledDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.nfc_not_enabled_title).setMessage(R.string.nfc_not_enabled_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            PackageManager packageManager = getActivity().getPackageManager();
                            if (packageManager != null) {
                                Intent settingsIntent = new Intent(Settings.Global.RADIO_NFC);
                                List<ResolveInfo> results = packageManager.queryIntentActivities(settingsIntent, PackageManager.MATCH_DEFAULT_ONLY);
                                if (results.isEmpty()) {
                                    settingsIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                                    results = packageManager.queryIntentActivities(settingsIntent, PackageManager.MATCH_DEFAULT_ONLY);
                                }
                                if (!results.isEmpty()) {
                                    NfcNotEnabledDialog.this.dismiss();
                                    startActivity(settingsIntent);
                                } else {
                                    Toast.makeText(getActivity(), R.string.wireless_settings_not_found, Toast.LENGTH_SHORT).show();
                                    NfcNotEnabledDialog.this.dismiss();
                                }
                            } else {
                                Toast.makeText(getActivity(), R.string.package_manager_not_found, Toast.LENGTH_SHORT).show();
                                NfcNotEnabledDialog.this.dismiss();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            NfcNotEnabledDialog.this.dismiss();
                        }
                    });
            return builder.create();
        }
    }
}
