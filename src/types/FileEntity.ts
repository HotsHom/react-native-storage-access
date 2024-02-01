export interface FileEntity {
  name: string; // Имя файла или директории
  uri: string; // URI файла или директории
  isDirectory: boolean; // Является ли элемент директорией
  isFile: boolean; // Является ли элемент файлом
  size?: number; // Размер файла или общий размер содержимого директории, если includeSizeAndCount=true
  totalCount?: number; // Общее количество элементов в директории, если includeSizeAndCount=true и элемент является директорией
  includes?: FileEntity[]; // Список вложенных файлов или директорий, если элемент является директорией
}
