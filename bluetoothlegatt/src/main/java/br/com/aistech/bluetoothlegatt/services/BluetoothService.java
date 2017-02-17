package br.com.aistech.bluetoothlegatt.services;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import br.com.aistech.bluetoothlegatt.exceptions.BTLAGException;
import br.com.aistech.bluetoothlegatt.interfaces.BluetoothConnectCallback;
import br.com.aistech.bluetoothlegatt.interfaces.BluetoothInitializeCallback;

/**
 * Created by jonathan on 01/02/17.
 */

public class BluetoothService extends Service {

    private static final String TAG = BluetoothService.class.getSimpleName();

    /**
     * Local Binder
     */
    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    private final IBinder localBinder = new LocalBinder();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback bluetoothGattCallback;

    private String lastDeviceAddress;

    public static final int REQUEST_ENABLE_BT = 0x03;

    public static void initialize(final Context context, final BluetoothInitializeCallback callback) {
        // Code to manage Service lifecycle.
        ServiceConnection serviceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                BluetoothService bluetoothService = ((BluetoothService.LocalBinder) service).getService();

                // For API level 18 and above, get a reference to BluetoothAdapter through
                // BluetoothManager.
                if (bluetoothService.bluetoothManager == null) {
                    bluetoothService.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                    if (bluetoothService.bluetoothManager == null) {
                        callback.onError(new BTLAGException("Unable to initialize BluetoothManager."));
                        return;
                    }
                }

                bluetoothService.bluetoothAdapter = bluetoothService.bluetoothManager.getAdapter();
                if (bluetoothService.bluetoothAdapter == null) {
                    callback.onError(new BTLAGException("Unable to obtain a BluetoothAdapter."));
                    return;
                }

                callback.successfulInitialized(bluetoothService, this);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                callback.onDisconnected();
            }
        };
        Intent gattServiceIntent = new Intent(context, BluetoothService.class);
        context.bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (bluetoothGatt == null || bluetoothAdapter == null) {
            return;
        }
        bluetoothGatt.disconnect();
    }

    public void disconnectAndClose() {
        disconnect();
        close();
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *                The connection result is reported asynchronously through the
     *                {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *                callback.
     */
    public void connect(String address, final BluetoothConnectCallback connectCallback) {

        if (bluetoothAdapter == null) {
            connectCallback.onError(new BTLAGException("BluetoothAdapter not initialized"));
            return;
        }

        if (address == null || (address != null && address.isEmpty())) {
            connectCallback.onError(new BTLAGException("Invalid address"));
            return;
        }

        // Previously onConnected device.  Try to reconnect.
        if (lastDeviceAddress != null && address.equals(lastDeviceAddress) && bluetoothGatt != null) {
            if (bluetoothGatt.connect()) {
                connectCallback.onConnected(bluetoothGatt.getDevice());
                return;
            }
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            connectCallback.onDeviceNotFound(device);
            return;
        }

        this.bluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        connectCallback.onConnected(gatt.getDevice());
                        gatt.discoverServices();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        connectCallback.onDisconnected(gatt.getDevice());
                        break;
                    case BluetoothProfile.STATE_CONNECTING:
                        connectCallback.onConnecting(gatt.getDevice());
                        break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    connectCallback.onServicesDiscovered(gatt.getDevice(), gatt.getServices());
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    connectCallback.onCharacteristicRead(characteristic);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    connectCallback.onCharacteristicWrite(characteristic);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                connectCallback.onCharacteristicChanged(characteristic);
            }
        };

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.

        bluetoothAdapter.cancelDiscovery();

        // Apparently BLE can manage only one connection at time, so, we disconnect from anyone before attempting another connection!
        disconnectAndClose();

        bluetoothGatt = device.connectGatt(this, true, this.bluetoothGattCallback);
        lastDeviceAddress = address;
    }

     /*======= Characteristics Utils =======*/

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    // Max data length you can write which is 20 bytes
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, String data) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }

        Log.i(TAG, "Writing Characteristic " + characteristic.toString());
        try {
            Log.i(TAG, "data: " + URLEncoder.encode(data, "utf-8"));

            characteristic.setValue(URLEncoder.encode(data, "utf-8"));

            bluetoothGatt.writeCharacteristic(characteristic);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    /*======= Bluetooth Utils =======*/

    public static Boolean checkBluetoothEnable(Activity activity, int requestCode) {
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter(activity);

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, requestCode);
            return false;
        }
        return true;
    }

    public static BluetoothAdapter getBluetoothAdapter(Context context) {
        return ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
    }

    /* Services */

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    /* Getters */

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }
}
