package com.geomatikk.bspp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.content.Context;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "GNSSBluetooth";
    private Context context;
    private static final int REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Starting application.");

        try {
            if (ensureBluetoothPermissions(this)) {
                Log.d(TAG, "Bluetooth permission granted.");

                Handler handler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case BluetoothSerial.MESSAGE_STATE_CHANGE:
                                int newState = msg.arg1;
                                Log.d(TAG, "Bluetooth state changed: " + newState);
                                break;

                            case BluetoothSerial.MESSAGE_DEVICE_NAME:
                                String deviceName = msg.getData().getString(BluetoothSerial.DEVICE_NAME);
                                Log.d(TAG, "Connected to " + deviceName);
                                break;

                            case BluetoothSerial.MESSAGE_READ:
                                String receivedData = (String) msg.obj;
                                Log.d(TAG, "GNSS String Data: " + receivedData);
                                // UnitySendMessage("GpsReceiver", "OnNmeaData", receivedData);
                                break;

                            case BluetoothSerial.MESSAGE_READ_RAW:
                                byte[] raw = (byte[]) msg.obj;
                                Log.d(TAG, "Raw GNSS bytes: " + Arrays.toString(raw));
                                break;

                            case BluetoothSerial.MESSAGE_WRITE:
                                byte[] written = (byte[]) msg.obj;
                                Log.d(TAG, "Wrote to device: " + Arrays.toString(written));
                                break;

                            case BluetoothSerial.MESSAGE_TOAST:
                                String toast = msg.getData().getString(BluetoothSerial.TOAST);
                                Log.d(TAG, "Toast: " + toast);
                                break;

                            default:
                                Log.w(TAG, "Unhandled message code: " + msg.what);
                        }
                    }
                };
                BluetoothSerialService bluetoothSerialService = new BluetoothSerialService(handler);
                BluetoothDevice device = BluetoothAdapter.getDefaultAdapter()
                        .getRemoteDevice("BC:33:AC:D4:DA:18");
                bluetoothSerialService.connect(device, false); // or false for insecure
            } else {
                Log.d(TAG, "Bluetooth permission NOT granted.");
                return;
            }
        } catch (Exception e){
            Log.d(TAG, "Failed cause of: " + e);
        }
    }

    /**
     * Ensures that the user has the appropriate permissions
     * @param activity  "passport" to using the android services.
     */
    public static boolean ensureBluetoothPermissions(Activity activity) {
        String[] allPermissions = new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        List<String> missing = new ArrayList<>();

        for (String permission : allPermissions) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }

        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    missing.toArray(new String[0]),
                    REQUEST_CODE
            );
            return false; // Not ready yet — permissions are being requested
        }
        return true; //  All permissions already granted
    }
}


