package br.com.aistech.bluetoothlegatt.services;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import br.com.aistech.bluetoothlegatt.exceptions.BTLAGException;
import br.com.aistech.bluetoothlegatt.interfaces.BluetoothInitializeCallback;

/**
 * Created by jonathan on 01/02/17.
 */

public class BluetoothService extends Service {

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

    private static final int REQUEST_ENABLE_BT = 0x03;

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

    public static void initialize(final Context context, final BluetoothInitializeCallback callback) {
        // Code to manage Service lifecycle.
        new ServiceConnection() {

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

                callback.successfulInitialized(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                callback.onDisconnected();
            }
        };
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

    public static Boolean checkBluetoothEnable(Activity activity) {
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter(activity);

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return true;
        }
        return false;
    }

    public static BluetoothAdapter getBluetoothAdapter(Context context) {
        return ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
    }
}
