package com.example.mmplayer.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    
    companion object {
        const val STORAGE_PERMISSION_REQUEST_CODE = 1001
        const val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1002
        
        private val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
    
    fun hasStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager() || hasBasicStoragePermissions()
        } else {
            hasBasicStoragePermissions()
        }
    }
    
    private fun hasBasicStoragePermissions(): Boolean {
        return STORAGE_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestStoragePermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageExternalStoragePermission(activity)
            } else if (!hasBasicStoragePermissions()) {
                ActivityCompat.requestPermissions(
                    activity,
                    STORAGE_PERMISSIONS,
                    STORAGE_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                STORAGE_PERMISSIONS,
                STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun requestManageExternalStoragePermission(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            activity.startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            activity.startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
        }
    }
    
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return STORAGE_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        callback: (Boolean) -> Unit
    ) {
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                callback(allGranted)
            }
        }
    }
    
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, callback: (Boolean) -> Unit) {
        when (requestCode) {
            MANAGE_EXTERNAL_STORAGE_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    callback(Environment.isExternalStorageManager())
                } else {
                    callback(false)
                }
            }
        }
    }
}