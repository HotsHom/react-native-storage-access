package com.storageaccess

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

private const val REQUEST_CODE_OPEN_DIRECTORY = 1
private const val REQUEST_CODE_FULL_ACCESS = 2
private const val PERMISSIONS_REQUEST_CODE = 100
private const val PREFS_NAME = "StorageAccessPrefs"
private const val KEY_SAVED_URI = "SAVED_URI"
private const val KEY_ACCESS_TYPE = "ACCESS_TYPE"

class StorageAccessModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener {

  private var permissionsPromise: Promise? = null
  private var permissionType: String = "full"
  private val externalStorageAccess = ExternalStorageAccess(reactContext)
  private val internalStorageAccess = InternalStorageAccess(reactContext)
  private val sharedPreferences: SharedPreferences by lazy {
    reactApplicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  init {
    reactContext.addActivityEventListener(this)
  }

  override fun getName(): String {
    return NAME
  }

  companion object {
    const val NAME = "StorageAccess"
  }

  @ReactMethod
  fun getStorageType(uriOrNull: String?, promise: Promise? = null): String {
    try {
      val uriString = uriOrNull ?: getAppDirectorySync()
      ?: throw IllegalArgumentException("Uri string and App directory both can't be null or empty")

      val uri = Uri.parse(uriString)
      val storageType = when {
        uri.scheme.equals("content", ignoreCase = true) -> "external"
        uri.path?.startsWith("/data/") == true -> "internal"
        else -> "unknown"
      }

      promise?.resolve(storageType)
      return storageType
    } catch (e: Exception) {
      promise?.reject("Error", e.localizedMessage)
      return "unknown"
    }
  }


  @ReactMethod
  fun setPermissionType(type: String) {
    permissionType = type
  }

  @ReactMethod
  fun readFile(uriString: String, promise: Promise) {
    when (getStorageType(uriString)) {
      "external" -> externalStorageAccess.readFile(uriString, promise)
      "internal" -> internalStorageAccess.readFile(uriString, promise)
      else -> promise.reject("Error", "Unsupported URI type")
    }
  }

  @ReactMethod
  fun writeFile(filePath: String, content: String, filename: String, extension: String, promise: Promise) {
    when (getStorageType(filePath)) {
      "external" -> externalStorageAccess.writeFile(filePath, content, filename, extension, reactApplicationContext, promise)
      "internal" -> internalStorageAccess.writeFile("$filePath/$filename.$extension", content, promise)
      else -> promise.reject("Error", "Unsupported URI type")
    }
  }

  @ReactMethod
  fun convertToJpgAndCopy(sourceUriString: String, destinationDirUriString: String, fileName: String, promise: Promise) {
    when (getStorageType(destinationDirUriString)) {
      "external" -> externalStorageAccess.convertToJpgAndCopy(sourceUriString, destinationDirUriString, fileName, reactApplicationContext, promise)
      "internal" -> promise.reject("Error", "Unsupported URI type")
      else -> promise.reject("Error", "Unsupported URI type")
    }
  }
  @ReactMethod
  fun overwriteFile(fileUri: String, content: String, promise: Promise) {
    when (getStorageType(fileUri)) {
      "external" -> externalStorageAccess.overwriteFile(fileUri, content, reactApplicationContext, promise)
      "internal" -> promise.reject("Error", "Unsupported URI type")
      else -> promise.reject("Error", "Unsupported URI type")
    }
  }

  @ReactMethod
  fun deleteFile(filePath: String, promise: Promise) {
    when (getStorageType(filePath)) {
      "external" -> externalStorageAccess.deleteFile(filePath, reactApplicationContext, promise)
      "internal" -> internalStorageAccess.deleteFile(filePath, promise)
      else -> promise.reject("Error", "Unsupported URI type")
    }
  }

  @ReactMethod
  fun fileExists(filePath: String, promise: Promise) {
    when (getStorageType(filePath)) {
      "external" -> externalStorageAccess.fileExists(filePath, reactApplicationContext, promise)
      "internal" -> internalStorageAccess.fileExists(filePath, promise)
      else -> promise.reject("Error", "Unsupported URI type")
    }
  }

  @ReactMethod
  fun listFiles(dirPath: String, maxDepth: Int = -1, includeSizeAndCount: Boolean = false, promise: Promise) {
    when (getStorageType(dirPath)) {
      "external" -> externalStorageAccess.listFiles(dirPath, maxDepth, includeSizeAndCount, reactApplicationContext, promise)
      "internal" -> internalStorageAccess.listFiles(dirPath, promise)
      else -> promise.reject("Error", "Unsupported URI type")
    }
  }

  @ReactMethod
  fun createDirectory(dirName: String, parentDir: String? = null, promise: Promise) {
    val parentDir_ = parentDir ?: getAppDirectorySync()
    if (parentDir_.isNullOrEmpty()) {
      promise.reject("Error", "Directory not found")
      return
    }
    when (getStorageType(parentDir)) {
      "external" -> externalStorageAccess.createDirectory(parentDir_, dirName, reactApplicationContext, promise)
      "internal" -> internalStorageAccess.createDirectory(parentDir, dirName, promise)
      else -> promise.reject("Error", "Unsupported URI type")
    }
  }

  @ReactMethod
  fun deleteDirectory(dirPath: String, promise: Promise) {
    when (getStorageType(dirPath)) {
      "external" -> externalStorageAccess.deleteDirectory(dirPath, reactApplicationContext, promise)
      "internal" -> internalStorageAccess.deleteDirectory(dirPath, promise)
      else -> promise.reject("Error", "Unsupported URI type")
    }
  }

  @ReactMethod
  fun requestPermission(promise: Promise) {
    val activity = currentActivity

    if (activity == null) {
      promise.reject("Activity Error", "Current activity is null.")
      return
    }

    permissionsPromise = promise

    try {
      when (permissionType) {
        "full" -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
//              flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
//                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
//                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
//                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
//            }
//            activity.startActivityForResult(intent, REQUEST_CODE_FULL_ACCESS)
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            activity.startActivity(intent)
          } else {
            requestFileAccessPermissions(activity, promise)
          }
        }
        "directory" -> {
          val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION
          }
          activity.startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)
        }
        else -> promise.reject("Invalid Access Type", "Invalid access type: $permissionType")
      }
    } catch (e: Exception) {
      promise.reject("Permission Request Error", e.localizedMessage)
    }
  }

  private fun requestFileAccessPermissions(activity: Activity, promise: Promise) {
    permissionsPromise = promise

    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

      ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
    } else {
      promise.resolve(null)
    }
  }

  override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
    if (resultCode != Activity.RESULT_OK || data == null) {
      permissionsPromise?.reject("Error", "Operation Cancelled or Invalid Data")
      permissionsPromise = null
      return
    }

    if (!(requestCode == PERMISSIONS_REQUEST_CODE || requestCode == REQUEST_CODE_OPEN_DIRECTORY || requestCode == REQUEST_CODE_FULL_ACCESS)) {
      permissionsPromise?.reject("Error", "Not a result")
      permissionsPromise = null
      return
    }

    when (requestCode) {
      PERMISSIONS_REQUEST_CODE -> {
        handlePermissionResult(activity, resultCode)
      }
      REQUEST_CODE_OPEN_DIRECTORY, REQUEST_CODE_FULL_ACCESS -> {
        val uri = data.data ?: return permissionsPromise!!.reject("URI Error", "No URI returned")
        savePersistableUriPermission(activity, uri, requestCode)
      }
    }
  }

  private fun handlePermissionResult(activity: Activity, resultCode: Int) {
    if (resultCode == Activity.RESULT_OK) {
      permissionsPromise?.resolve("Granted")
    } else {
      permissionsPromise?.reject("Denied", "Permission Denied")
    }
    permissionsPromise = null
  }

  private fun savePersistableUriPermission(activity: Activity, uri: Uri, requestCode: Int) {
    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    activity.contentResolver.takePersistableUriPermission(uri, takeFlags)
    val accessType = when (requestCode) {
      REQUEST_CODE_OPEN_DIRECTORY -> "directory"
      REQUEST_CODE_FULL_ACCESS -> "full"
      else -> "unknown"
    }
    saveUriAndAccessType(uri.toString(), accessType)
    permissionsPromise?.resolve(uri.toString())
    permissionsPromise = null
  }

  private fun saveUriAndAccessType(uriString: String, accessType: String) {
    sharedPreferences.edit().apply {
      putString(KEY_SAVED_URI, uriString)
      putString(KEY_ACCESS_TYPE, accessType)
      apply()
    }
  }

  @ReactMethod
  fun checkPermissions(promise: Promise) {
    val activity = currentActivity ?: return promise.reject("Activity Error", "Current activity is null.")

    val savedUri = sharedPreferences.getString(KEY_SAVED_URI, null)
    val savedAccessType = sharedPreferences.getString(KEY_ACCESS_TYPE, null)

    val granted = when {
      savedUri != null && savedAccessType != null -> {
        true
      }
      permissionType == "full" -> {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
      }
      else -> {
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
          ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
      }
    }

    promise.resolve(granted)
  }


  @ReactMethod
  fun selectDirectory(promise: Promise) {
    val activity = currentActivity ?: return promise.reject("Activity Error", "Current activity is null.")

    permissionsPromise = promise

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    activity.startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)
  }

  @ReactMethod(isBlockingSynchronousMethod = true)
  fun getAppDirectorySync(): String? {
    return try {
      val savedUri = sharedPreferences.getString(KEY_SAVED_URI, null)
      if (!savedUri.isNullOrEmpty()) {
        return savedUri
      } else {
        reactApplicationContext.filesDir.absolutePath
      }
    } catch (e: Exception) {
      null
    }
  }

  @ReactMethod
  fun getSubdirectoryUri(baseUri: String, subdirectory: String, promise: Promise) {
    when (getStorageType(baseUri)) {
      "external" -> externalStorageAccess.getSubdirectoryUri(baseUri, subdirectory, reactApplicationContext, promise)
      "internal" -> internalStorageAccess.getSubdirectoryUri(subdirectory, promise)
      else -> promise.reject("Error", "Unsupported URI type")
    }
  }

  override fun onNewIntent(p0: Intent?) {
    TODO("Not yet implemented")
  }
}
