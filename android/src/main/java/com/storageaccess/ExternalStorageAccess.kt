package com.storageaccess

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext

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
          promise.resolve(null)
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

  fun createDirectory(dirPath: String, context: ReactApplicationContext, promise: Promise) {
    try {
      val uri = Uri.parse(dirPath)
      val parentDocument = DocumentFile.fromTreeUri(context, uri)

      if (parentDocument != null && parentDocument.exists()) {
        val newDirectory = parentDocument.createDirectory(dirPath)
        if (newDirectory != null && newDirectory.exists()) {
          promise.resolve(null)
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

  fun listFiles(dirPath: String, context: ReactApplicationContext, promise: Promise) {
    try {
      val uri = Uri.parse(dirPath)
      val documentFile = DocumentFile.fromTreeUri(context, uri)

      if (documentFile != null && documentFile.exists()) {
        val fileList = documentFile.listFiles().map { file ->
          mapOf(
            "name" to (file.name ?: ""),
            "uri" to file.uri.toString(),
            "isDirectory" to file.isDirectory,
            "isFile" to file.isFile,
            "length" to if (file.isFile) file.length() else 0
          )
        }
        promise.resolve(Arguments.fromList(fileList))
      } else {
        promise.reject("Directory Not Found", "Directory at path '$dirPath' not found")
      }
    } catch (e: Exception) {
      promise.reject("List Files Error", e.localizedMessage)
    }
  }

  fun getSubdirectoryUri(baseUri: String, subdirectory: String, context: ReactApplicationContext, promise: Promise) {
    try {
      val parentDocument = DocumentFile.fromTreeUri(context, Uri.parse(baseUri))
      val subdirectoryDocument = parentDocument?.findFile(subdirectory)

      if (subdirectoryDocument != null && subdirectoryDocument.exists()) {
        promise.resolve(subdirectoryDocument.uri.toString())
      } else {
        promise.reject("Directory Not Found", "Subdirectory not found")
      }
    } catch (e: Exception) {
      promise.reject("Error", e.localizedMessage)
    }
  }
}
