package br.com.aistech.bluetoothlegatt.interfaces;

import br.com.aistech.bluetoothlegatt.exceptions.BTLAGException;

/**
 * Created by jonathan on 01/02/17.
 */

public interface BaseBluetoothCallback {
    void onError(BTLAGException exception);
}
