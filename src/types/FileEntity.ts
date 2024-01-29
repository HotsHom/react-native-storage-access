interface FileEntry {
  name: string; // Имя файла или директории
  uri: string; // URI файла или директории
  isDirectory: boolean; // Является ли элемент директорией
  isFile: boolean; // Является ли элемент файлом
  length: number; // Размер файла (для директорий обычно равен 0)
}
