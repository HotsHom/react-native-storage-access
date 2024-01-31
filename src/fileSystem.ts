import { NativeModules, Platform } from 'react-native';

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
 * @param {string} filePath Путь к файлу (включая имя файла и его расширение).
 * @param {string} content Содержимое для записи.
 * @return {Promise<void>} Промис, который выполнится после записи.
 */
export async function writeFile(
  filePath: string,
  content: string
): Promise<void> {
  try {
    const filename = filePath.split('/').pop()?.split('.').shift() || 'newfile';
    const extension = filePath.split('.').pop() || 'txt';

    await StorageAccess.writeFile(filePath, content, filename, extension);
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
 * Перечисляет файлы в указанной директории.
 * @param {string} dirPath - Путь к директории.
 * @return {Promise<FileEntry[]>} Промис, возвращающий массив {@link FileEntry}.
 */
export async function listFiles(dirPath: string): Promise<FileEntry[]> {
  try {
    return await StorageAccess.listFiles(dirPath);
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
