package com.storageaccess

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

class ExternalStorageAccess(private val context: Context) {

  private val myPluginScope = CoroutineScope(Dispatchers.IO)

  fun readFile(uriString: String, promise: Promise) {
    myPluginScope.launch {
      val uri = Uri.parse(uriString)
      val documentFile = DocumentFile.fromSingleUri(context, uri)
      val result = if (documentFile != null && documentFile.exists()) {
        context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
      } else null
      promise.resolve(result)
    }
  }

  fun writeFile(filePath: String, content: String, filename: String, extension: String, context: ReactApplicationContext, promise: Promise) {
    try {
      val parentUri = Uri.parse(filePath)
      val parentDocument = DocumentFile.fromTreeUri(context, parentUri)

      if (parentDocument != null && parentDocument.exists() && parentDocument.isDirectory) {
        ensureNoMediaFile(parentDocument)
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

  fun overwriteFile(fileUri: String, content: String, context: ReactApplicationContext, promise: Promise) {
    try {
      // Преобразуем строку URI в объект Uri
      val uri = Uri.parse(fileUri)

      // Открываем поток для записи в файл с флагом "rwt"
      context.contentResolver.openOutputStream(uri, "rwt").use { outputStream ->
        // Проверяем, что поток не null
        if (outputStream != null) {
          // Записываем новое содержимое
          outputStream.write(content.toByteArray())
          outputStream.flush() // Гарантируем, что все данные записаны
          promise.resolve(fileUri) // Успешно завершаем Promise
        } else {
          promise.reject("Output Stream Error", "Failed to open output stream")
        }
      }
    } catch (e: Exception) {
      promise.reject("Overwrite File Error", e.localizedMessage)
    }
  }

  fun convertToJpgAndCopy(sourceUriString: String, destinationDirUriString: String, fileName: String, context: ReactApplicationContext, promise: Promise) {
    myPluginScope.launch {
      try {
        val sourceUri = Uri.parse(sourceUriString)
        val inputStream = context.contentResolver.openInputStream(sourceUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val destinationDirUri = Uri.parse(destinationDirUriString)
        val destinationDir = DocumentFile.fromTreeUri(context, destinationDirUri)

        if (destinationDir != null && destinationDir.exists() && destinationDir.isDirectory) {
          ensureNoMediaFile(destinationDir)
          val existingFile = destinationDir.findFile("$fileName.jpg")
          existingFile?.delete() // Удаляем существующий файл, если он найден

          val mimeType = "image/jpeg"
          // После удаления существующего файла создаем новый
          val newFile = destinationDir.createFile(mimeType, "$fileName.jpg")

          newFile?.let { file ->
            val outputStream = context.contentResolver.openOutputStream(file.uri)
            if (outputStream != null) {
              val correctedBitmap = correctImageOrientation(sourceUri, bitmap, context)
              correctedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
              outputStream.close()
            }
            promise.resolve(file.uri.toString())
          } ?: run {
            promise.reject("File Creation Error", "Failed to create file in the directory")
          }
        } else {
          promise.reject("Destination Directory Error", "Destination directory does not exist or is not a directory")
        }
      } catch (e: Exception) {
        promise.reject("Convert and Copy File Error", e.localizedMessage)
      }
    }
  }

  private fun correctImageOrientation(imageUri: Uri, bitmap: Bitmap, context: Context): Bitmap {
    val inputStream = context.contentResolver.openInputStream(imageUri)
    val exifInterface = inputStream?.let { ExifInterface(it) }
    val orientation = exifInterface?.getAttributeInt(
      ExifInterface.TAG_ORIENTATION,
      ExifInterface.ORIENTATION_NORMAL
    )

    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  fun ensureNoMediaFile(directory: DocumentFile) {
    val noMediaFile = directory.findFile(".nomedia")
    if (noMediaFile == null || !noMediaFile.exists()) {
      directory.createFile("", ".nomedia")
    }
  }

  fun moveFolder(sourceUriString: String, destinationUriString: String, promise: Promise) {
    myPluginScope.launch {
      try {
        val sourceUri = Uri.parse(sourceUriString)
        val destinationUri = Uri.parse(destinationUriString)
        val sourceDocument = DocumentFile.fromTreeUri(context, sourceUri)
        val destinationDocument = DocumentFile.fromTreeUri(context, destinationUri)

        if (sourceDocument != null && sourceDocument.isDirectory && destinationDocument != null && destinationDocument.isDirectory) {
          copyFolderRecursively(sourceDocument, destinationDocument)
          sourceDocument.delete()
          promise.resolve("Folder moved successfully")
        } else {
          promise.reject("Move Folder Error", "Either source or destination is not valid")
        }
      } catch (e: Exception) {
        promise.reject("Move Folder Error", e.localizedMessage)
      }
    }
  }

  private suspend fun copyFolderRecursively(source: DocumentFile, destination: DocumentFile) {
    source.listFiles().forEach { file ->
      if (file.isDirectory) {
        val newFolder = destination.createDirectory(file.name ?: "NewFolder")
        newFolder?.let {
          copyFolderRecursively(file, it)
        }
      } else {
        val newFile = destination.createFile(file.type ?: "application/octet-stream", file.name ?: "NewFile")
        newFile?.let { destFile ->
          context.contentResolver.openInputStream(file.uri).use { inputStream ->
            context.contentResolver.openOutputStream(destFile.uri).use { outputStream ->
              inputStream?.copyTo(outputStream ?: return)
            }
          }
        }
      }
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

  private suspend fun listFilesRecursiveNew(
    context: Context,
    documentUri: Uri,
    currentDepth: Int = 0,
    maxDepth: Int = 2,
    includeSizeAndCount: Boolean = false
  ): WritableMap = coroutineScope {
    val projection = arrayOf(
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_SIZE,
      DocumentsContract.Document.COLUMN_MIME_TYPE
    )

    val fileMap = Arguments.createMap()

    suspend fun calculateDirectorySize(directoryUri: Uri): Long = coroutineScope {
      var size = 0L
      val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(directoryUri, DocumentsContract.getDocumentId(directoryUri))
      context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
        val sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
        val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

        while (cursor.moveToNext()) {
          val mimeType = cursor.getString(mimeTypeColumn)
          val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
          size += if (isDirectory) {
            val id = cursor.getString(idColumn)
            val childUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, id)
            val childSize = async(Dispatchers.IO) {
              calculateDirectorySize(childUri)
            }.await()
            childSize
          } else {
            val fileSize = cursor.getLong(sizeColumn)
            fileSize
          }
        }
      }
      size
    }

    val totalSize = if (includeSizeAndCount) {
      calculateDirectorySize(documentUri)
    } else 0L

    // Сначала обработаем корневую папку
    context.contentResolver.query(documentUri, projection, null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val name = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
        fileMap.putString("name", name)
        fileMap.putString("uri", documentUri.toString())
        fileMap.putBoolean("isDirectory", true)
        fileMap.putBoolean("isFile", false)
      }
    }

    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(documentUri, DocumentsContract.getDocumentId(documentUri))
    val includes = Arguments.createArray()
    var totalCount = 0

    context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
      while (cursor.moveToNext()) {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
        val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE))
        val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
        val childUri = DocumentsContract.buildDocumentUriUsingTree(documentUri, id)

        var childSize = 0L
        if (isDirectory && currentDepth + 1 < maxDepth) {
          val childResult = async(Dispatchers.IO) {
            listFilesRecursiveNew(context, childUri, currentDepth + 1, maxDepth, includeSizeAndCount)
          }.await()
          includes.pushMap(childResult)
        } else if (!isDirectory) {
          childSize = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE))
        }

        if (currentDepth + 1 == maxDepth || !isDirectory) {
          val childMap = Arguments.createMap().apply {
            putString("name", name)
            putString("uri", childUri.toString())
            putBoolean("isDirectory", isDirectory)
            putBoolean("isFile", !isDirectory)
            putDouble("totalSize", childSize.toDouble())
            putBoolean("isChildrenLoaded", false)
          }
          includes.pushMap(childMap)
        }

        totalCount++
      }
    }

    fileMap.putArray("includes", includes)
    fileMap.putDouble("totalSize", totalSize.toDouble())
    fileMap.putInt("totalCount", totalCount)
    fileMap.putBoolean("isChildrenLoaded", true)

    return@coroutineScope fileMap
  }

  fun listFiles(dirPath: String, maxDepth: Int, includeSizeAndCount: Boolean, context: Context, promise: Promise) {
    myPluginScope.launch {
      try {
        val uri = Uri.parse(dirPath)
        val result = listFilesRecursiveNew(context, uri, 0, maxDepth, includeSizeAndCount)
        promise.resolve(result)
      } catch (e: Exception) {
        e.printStackTrace()
        promise.reject("List Files Error", e.localizedMessage)
      }
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
