package br.com.aistech.bluetoothlegatt.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by jonathan on 01/02/17.
 */

public class PermissionUtils {

    public static final int BLUETOOTH_PERMISSION_CODE = 0x01;
    public static final int LOCATION_PERMISSION_CODE = 0x02;

    public static void isPermissionGranted(Activity activity, String permission, PermissionStatus permissionStatus) {
        switch (ContextCompat.checkSelfPermission(activity, permission)) {
            case PackageManager.PERMISSION_GRANTED:
                permissionStatus.granted();
                break;
            case PackageManager.PERMISSION_DENIED:
                permissionStatus.denied();
                break;
        }
    }

    public static Boolean isPermissionGranted(Activity activity, String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermission(Activity activity, String permission, int requestPermission) {
        if (isPermissionGranted(activity, permission)) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestPermission);
        }
    }

    /* Permission Callback */

    public interface PermissionStatus {
        public void granted();

        public void denied();
    }
}
