package br.com.aistech.bluetoothle;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;
import java.util.UUID;

import br.com.aistech.bluetoothlegatt.exceptions.BTLAGException;
import br.com.aistech.bluetoothlegatt.interfaces.BluetoothInitializeCallback;
import br.com.aistech.bluetoothlegatt.interfaces.BluetoothScanOnDeviceDiscovered;
import br.com.aistech.bluetoothlegatt.services.BluetoothScanner;
import br.com.aistech.bluetoothlegatt.services.BluetoothService;

import static br.com.aistech.bluetoothlegatt.utils.PermissionUtils.LOCATION_PERMISSION_CODE;
import static br.com.aistech.bluetoothlegatt.utils.PermissionUtils.PermissionStatus;
import static br.com.aistech.bluetoothlegatt.utils.PermissionUtils.isPermissionGranted;
import static br.com.aistech.bluetoothlegatt.utils.PermissionUtils.requestPermission;

public class MainActivity extends AppCompatActivity {

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
                BluetoothScanner.scanDevicesWithService(MainActivity.this, UUID.fromString("5DD62B2B-6117-447D-84BD-1F6EAF12872B"), new BluetoothScanOnDeviceDiscovered() {
                    @Override
                    public void onDeviceDiscovered(BluetoothDevice device, List<UUID> services) {
                        Log.e("BluetoothScanner", "NAME: " + device.getName() + " - ADDRESS: " + device.getAddress() + " - SERVICES: " + services.toString());
                    }
                });
            }
        });


        // Requesting Permissions
        requestBluetoothPermission();
    }

    private void requestBluetoothPermission() {
        isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION, new PermissionStatus() {
            @Override
            public void granted() {
                BluetoothService.checkBluetoothEnable(MainActivity.this);

                BluetoothService.initialize(MainActivity.this, new BluetoothInitializeCallback() {
                    @Override
                    public void successfulInitialized(ServiceConnection serviceConnection) {
                        Log.e("BluetoothLE", "BLE Connected");
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

            @Override
            public void denied() {
                requestPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_CODE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
