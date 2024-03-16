import { NativeModules, Platform } from 'react-native';
import type { FileEntity } from './types/FileEntity';

const LINKING_ERROR =
  `The package 'react-native-storage-access' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const StorageAccess = NativeModules.StorageAccess
  ? NativeModules.StorageAccess
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

/**
 * Устанавливает тип разрешения для доступа к файлам.
 * @param {string} type Тип разрешения ('full' или 'directory').
 */
export function setPermissionType(type: 'full' | 'directory') {
  StorageAccess.setPermissionType(type);
}

/**
 * Запрашивает разрешение на доступ к файлам.
 * @return {Promise<void>} Промис, который выполнится после получения разрешения.
 */
export async function requestPermission(): Promise<string | any> {
  try {
    return await StorageAccess.requestPermission();
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error requesting permission: ${errorMessage}`);
  }
}

/**
 * Позволяет получить тип текущего URI.
 * @param {string | null} uriOrNull - URI string. Может быть null, в этом случае берётся путь из метода getAppDirectorySync().
 * @return {Promise<string>} Промис, возвращающий тип текущего URI: 'external', 'internal', 'unknown'.
 */
export async function getStorageType(
  uriOrNull?: string
): Promise<'internal' | 'external' | 'unknown'> {
  try {
    return StorageAccess.getStorageType(uriOrNull ?? null);
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error getting storage type: ${errorMessage}`);
  }
}

/**
 * Читает содержимое файла по указанному пути.
 * @param {string} filePath Путь к файлу.
 * @return {Promise<string>} Промис, возвращающий содержимое файла в виде строки.
 */
export async function readFile(filePath: string): Promise<string> {
  try {
    return await StorageAccess.readFile(filePath);
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error reading file: ${errorMessage}`);
  }
}

/**
 * Записывает текст в файл по указанному пути.
 * @param {string} rootPath Путь, где будет создан файл
 * @param {string} filename Имя файла
 * @param {string} extension Расширение файла
 * @param {string} content Контент, который будет записан
 * @return {Promise<string>} Промис, который возвращает URI нового файла.
 */
export async function writeFile(
  rootPath: string,
  filename: string,
  extension: string,
  content: string
): Promise<string> {
  try {
    return await StorageAccess.writeFile(
      rootPath,
      content,
      filename,
      extension
    );
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error writing file: ${errorMessage}`);
  }
}

/**
 * Перезаписывает текст в файл по указанному пути.
 * @param {string} fileUri Путь к файлу
 * @param {string} content Контент, который будет записан
 * @return {Promise<string>} Промис, который возвращает URI файла.
 */
export async function overwriteFile(
  fileUri: string,
  content: string
): Promise<string> {
  try {
    return await StorageAccess.overwriteFile(fileUri, content);
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error writing file: ${errorMessage}`);
  }
}

/**
 * Конвертирует изображение в JPF и копирует по указанному пути
 * @param {string} sourceUriString Путь к исходному изображению
 * @param {string} destinationDirUriString Путь к новому расположению файла
 * @param {string} fileName Имя файла
 * @return {Promise<string>} Промис, который возвращает URI нового файла.
 */
export async function convertToJpgAndCopy(
  sourceUriString: string,
  destinationDirUriString: string,
  fileName: string
): Promise<string> {
  try {
    return await StorageAccess.convertToJpgAndCopy(
      sourceUriString,
      destinationDirUriString,
      fileName
    );
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error writing file: ${errorMessage}`);
  }
}

/**
 * Перемещает папку по указанному пути
 * @param {string} sourceUriString Путь к исходной папке
 * @param {string} destinationDirUriString Путь к новому расположению папки
 * @return {Promise<void>} Промис
 */
export async function moveDirectory(
  sourceUriString: string,
  destinationDirUriString: string
): Promise<void> {
  try {
    await StorageAccess.moveDirectory(sourceUriString, destinationDirUriString);
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error writing file: ${errorMessage}`);
  }
}

/**
 * Удаляет файл по указанному пути.
 * @param {string} filePath Путь к файлу.
 * @return {Promise<void>} Промис, который выполнится после удаления файла.
 */
