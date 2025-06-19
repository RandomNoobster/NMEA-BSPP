package com.geomatikk.bspp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.content.Context;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.petr_s.nmea.GpsSatellite;
import com.github.petr_s.nmea.NMEAHandler;
import com.github.petr_s.nmea.NMEAParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  Initiates the bluetooth serial connection with the GNSS device.
 */
public class MainActivity extends Activity {

    // Context for android specific features
    private Context context;

    /**
     * Handler which defines what happens after parsing NMEA strings.
     */
    private final NMEAHandler nmeaHandler = new NMEAHandler() {
        @Override
        public void onStart() {

        }

        @Override
        public void onLocation(Location location) {
            Log.d(TAG, "Latitude: " + location.getLatitude() +  "Longitude: " + location.getLongitude());
            // Intended case for sending to Unity as plugin.
            // e.g
            // UnitySendMessage("GpsReceiver", "OnNmeaData", receivedData);
        }

        @Override
        public void onSatellites(List<GpsSatellite> satellites) {

        }

        @Override
        public void onUnrecognized(String sentence) {

        }

        @Override
        public void onBadChecksum(int expected, int actual) {

        }

        @Override
        public void onException(Exception e) {

        }

        @Override
        public void onFinish() {

        }
    };

    private final NMEAParser nmeaParser = new NMEAParser(nmeaHandler);

    // Handler responsible for receiving data and changes in state from the
    // bluetooth service.
    // Sends it to the message queue of the Looper which is what manages it.
    private final Handler bluetoothHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothSerialService.MESSAGE_STATE_CHANGE:
                    int newState = msg.arg1;
                    Log.d(TAG, "Bluetooth state changed: " + newState);
                    break;

                case BluetoothSerialService.MESSAGE_DEVICE_NAME:
                    String deviceName = msg.getData().getString(BluetoothSerialService.DEVICE_NAME);
                    Log.d(TAG, "Connected to " + deviceName);
                    break;

                case BluetoothSerialService.MESSAGE_READ:
                    String receivedData = (String) msg.obj;
                    Log.d(TAG, "GNSS String Data: " + receivedData.length());

                    String[] lines = receivedData.split("\\r?\\n");

                    for (String line : lines) {
                        if (!line.isBlank()) {
                            String sentence =  line.trim();
                            if (!sentence.startsWith("$PASHR")) {
                                Log.d(TAG, "Parsed sentence: " + sentence);
                                nmeaParser.parse(sentence);
                            }
                        }
                    }

                    break;

                case BluetoothSerialService.MESSAGE_READ_RAW:
                    byte[] raw = (byte[]) msg.obj;
                    Log.d(TAG, "Raw GNSS bytes: " + Arrays.toString(raw).length());
                    break;

                case BluetoothSerialService.MESSAGE_WRITE:
                    byte[] written = (byte[]) msg.obj;
                    Log.d(TAG, "Wrote to device: " + Arrays.toString(written));
                    break;

                case BluetoothSerialService.MESSAGE_TOAST:
                    String toast = msg.getData().getString(BluetoothSerialService.TOAST);
                    Log.d(TAG, "Toast: " + toast);
                    break;

                default:
                    Log.w(TAG, "Unhandled message code: " + msg.what);
            }
        }
    };

    //Debugging
    private static final String TAG = "GNSSBluetooth";
    private static final int REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (ensureBluetoothPermissions(this)) {
                Log.d(TAG, "Bluetooth permission granted.");

                BluetoothSerialService bluetoothSerialService = new BluetoothSerialService(bluetoothHandler);

                BluetoothDevice device = BluetoothAdapter.getDefaultAdapter()

                        .getRemoteDevice("BC:33:AC:D4:DA:18");
                bluetoothSerialService.connect(device, false);

            } else {
                Log.d(TAG, "Bluetooth permission NOT granted.");
                return;
            }
        } catch (Exception e){
            Log.d(TAG, "Failed cause of: " + e);
        }
    }



    /**
     * Ensures that the user has the appropriate permissions for initiating a bluetooth connection
     * @param activity  The current page/section of the application which holds the context for permissions.
     */
    public static boolean ensureBluetoothPermissions(Activity activity) {
        String[] allPermissions = new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.BLUETOOTH_CONNECT,
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


