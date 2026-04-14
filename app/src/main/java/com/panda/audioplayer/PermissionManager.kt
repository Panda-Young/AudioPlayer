package com.panda.audioplayer.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                Logger.loge("READ_MEDIA_AUDIO permission not granted, requesting permission")
            } else {
                Logger.logi("READ_MEDIA_AUDIO permission already granted")
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                Logger.loge("READ_EXTERNAL_STORAGE permission not granted, requesting permission")
            } else {
                Logger.logi("READ_EXTERNAL_STORAGE permission already granted")
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
                        Logger.loge("${permissions[i]} permission denied")
                        allPermissionsGranted = false
                    }
                }
                if (allPermissionsGranted) {
                    callback?.onAllPermissionsGranted()
                } else {
                    showPermissionDeniedDialog()
                }
            } else {
                Logger.loge("No permission results received")
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Storage Permission Recommended")
            .setMessage("You can continue with SAF imported files even without storage permission. Granting permission enables local auto-scan.")
            .setPositiveButton("Go to Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Continue with Import Only") { _, _ ->
                callback?.onPermissionsDenied()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", activity.packageName, null)
        activity.startActivity(intent)
    }
}
