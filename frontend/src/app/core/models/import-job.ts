/** Spiegelt nl.johnkruidhof.musiccollection.dto.importexport.ImportJobDto. */
export interface ImportJob {
  id: string;
  status: 'RUNNING' | 'FINISHED' | 'FAILED';
  total: number;
  processed: number;
  imported: number;
  skipped: number;
  failed: number;
  errors: string[];
  startedAt: string;
  finishedAt: string | null;
}
