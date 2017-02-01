package br.com.aistech.bluetoothlegatt.interfaces;

import android.bluetooth.BluetoothDevice;

import java.util.List;
import java.util.UUID;

/**
 * Created by jonathan on 01/02/17.
 */

public interface BluetoothScanOnDeviceDiscovered {

    void onDeviceDiscovered(BluetoothDevice device, List<UUID> services);
}
