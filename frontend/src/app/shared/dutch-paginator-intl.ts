import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

/** Nederlandse teksten voor mat-paginator. */
@Injectable()
export class DutchPaginatorIntl extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Per pagina';
  override nextPageLabel = 'Volgende pagina';
  override previousPageLabel = 'Vorige pagina';
  override firstPageLabel = 'Eerste pagina';
  override lastPageLabel = 'Laatste pagina';

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0 || pageSize === 0) {
      return `0 van ${length}`;
    }
    const start = page * pageSize;
    const end = Math.min(start + pageSize, length);
    return `${start + 1} – ${end} van ${length}`;
  };
}
