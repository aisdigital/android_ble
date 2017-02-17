package br.com.aistech.bluetoothle;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
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

    private final LinkedList<UUID> serviceList = new LinkedList<>(Arrays.asList(
            UUID.fromString("B34902E1-3FEF-45F6-963A-661A9E08714A")
    ));

    private final LinkedList<UUID> characteristicsList = new LinkedList<>(Arrays.asList(
            UUID.fromString("EA15CA03-BEA3-40FD-BCA7-33F89F553EDF"),
            UUID.fromString("6CFF3DC2-8AC8-4D3B-BC06-C0ACECC2C8A5"),
            UUID.fromString("51AD8773-00AA-41C1-9610-10A5F91E874C"),
            UUID.fromString("6ABF49C1-4DAF-4007-9D69-F4B4F1C3BE91"),
            UUID.fromString("BFB9339A-50A7-475A-B7E1-AC5C5395613F"),
            UUID.fromString("1EA9FDAA-16B2-46CF-A808-CC47D3AF0F1F"),
            UUID.fromString("0D083428-F5C1-4FD8-93B3-F3058BCC72E6"),
            UUID.fromString("FE00E5A8-A2A8-44BE-ADAF-7D1C9A59AC91")
    ));

    private final Handler handler = new Handler();
    private Runnable readRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        BluetoothScanner.scanDevicesWithService(MainActivity.this, UUID.fromString("B34902E1-3FEF-45F6-963A-661A9E08714A"), new BluetoothScanOnDeviceDiscovered() {
            @Override
            public void onDeviceDiscovered(final BluetoothDevice device, final List<UUID> services) {
                connectDevice(device, service);
            }
        });
    }

    private void connectDevice(final BluetoothDevice device, final BluetoothService service) {
        final BluetoothConnectCallback connectCallback = new BluetoothConnectCallback() {
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

                // Reconnect
                cancelReadRunnable();
                connectDevice(device, service);
            }

            @Override
            public void onServicesDiscovered(BluetoothDevice device, List<BluetoothGattService> services) {
                Log.e("BluetoothLE", "Services Discovered");
                enableNotificationForAllServicesAndCharacteristics(services);
            }

            @Override
            public void onCharacteristicRead(BluetoothGattCharacteristic characteristic) {
                try {
                    Log.e("BluetoothLE", "Characteristic READ (UUID: " + characteristic.getUuid() + ", VALUE: " + new String(characteristic.getValue(), "utf-8") + ")");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
                Log.e("BluetoothLE", "Characteristic Write");
            }

            @Override
            public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
                try {
                    Log.e("BluetoothLE", "Characteristic READ (UUID: " + characteristic.getUuid() + ", VALUE: " + new String(characteristic.getValue(), "utf-8") + ")");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
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

        cancelReadRunnable();

        final List<BluetoothGattCharacteristic> characteristics = new LinkedList<>();

        // Well, let's hear it out
        for (BluetoothGattService gattService : services) {

            // Parse only the know service
            if (serviceList.contains(gattService.getUuid())) {

                // Read only the know characteristics
                for (final BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                    characteristics.add(characteristic);
                }
            }
        }

        readRunnable = new Runnable() {
            public void run() {
                try {
                    for (BluetoothGattCharacteristic characteristic : characteristics) {

                        if (characteristicsList.contains(characteristic.getUuid())) {
                            MainActivity.this.service.readCharacteristic(characteristic);
                            Thread.sleep(1000);
                        }
                    }
                    handler.postDelayed(this, 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        handler.postDelayed(readRunnable, 1000);
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

    private void cancelReadRunnable() {
        if (readRunnable != null) {
            handler.removeCallbacks(readRunnable);
        }
    }

    /* Getters */


}