export async function deleteFile(filePath: string): Promise<void> {
  try {
    await StorageAccess.deleteFile(filePath);
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error deleting file: ${errorMessage}`);
  }
}

/**
 * Проверяет, существует ли файл по указанному пути.
 * @param {string} filePath Путь к файлу.
 * @return {Promise<boolean>} Промис, возвращающий true, если файл существует.
 */
export async function fileExists(filePath: string): Promise<boolean> {
  try {
    return await StorageAccess.fileExists(filePath);
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error checking file existence: ${errorMessage}`);
  }
}

/**
 * Перечисляет файлы и папки в указанной директории с возможностью контролировать глубину загрузки.
 *
 * @param dirPath Путь к директории для перечисления файлов и папок.
 * @param lazy Включает ленивую загрузку, во время неё загружается первый уровень вложенности и информация о следующем уровне вложенности.
 * @param includeSizeAndCount Включить информацию о размере и количестве элементов.
 *
 * @return {Promise<FileEntity>} Промис, возвращающий объект FileEntry.
 */
export async function listFiles(
  dirPath: string,
  lazy: boolean = false,
  includeSizeAndCount: boolean = false
): Promise<FileEntity> {
  try {
    return await StorageAccess.listFiles(
      dirPath,
      lazy ? 2 : -1,
      includeSizeAndCount
    );
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error listing files: ${errorMessage}`);
  }
}

/**
 * Создает новую директорию по указанному пути.
 * @param {string} dirName Название новой папки.
 * @param {string} parentDir Путь к родительской папке, где нужно создать новую папку
 * @return {Promise<string>} Промис, который выполнится после создания директории, возвращает путь к новой директории.
 */
export async function createDirectory(
  dirName: string,
  parentDir?: string
): Promise<string> {
  try {
    return await StorageAccess.createDirectory(dirName, parentDir ?? null);
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error creating directory: ${errorMessage}`);
  }
}

/**
 * Удаляет директорию по указанному пути.
 * @param {string} dirPath Путь к директории.
 * @return {Promise<void>} Промис, который выполнится после удаления директории.
 */
export async function deleteDirectory(dirPath: string): Promise<void> {
  try {
    await StorageAccess.deleteDirectory(dirPath);
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error deleting directory: ${errorMessage}`);
  }
}

/**
 * Проверяет, предоставлены ли необходимые разрешения.
 * @return {Promise<boolean>} Промис, возвращающий true, если разрешения предоставлены.
 */
export async function checkPermissions(): Promise<boolean> {
  try {
    return await StorageAccess.checkPermissions();
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error checking permissions: ${errorMessage}`);
  }
}

/**
 * Запускает интерфейс выбора директории пользователем.
 * @return {Promise<string>} Промис, возвращающий URI выбранной директории.
 */
export async function selectDirectory(): Promise<string> {
  try {
    return await StorageAccess.selectDirectory();
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error selecting directory: ${errorMessage}`);
  }
}

/**
 * Возвращает путь к текущей директории
 *
 * @returns {string} Путь к текущей выбранной директории, если не выбран внешний путь - возвращает путь к внутреннему хранилищу приложения
 */
export function getAppDirectorySync(): string {
  try {
    return StorageAccess.getAppDirectorySync();
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error getting app directory: ${errorMessage}`);
  }
}

/**
 * Получает URI поддиректории в выбранной пользователем директории.
 * @param {string} baseUri URI базовой директории.
 * @param {string} subdirectory Название поддиректории.
 * @return {Promise<string>} Промис, возвращающий URI поддиректории. Внимание! Данная директория или файл может не сущесвовать!.
 */
export async function getSubdirectoryUri(
  baseUri: string,
  subdirectory: string
): Promise<string> {
  try {
    return await StorageAccess.getSubdirectoryUri(baseUri, subdirectory);
  } catch (error) {
    const errorMessage = (error as Error).message;
    throw new Error(`Error getting subdirectory URI: ${errorMessage}`);
  }
}
export const appDirectory = getAppDirectorySync();
