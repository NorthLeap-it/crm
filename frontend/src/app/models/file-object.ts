// entities/FileObject.java — metadati di un allegato. Il `path` è il nome randomizzato su disco
// (non navigabile direttamente), il download passa sempre da GET /api/files/{id}.
export interface FileObject {
  id: string;
  filename: string;
  mime: string | null;
  size: number;
  path: string;
  uploadedBy: string | null;
  recordId: string | null;
  createdAt: string;
}
