package br.com.aistech.bluetoothlegatt.interfaces;

import android.app.Service;
import android.content.ServiceConnection;

/**
 * Created by jonathan on 01/02/17.
 */

public interface BluetoothInitializeCallback<T extends Service> extends BaseBluetoothCallback {
    void successfulInitialized(T service, ServiceConnection serviceConnection);

    void onDisconnected();
}
