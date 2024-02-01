import * as FileSystem from './fileSystem';

const StorageAccess = {
  ...FileSystem,
};

export type { FileEntity } from './types/FileEntity';

export default StorageAccess;
