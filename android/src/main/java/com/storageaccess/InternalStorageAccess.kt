package com.storageaccess

import android.content.Context
import android.net.Uri
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import java.io.File
import java.io.IOException

class InternalStorageAccess(private val context: Context) {

  fun readFile(filePath: String, promise: Promise) {
    try {
      val file = File(context.filesDir, filePath)
      if (file.exists()) {
        promise.resolve(file.readText())
      } else {
        promise.resolve(null)
      }
    } catch (e: IOException) {
      promise.reject("Error", e.localizedMessage)
    }
  }

  fun writeFile(filePath: String, content: String, promise: Promise) {
    return try {
      val file = File(context.filesDir, filePath)
      file.writeText(content)
      promise.resolve(true)
    } catch (e: IOException) {
      promise.resolve(false)
    }
  }

  fun deleteFile(filePath: String, promise: Promise) {
    return try {
      val file = File(context.filesDir, filePath)
      if (file.exists()) {
        promise.resolve(file.delete())
      } else {
        promise.resolve(false)
      }
    } catch (e: IOException) {
      promise.reject("Error", e.localizedMessage)
    }
  }

  fun fileExists(filePath: String, promise: Promise) {
    val file = File(context.filesDir, filePath)
    promise.resolve(file.exists())
  }

  fun createDirectory(dirPath: String, promise: Promise) {
    val dir = File(context.filesDir, dirPath)
    promise.resolve(dir.mkdirs())
  }

  fun deleteDirectory(dirPath: String, promise: Promise) {
    val dir = File(context.filesDir, dirPath)
    return if (dir.isDirectory) {
      promise.resolve(dir.deleteRecursively())
    } else {
      promise.resolve(false)
    }
  }

  fun listFiles(dirPath: String, promise: Promise) {
    val dir = File(context.filesDir, dirPath)
    val fileList = dir.listFiles()?.map { file ->
      mapOf(
        "name" to (file.name ?: ""),
        "uri" to Uri.fromFile(file),
        "isDirectory" to file.isDirectory,
        "isFile" to file.isFile,
        "length" to if (file.isFile) file.length() else 0
      )
    } ?: emptyList()
    promise.resolve(Arguments.fromList(fileList))
  }

  fun getSubdirectoryUri(subdirectory: String, promise: Promise) {
    val dir = File(context.filesDir, subdirectory)
    promise.resolve(Uri.fromFile(dir).toString())
  }
}
