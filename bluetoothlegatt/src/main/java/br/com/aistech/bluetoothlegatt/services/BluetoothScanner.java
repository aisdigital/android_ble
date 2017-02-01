package br.com.aistech.bluetoothlegatt.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.aistech.bluetoothlegatt.interfaces.BluetoothScanOnDeviceDiscovered;

/**
 * Created by jonathan on 01/02/17.
 */

public class BluetoothScanner {

    public static final String TAG = BluetoothAdapter.class.getSimpleName();

    private static final int SCAN_PERIOD = 20000;

    private static BluetoothAdapter.LeScanCallback scanCallback;
    private static BluetoothAdapter.LeScanCallback filteredScanCallback;

    public static void scan(Context context, final BluetoothScanOnDeviceDiscovered onDeviceDiscovered) {
        final BluetoothAdapter bluetoothAdapter = BluetoothService.getBluetoothAdapter(context);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(scanCallback);
            }
        }, SCAN_PERIOD);

        bluetoothAdapter.startLeScan(getDefaultScanCallback(onDeviceDiscovered));
    }

    public static void scanDevicesWithService(Context context, final UUID serviceUUID, final BluetoothScanOnDeviceDiscovered onDeviceDiscovered) {
        final BluetoothAdapter bluetoothAdapter = BluetoothService.getBluetoothAdapter(context);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(filteredScanCallback);
            }
        }, SCAN_PERIOD);

        bluetoothAdapter.startLeScan(getDefaultFilteredScanCallback(context, serviceUUID, onDeviceDiscovered));
    }

    private static BluetoothAdapter.LeScanCallback getDefaultScanCallback(final BluetoothScanOnDeviceDiscovered onDeviceDiscovered) {
        if (scanCallback == null) {
            scanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    final List<UUID> services = parseUUIDs(scanRecord);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            onDeviceDiscovered.onDeviceDiscovered(device, services);
                        }
                    });
                }
            };
        }
        return scanCallback;
    }

    private static BluetoothAdapter.LeScanCallback getDefaultFilteredScanCallback(Context context, final UUID serviceUUID, final BluetoothScanOnDeviceDiscovered onDeviceDiscovered) {
        final BluetoothAdapter bluetoothAdapter = BluetoothService.getBluetoothAdapter(context);

        if (filteredScanCallback == null) {
            filteredScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    final List<UUID> services = parseUUIDs(scanRecord);

                    if (services.contains(serviceUUID)) {

                        // When find the first occurrence, Stop!
                        bluetoothAdapter.stopLeScan(this);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                onDeviceDiscovered.onDeviceDiscovered(device, services);
                            }
                        });
                    }
                }
            };
        }
        return filteredScanCallback;
    }

    /**
     * To fix a buf in Android API > 4.3
     * <p>
     * BLE filtering in startLeScan(UUIDs, callback) doesn't work for 128-bit UUIDs
     *
     * @param advertisedData
     * @return
     * @see <a href="https://code.google.com/p/android/issues/detail?id=59490"
     */
    private static List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData,
                                    offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            Log.e(TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }

        return uuids;
    }
}
