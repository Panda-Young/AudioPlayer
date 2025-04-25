package com.panda.audioplayer.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.panda.audioplayer.utils.Logger

class PermissionManager(private val activity: Activity) {

    companion object {
        const val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1001
    }

    interface PermissionCallback {
        fun onAllPermissionsGranted()
        fun onPermissionsDenied()
    }

    private var callback: PermissionCallback? = null

    fun setPermissionCallback(callback: PermissionCallback) {
        this.callback = callback
    }

    fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check for READ_MEDIA_AUDIO permission on Android 13 (Tiramisu) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                Logger.logw("READ_MEDIA_AUDIO permission not granted, requesting permission")
            } else {
                Logger.logi("READ_MEDIA_AUDIO permission already granted")
            }
        } else {
            // Check for READ_EXTERNAL_STORAGE permission on older versions
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                Logger.logw("READ_EXTERNAL_STORAGE permission not granted, requesting permission")
            } else {
                Logger.logi("READ_EXTERNAL_STORAGE permission already granted")
            }
        }

        // Check for WRITE_EXTERNAL_STORAGE permission on older versions before Android 10 (Q)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                Logger.logw("WRITE_EXTERNAL_STORAGE permission not granted, requesting permission")
            } else {
                Logger.logi("WRITE_EXTERNAL_STORAGE permission already granted")
            }
        }

        // Check for MANAGE_EXTERNAL_STORAGE permission on Android 11 (R) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Logger.logw("MANAGE_EXTERNAL_STORAGE permission not granted, redirecting to settings")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            } else {
                Logger.logi("MANAGE_EXTERNAL_STORAGE permission granted")
            }
        }

        // Request permissions if needed
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), REQUEST_CODE_READ_EXTERNAL_STORAGE)
        } else {
            Logger.logi("All required permissions already granted")
            callback?.onAllPermissionsGranted()
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty()) {
                var allPermissionsGranted = true
                for (i in permissions.indices) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Logger.logi("${permissions[i]} permission granted")
                    } else {
                        Logger.logw("${permissions[i]} permission denied")
                        allPermissionsGranted = false
                    }
                }
                if (allPermissionsGranted) {
                    callback?.onAllPermissionsGranted()
                } else {
                    showPermissionDeniedDialog()
                }
            } else {
                Logger.logw("No permission results received")
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage("This app requires permissions to access audio files. Please grant the permissions to continue.")
            .setPositiveButton("OK") { _, _ ->
                // Open app settings to allow the user to manually grant permissions
                openAppSettings()
            }
            .setNegativeButton("Exit") { _, _ ->
                // Exit the app if the user refuses to grant permissions
                callback?.onPermissionsDenied()
            }
            .setCancelable(false) // Prevent dismissing the dialog by clicking outside
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", activity.packageName, null)
        activity.startActivity(intent)
    }
}
