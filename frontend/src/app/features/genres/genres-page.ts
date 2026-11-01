import { NgTemplateOutlet } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, filter, switchMap } from 'rxjs';
import { GenreApiService } from '../../core/api/genre-api.service';
import { Genre, GenreImportResult } from '../../core/models/genre';
import { ConfirmDialog, ConfirmDialogData } from '../../shared/confirm-dialog';
import { GenreStore } from '../../shared/genre-store';
import { errorMessage } from '../../shared/http-error';

/** Wat er nu bewerkt wordt: een nieuw (sub)genre onder {@code parentId}, of een bestaand genre hernoemen. */
type Editing = { kind: 'new'; parentId: string | null } | { kind: 'rename'; genre: Genre } | null;

/** Eigen genres beheren en een boomstructuur van de muziekmap importeren (F-29). */
@Component({
  selector: 'app-genres-page',
  imports: [
    NgTemplateOutlet,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './genres-page.html',
  styleUrl: './genres-page.scss',
})
export class GenresPage {
  protected readonly store = inject(GenreStore);
  private readonly genreApi = inject(GenreApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly editing = signal<Editing>(null);
  protected readonly nameControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(100)],
  });
  readonly importing = signal(false);
  readonly importResult = signal<GenreImportResult | null>(null);

  constructor() {
    this.reload();
  }

  startNew(parentId: string | null = null): void {
    this.nameControl.reset('');
    this.editing.set({ kind: 'new', parentId });
  }

  startRename(genre: Genre): void {
    this.nameControl.reset(genre.name);
    this.editing.set({ kind: 'rename', genre });
  }

  cancel(): void {
    this.editing.set(null);
  }

  /** Is dit het formulier voor een nieuw subgenre onder {@code parentId} (of een nieuw hoofdgenre bij null)? */
  protected isNewUnder(parentId: string | null): boolean {
    const e = this.editing();
    return e?.kind === 'new' && e.parentId === parentId;
  }

  protected isRenaming(genre: Genre): boolean {
    const e = this.editing();
    return e?.kind === 'rename' && e.genre.id === genre.id;
  }

  save(): void {
    const editing = this.editing();
    const name = this.nameControl.value.trim();
    if (!editing || !name || this.nameControl.invalid) {
      this.nameControl.markAsTouched();
      return;
    }
    const request: Observable<Genre> =
      editing.kind === 'new'
        ? this.genreApi.create(name, editing.parentId)
        : this.genreApi.rename(editing.genre.id, name);
    request.subscribe({
      next: () => {
        this.editing.set(null);
        this.reload();
      },
      error: (err) => this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 }),
    });
  }

  remove(genre: Genre): void {
    const parts = [
      genre.subgenres.length > 0 ? `de ${genre.subgenres.length} subgenres` : '',
      'releases in dit genre houden hun plek, maar zonder genre',
    ].filter(Boolean);
    this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
        data: {
          title: `"${genre.name}" verwijderen?`,
          message: (genre.subgenres.length > 0 ? 'Ook ' : '') + parts.join('; ') + '.',
          confirmLabel: 'Verwijderen',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed === true),
        switchMap(() => this.genreApi.delete(genre.id)),
      )
      .subscribe({
        next: () => this.reload(),
        error: (err) => this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 }),
      });
  }

  onTreeSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      this.importTree(file);
    }
  }

  importTree(file: File): void {
    this.importing.set(true);
    this.importResult.set(null);
    this.genreApi.importTree(file).subscribe({
      next: (result) => {
        this.importing.set(false);
        this.importResult.set(result);
        this.reload();
      },
      error: (err) => {
        this.importing.set(false);
        this.snackBar.open(errorMessage(err), 'OK', { duration: 8000 });
      },
    });
  }

  protected itemCount(genre: Genre): number {
    return genre.itemCount + genre.subgenres.reduce((sum, s) => sum + s.itemCount, 0);
  }

  private reload(): void {
    this.store.reload().subscribe({
      error: (err) => this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 }),
    });
  }
}
