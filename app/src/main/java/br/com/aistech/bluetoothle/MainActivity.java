package br.com.aistech.bluetoothle;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.util.List;
import java.util.UUID;

import br.com.aistech.bluetoothlegatt.exceptions.BTLAGException;
import br.com.aistech.bluetoothlegatt.interfaces.BluetoothConnectCallback;
import br.com.aistech.bluetoothlegatt.interfaces.BluetoothInitializeCallback;
import br.com.aistech.bluetoothlegatt.interfaces.BluetoothScanOnDeviceDiscovered;
import br.com.aistech.bluetoothlegatt.services.BluetoothScanner;
import br.com.aistech.bluetoothlegatt.services.BluetoothService;

import static br.com.aistech.bluetoothlegatt.services.BluetoothService.REQUEST_ENABLE_BT;
import static br.com.aistech.bluetoothlegatt.utils.PermissionUtils.LOCATION_PERMISSION_CODE;
import static br.com.aistech.bluetoothlegatt.utils.PermissionUtils.PermissionStatus;
import static br.com.aistech.bluetoothlegatt.utils.PermissionUtils.isPermissionGranted;
import static br.com.aistech.bluetoothlegatt.utils.PermissionUtils.requestPermission;

public class MainActivity extends AppCompatActivity {

    private BluetoothService service;
    private ServiceConnection serviceConnection;

    private BluetoothGattCharacteristic characteristicToBeWritten;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (characteristicToBeWritten != null) {
                    MainActivity.this.service.writeCharacteristic(characteristicToBeWritten, "Oh Look, a penny!");
                }
            }
        });

        // Requesting Permissions
        requestBluetoothPermission();
    }

    private void requestBluetoothPermission() {
        isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION, new PermissionStatus() {
            @Override
            public void granted() {
                // Will be responded in onActivityResult
                if (BluetoothService.checkBluetoothEnable(MainActivity.this, REQUEST_ENABLE_BT)) {
                    initializeBluetoothServices();
                }
            }

            @Override
            public void denied() {
                requestPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_CODE);
            }
        });
    }

    private void initializeBluetoothServices() {
        BluetoothService.initialize(MainActivity.this, new BluetoothInitializeCallback<BluetoothService>() {
            @Override
            public void successfulInitialized(final BluetoothService service, ServiceConnection serviceConnection) {
                // Used to manipulate Bluetooth features (e.g. receiving, sending data)
                MainActivity.this.service = service;

                // Used to unbinding this Activity to  Bluetooth service
                MainActivity.this.serviceConnection = serviceConnection;

                scanDevices(service);
            }

            @Override
            public void onDisconnected() {
                Log.e("BluetoothLE", "BLE Disconnected");
            }

            @Override
            public void onError(BTLAGException exception) {
                exception.printStackTrace();
            }
        });
    }

    private void scanDevices(final BluetoothService service) {
        BluetoothScanner.scanDevicesWithService(MainActivity.this, UUID.fromString("5DD62B2B-6117-447D-84BD-1F6EAF12872B"), new BluetoothScanOnDeviceDiscovered() {
            @Override
            public void onDeviceDiscovered(BluetoothDevice device, List<UUID> services) {
                connectDevice(device, service);
            }
        });
    }

    private void connectDevice(BluetoothDevice device, final BluetoothService service) {
        BluetoothConnectCallback connectCallback = new BluetoothConnectCallback() {
            @Override
            public void onConnecting(BluetoothDevice device) {
                Log.e("BluetoothLE", "Device Connecting");
            }

            @Override
            public void onConnected(BluetoothDevice device) {
                Log.e("BluetoothLE", "Device Connected");
            }

            @Override
            public void onDisconnected(BluetoothDevice device) {
                Log.e("BluetoothLE", "Device Disconnected");
            }

            @Override
            public void onServicesDiscovered(BluetoothDevice device, List<BluetoothGattService> services) {
                Log.e("BluetoothLE", "Services Discovered");
                enableNotificationForAllServicesAndCharacteristics(services);
            }

            @Override
            public void onCharacteristicRead(BluetoothGattCharacteristic characteristic) {
                Log.e("BluetoothLE", "Characteristic Read");
            }

            @Override
            public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
                Log.e("BluetoothLE", "Characteristic Write");
            }

            @Override
            public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
                Log.e("BluetoothLE", "Characteristic Changed");
            }

            @Override
            public void onDeviceNotFound(BluetoothDevice device) {
                Log.e("BluetoothLE", "Device Not Found");
            }

            @Override
            public void onError(BTLAGException exception) {
                exception.printStackTrace();
            }
        };

        service.connect(device.getAddress(), connectCallback);
    }

    private void enableNotificationForAllServicesAndCharacteristics(List<BluetoothGattService> services) {
        // Well, let's hear it out
        for (BluetoothGattService gattService : services) {
            for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {

                // Get an example to be written
                if (characteristic.getUuid().equals(UUID.fromString("A198DE6A-2D5D-462F-9CE1-9B34571B6BA3"))) {
                    MainActivity.this.characteristicToBeWritten = characteristic;
                }

                MainActivity.this.service.setCharacteristicNotification(characteristic, true);
            }
        }
    }

    /* Activity Methods */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else {
            initializeBluetoothServices();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }
}
