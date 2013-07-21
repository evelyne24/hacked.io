package org.hacked.io.hue;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import org.ndeftools.Message;
import org.ndeftools.util.activity.NfcReaderActivity;

import java.util.List;

import static org.hacked.io.hue.Constants.*;

public class MainActivity extends NfcReaderActivity {

    public static final String ACTION_DEVICE_CONNECTED = "org.hacked.io.hue.ACTION_DEVICE_CONNECTED";
    public static final String ACTION_SCANNER_READY = "org.hacked.io.hue.ACTION_SCANNER_READY";
    public static final String ACTION_PLAYER_JOINED = "org.hacked.io.hue.ACTION_PLAYER_JOINED";

    private SharedPreferences sharedPreferences;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                Bundle args = intent.getExtras();

                if (ACTION_DEVICE_CONNECTED.equals(action)) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(android.R.id.content, ScannerFragment.getInstance(args))
                            .commit();
                } else if (ACTION_PLAYER_JOINED.equals(action)) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(android.R.id.content, ColourFragment.getInstance(args))
                            .commit();
                } else if(ACTION_SCANNER_READY.equals(action)) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(android.R.id.content, ScannerFragment.getInstance(args))
                            .commit();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        showContentFragment(null);

        // Start detecting NDEF messages using foreground mode
        setDetecting(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DEVICE_CONNECTED);
        intentFilter.addAction(ACTION_PLAYER_JOINED);
        intentFilter.addAction(ACTION_SCANNER_READY);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onNfcStateEnabled() {
        Log.d(APP_TAG, "NFC State enabled.");
    }

    @Override
    protected void onNfcStateDisabled() {
        Log.d(APP_TAG, "NFC State disabled.");
        showDialogFragment(new NfcNotEnabledDialog());
    }

    @Override
    protected void onNfcStateChange(boolean enabled) {
        Log.d(APP_TAG, "NFC State changed: enabled = " + enabled);
    }

    @Override
    protected void onNfcFeatureNotFound() {
        Log.w(APP_TAG, "NFC Not Supported.");
        showDialogFragment(new NfcNotSupportedDialog());
    }

    @Override
    protected void readNdefMessage(Message message) {
        Log.i(APP_TAG, "Read NDEF message.");
    }

    @Override
    protected void readEmptyNdefMessage() {
        Log.i(APP_TAG, "Read empty NDEF message.");
    }

    @Override
    protected void readNonNdefMessage() {
        Log.i(APP_TAG, "Read non-NDEF message.");
        Intent intent = getIntent();
        if (intent != null) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                byte[] bytes = tag.getId();
                if (bytes != null) {
                    String tagId = Utils.bytesToHex(bytes);
                    Log.i(APP_TAG, "NFC Tag Id: " + tagId);
                    showContentFragment(tagId);
                }
            }
        }
    }

    private boolean isFirstTimeRun() {
        return TextUtils.isEmpty(sharedPreferences.getString(KEY_DEVICE_ID, null));
    }

    private void showContentFragment(String tagId) {
        Bundle args = new Bundle();
        args.putString(EXTRA_TAG_ID, tagId);
        Fragment fragment = isFirstTimeRun() ? ConnectFragment.getInstance(args) : ScannerFragment.getInstance(args);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }

    private void showDialogFragment(DialogFragment dialogFragment) {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment prevDialogFragment = (DialogFragment) fm.findFragmentByTag(TAG_DIALOG);
        if (prevDialogFragment != null) {
            prevDialogFragment.dismiss();
        }
        dialogFragment.show(fm, TAG_DIALOG);
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
