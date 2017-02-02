package br.com.aistech.bluetoothlegatt.interfaces;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

/**
 * Created by jonathan on 01/02/17.
 */

public interface BluetoothConnectCallback extends BaseBluetoothCallback {

    /* Connection State */

    void onConnecting(BluetoothDevice device);

    void onConnected(BluetoothDevice device);

    void onDisconnected(BluetoothDevice device);

    /* Service Discovering */

    void onServicesDiscovered(BluetoothDevice device, List<BluetoothGattService> services);

    /* Characteristic State */

    void onCharacteristicRead(BluetoothGattCharacteristic characteristic);

    void onCharacteristicWrite(BluetoothGattCharacteristic characteristic);

    void onCharacteristicChanged(BluetoothGattCharacteristic characteristic);

    /* Not Found */

    void onDeviceNotFound(BluetoothDevice device);
}
