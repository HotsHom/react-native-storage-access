import * as FileSystem from './fileSystem';

const StorageAccess = {
  ...FileSystem,
};

export type { FileEntry } from './types/FileEntity';

export default StorageAccess;
