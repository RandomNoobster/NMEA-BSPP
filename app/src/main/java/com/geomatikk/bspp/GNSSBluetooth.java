package com.geomatikk.bspp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class GNSSBluetooth {
    private static final String TAG = "GNSSBluetooth";
    private BluetoothSocket socket;
    private final BluetoothAdapter adapter;
    private final Context context;
    private final Activity activity;
    private static final int REQUEST_CODE = 1001;
    private volatile boolean keepReading = true;

    public GNSSBluetooth(Activity activity) {
        // Android lifecycle objects for permissions
        this.context = activity.getApplicationContext();
        this.activity = activity;

        adapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "Bluetooth adapter initialized");
    }

    public void connectTo(String macAddress) {
        Log.d(TAG, "connectTo() called with: " + macAddress);
        close(); // close any previous connection

        new Thread(() -> {
            try {
                BluetoothDevice device = adapter.getRemoteDevice(macAddress);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

                Thread.sleep(3000);

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[]{
                                    Manifest.permission.BLUETOOTH_ADMIN,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION}
                            , REQUEST_CODE);
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    Log.d(TAG, "Still have permissions.");
                } else {
                    Log.d(TAG, "Not permissions after all.");
                }
//                    socket = device.createRfcommSocketToServiceRecord(uuid);
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid);

                adapter.cancelDiscovery();
                Log.d(TAG, "Connecting...");

                // Device bonded check
                Log.d(TAG, "Device: " + device.getName() + ", bonded = " + (device.getBondState() == BluetoothDevice.BOND_BONDED));

                // UUIDs check
                ParcelUuid[] uuids = device.getUuids();
                if (uuids != null) {
                    for (ParcelUuid id : uuids) {
                        Log.d(TAG, "Device UUID: " + id.toString());
                    }
                } else {
                    Log.d(TAG, "No UUIDs available from device.");
                }
                Log.d(TAG, adapter.getBondedDevices().toString());
                Log.d(TAG, String.valueOf(adapter.getState()));

                Thread.sleep(2000);

                socket.connect();
                Log.d(TAG, "Socket connecting to SP60");

                boolean connection = socket.isConnected();
                Log.d(TAG, "Connection status: " + connection);

                // Start reading data
                readNMEA();

            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                close();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
    }

        private void readNMEA() {
            Log.d(TAG, "readNMEA() started");
            try {
                InputStream inputStream = socket.getInputStream();
                Log.d(TAG, "InputStream obtained from socket");

            } catch (IOException e) {
                Log.e(TAG, "Error reading NMEA data", e);
                close();
            }
        }

        public void close() {
            keepReading = false;
            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                Log.d(TAG, "Socket closed");
            } catch (IOException e) {
                Log.w(TAG, "Error closing socket", e);
            }
        }
    }


