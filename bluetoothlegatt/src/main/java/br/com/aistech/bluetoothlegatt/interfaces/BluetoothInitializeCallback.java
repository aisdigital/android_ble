package br.com.aistech.bluetoothlegatt.interfaces;

import android.content.ServiceConnection;

import br.com.aistech.bluetoothlegatt.exceptions.BTLAGException;

/**
 * Created by jonathan on 01/02/17.
 */

public interface BluetoothInitializeCallback {
    void successfulInitialized(ServiceConnection serviceConnection);

    void onDisconnected();

    void onError(BTLAGException exception);
}
