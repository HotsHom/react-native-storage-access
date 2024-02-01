package com.storageaccess

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap

class ExternalStorageAccess(private val context: Context) {

  fun readFile(uriString: String, promise: Promise) {
    val uri = Uri.parse(uriString)
    val documentFile = DocumentFile.fromSingleUri(context, uri)
    val result = if (documentFile != null && documentFile.exists()) {
      context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
    } else null
    promise.resolve(result)
  }

  fun writeFile(filePath: String, content: String, filename: String, extension: String, context: ReactApplicationContext, promise: Promise) {
    try {
      val parentUri = Uri.parse(filePath)
      val parentDocument = DocumentFile.fromTreeUri(context, parentUri)

      if (parentDocument != null && parentDocument.exists() && parentDocument.isDirectory) {
        val fileName = "$filename.$extension"
        val newFile = parentDocument.createFile("*/*", fileName)

        newFile?.let {
          context.contentResolver.openOutputStream(it.uri).use { outputStream ->
            outputStream?.writer()?.use { writer ->
              writer.write(content)
            }
          }
          promise.resolve(it.uri.toString())
        } ?: run {
          promise.reject("File Creation Error", "Failed to create file at path '$filePath'")
        }
      } else {
        promise.reject("Directory Not Found", "Directory at path '$filePath' not found")
      }
    } catch (e: Exception) {
      promise.reject("Write File Error", e.localizedMessage)
    }
  }

  fun deleteFile(filePath: String, context: ReactApplicationContext, promise: Promise) {
    try {
      val uri = Uri.parse(filePath)
      val documentFile = DocumentFile.fromSingleUri(context, uri)

      if (documentFile != null && documentFile.exists()) {
        if (documentFile.delete()) {
          promise.resolve(null)
        } else {
          promise.reject("Delete File Error", "Could not delete file at path '$filePath'")
        }
      } else {
        promise.reject("File Not Found", "File at path '$filePath' not found")
      }
    } catch (e: Exception) {
      promise.reject("Delete File Error", e.localizedMessage)
    }
  }

  fun fileExists(filePath: String, context: ReactApplicationContext, promise: Promise) {
    try {
      val uri = Uri.parse(filePath)
      val documentFile = DocumentFile.fromSingleUri(context, uri)
      promise.resolve(documentFile != null && documentFile.exists())
    } catch (e: Exception) {
      promise.reject("File Exists Error", e.localizedMessage)
    }
  }

  fun createDirectory(dirPath: String, folderName: String, context: ReactApplicationContext, promise: Promise) {
    try {
      val uri = Uri.parse(dirPath)
      val parentDocument = DocumentFile.fromTreeUri(context, uri)

      if (parentDocument != null && parentDocument.exists()) {
        val newDirectory = parentDocument.createDirectory(folderName)
        if (newDirectory != null && newDirectory.exists()) {
          promise.resolve(newDirectory.uri.toString())
        } else {
          promise.reject("Create Directory Error", "Could not create directory at path '$dirPath'")
        }
      } else {
        promise.reject("Parent Directory Not Found", "Parent directory not found")
      }
    } catch (e: Exception) {
      promise.reject("Create Directory Error", e.localizedMessage)
    }
  }

  fun deleteDirectory(dirPath: String, context: ReactApplicationContext, promise: Promise) {
    try {
      val uri = Uri.parse(dirPath)
      val documentFile = DocumentFile.fromTreeUri(context, uri)

      if (documentFile != null && documentFile.exists()) {
        if (documentFile.delete()) {
          promise.resolve(null)
        } else {
          promise.reject("Delete Directory Error", "Could not delete directory at path '$dirPath'")
        }
      } else {
        promise.reject("Directory Not Found", "Directory at path '$dirPath' not found")
      }
    } catch (e: Exception) {
      promise.reject("Delete Directory Error", e.localizedMessage)
    }
  }

  private fun listFilesRecursive(documentFile: DocumentFile, currentDepth: Int = 0, maxDepth: Int = 2, includeSizeAndCount: Boolean = false): WritableMap {
    val fileMap = Arguments.createMap().apply {
      putString("name", documentFile.name ?: "")
      putString("uri", documentFile.uri.toString())
      putBoolean("isDirectory", documentFile.isDirectory)
      putBoolean("isFile", documentFile.isFile)
      putBoolean("isChildrenLoaded", maxDepth == -1 || currentDepth < maxDepth)
    }

    if (documentFile.isDirectory && (maxDepth == -1 || currentDepth < maxDepth)) {
      val includes = Arguments.createArray()
      var totalSize = 0L
      var totalCount = 0

      for (file in documentFile.listFiles()) {
        if (currentDepth < maxDepth) {
          val childMap = listFilesRecursive(file, currentDepth + 1, maxDepth, includeSizeAndCount)
          includes.pushMap(childMap)
          if (includeSizeAndCount) {
            totalSize += if (file.isDirectory) childMap.getInt("totalSize").toLong() else file.length()
            totalCount++
          }
        }
      }

      fileMap.putArray("includes", includes)
      if (includeSizeAndCount) {
        fileMap.putInt("totalSize", totalSize.toInt())
        fileMap.putInt("totalCount", totalCount)
      }
    } else {
      fileMap.putInt("size", documentFile.length().toInt())
    }

    return fileMap
  }

  fun listFiles(dirPath: String, maxDepth: Int, includeSizeAndCount: Boolean, promise: Promise) {
    try {
      val uri = Uri.parse(dirPath)
      val documentFile = DocumentFile.fromTreeUri(context, uri)
      if (documentFile != null && documentFile.exists()) {
        promise.resolve(listFilesRecursive(documentFile, 0, maxDepth, includeSizeAndCount))
      } else {
        promise.reject("Directory Not Found", "Directory at path '$dirPath' not found")
      }
    } catch (e: Exception) {
      promise.reject("List Files Error", e.localizedMessage)
    }
  }

  fun getSubdirectoryUri(baseUri: String, subdirectory: String, context: ReactApplicationContext, promise: Promise) {
    try {
      val baseUriObj = Uri.parse(baseUri)
      val documentId = DocumentsContract.getTreeDocumentId(baseUriObj)
      val newDocumentId = "$documentId/$subdirectory"
      val newUri = DocumentsContract.buildDocumentUriUsingTree(baseUriObj, newDocumentId)

      promise.resolve(newUri.toString())
    } catch (e: Exception) {
      promise.reject("Error", e.localizedMessage)
    }
  }
}
