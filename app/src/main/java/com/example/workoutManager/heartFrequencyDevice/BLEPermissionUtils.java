package com.example.workoutManager.heartFrequencyDevice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;

public class BLEPermissionUtils {
    private static final int PERMISSION_REQUEST_CODE = 1;

    // Required permissions for different API levels
    private static final String[] PERMISSIONS_API_31_PLUS = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    };

    private static final String[] PERMISSIONS_API_30_AND_BELOW = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    // Check permissions based on API level
    public static boolean hasPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            return hasPermissionsList(context, PERMISSIONS_API_31_PLUS);
        } else { // Android 11 and below
            return hasPermissionsList(context, PERMISSIONS_API_30_AND_BELOW);
        }
    }

    // Request permissions dynamically
    public static void requestPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            ActivityCompat.requestPermissions(activity, PERMISSIONS_API_31_PLUS, PERMISSION_REQUEST_CODE);
        } else { // Android 11 and below
            ActivityCompat.requestPermissions(activity, PERMISSIONS_API_30_AND_BELOW, PERMISSION_REQUEST_CODE);
        }
    }

    // Helper method to check if permissions are granted
    private static boolean hasPermissionsList(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}


