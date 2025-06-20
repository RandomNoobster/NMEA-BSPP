package com.geomatikk.bspp.plugin;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.github.petr_s.nmea.NMEAHandler;
import com.github.petr_s.nmea.NMEAParser;
import com.geomatikk.bspp.plugin.BluetoothSerialService;
import android.util.Log;
import java.util.Arrays;

public class UnityLocationProvider {
    private static final String TAG = "UnityLocationProvider";
    private static final String TARGET_MAC = "BC:33:AC:D4:DA:18";
    private BluetoothSerialService bluetoothService;
    private NMEAParser nmeaParser;
    private double lastLatitude = 0.0;
    private double lastLongitude = 0.0;
    private boolean isStarted = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice targetDevice;

    public UnityLocationProvider(Context context) {
        nmeaParser = new NMEAParser(new NMEAHandler() {
            @Override
            public void onStart() {}
            @Override
            public void onLocation(Location location) {
                lastLatitude = location.getLatitude();
                lastLongitude = location.getLongitude();
                Log.d(TAG, "Parsed Location: lat=" + lastLatitude + ", lon=" + lastLongitude);
            }
            @Override
            public void onSatellites(java.util.List satellites) {}
            @Override
            public void onUnrecognized(String sentence) {
                Log.d(TAG, "Unrecognized NMEA sentence: " + sentence);
            }
            @Override
            public void onBadChecksum(int expected, int actual) {
                Log.w(TAG, "Bad NMEA checksum: expected=" + expected + ", actual=" + actual);
            }
            @Override
            public void onException(Exception e) {
                Log.e(TAG, "NMEA Exception", e);
            }
            @Override
            public void onFinish() {}
        });
        Log.d(TAG, "UnityLocationProvider initialized with target MAC: " + TARGET_MAC);
        Handler nmeaHandler = new Handler(Looper.getMainLooper(), msg -> {
            Log.d(TAG, "NMEA Handler received message: " + msg.what);
            Log.d(TAG, "Message object: " + msg.obj);
            Log.d(TAG, "Message data: " + Arrays.toString(msg.getData().keySet().toArray()));
            Log.d(TAG, "Message data size: " + msg.getData().size());
            Log.d(TAG, "Message data keys: " + msg.getData().keySet());
            
            if (msg.what == BluetoothSerialService.MESSAGE_READ && msg.obj instanceof String) {
                Log.d(TAG, "UnityLocationProvider Raw NMEA data received");
                String data = (String) msg.obj;
                Log.d(TAG, "Raw NMEA received: " + data);
                // Split by newlines and parse each sentence
                String[] sentences = data.split("\r?\n");
                for (String sentence : sentences) {
                    if (!sentence.trim().isEmpty()) {
                        nmeaParser.parse(sentence.trim());
                    }
                }
            }
            return true;
        });
        bluetoothService = new BluetoothSerialService(nmeaHandler);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        targetDevice = bluetoothAdapter.getRemoteDevice(TARGET_MAC);
    }

    public void start() {
        if (!isStarted && targetDevice != null) {
            bluetoothService.connect(targetDevice, true); // true = secure
            isStarted = true;
        }
    }

    public void stop() {
        if (isStarted) {
            bluetoothService.stop();
            isStarted = false;
        }
    }

    public double getLatitude() {
        return lastLatitude;
    }

    public double getLongitude() {
        return lastLongitude;
    }
}
