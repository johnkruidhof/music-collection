import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { AdminApiService } from '../../core/api/admin-api.service';
import { AdminRelease } from '../../core/models/admin';
import { MediaTypePipe, SourcePipe } from '../../shared/label.pipes';

/** Kies de release waarin {@code data} opgaat; sluit met het gekozen doel. */
@Component({
  selector: 'app-merge-dialog',
  imports: [
    MediaTypePipe,
    ReactiveFormsModule,
    SourcePipe,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatRadioModule,
  ],
  template: `
    <h2 mat-dialog-title>Samenvoegen</h2>
    <mat-dialog-content>
      <p>
        <strong>{{ source.artist }} – {{ source.title }}</strong>
        ({{ source.collectionCount }} in collecties, {{ source.wishlistCount }} op verlanglijsten)
        gaat op in de release die je hieronder kiest en wordt daarna verwijderd.
      </p>
      <form (submit)="$event.preventDefault(); search()">
        <mat-form-field appearance="outline" class="full">
          <mat-label>Zoek de juiste release</mat-label>
          <input matInput [formControl]="query" autocomplete="off" />
          <button matSuffix mat-icon-button type="submit" aria-label="Zoeken">
            <mat-icon>search</mat-icon>
          </button>
        </mat-form-field>
      </form>
      <mat-radio-group class="results" [value]="target()" (change)="target.set($event.value)">
        @for (release of results(); track release.id) {
          <mat-radio-button [value]="release">
            {{ release.artist }} – {{ release.title }}
            <span class="meta">
              · {{ release.mediaType | mediaType
              }}{{ release.releaseYear ? ' · ' + release.releaseYear : '' }} ·
              {{ release.source | source }} · {{ release.collectionCount }} in collecties
            </span>
          </mat-radio-button>
        } @empty {
          @if (searched()) {
            <p>Geen andere releases gevonden.</p>
          }
        }
      </mat-radio-group>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Annuleren</button>
      <button mat-flat-button type="button" [disabled]="!target()" (click)="confirm()">
        Samenvoegen
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .full {
      width: 100%;
    }
    .results {
      display: grid;
      gap: 4px;
    }
    .meta {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.85rem;
    }
  `,
})
export class MergeDialog {
  protected readonly source = inject<AdminRelease>(MAT_DIALOG_DATA);
  private readonly adminApi = inject(AdminApiService);
  private readonly dialogRef = inject(MatDialogRef<MergeDialog, AdminRelease>);

  protected readonly query = new FormControl(`${this.source.artist} ${this.source.title}`.trim(), {
    nonNullable: true,
  });
  readonly results = signal<AdminRelease[]>([]);
  readonly target = signal<AdminRelease | null>(null);
  protected readonly searched = signal(false);

  constructor() {
    this.query.setValue(this.source.title);
    this.search();
  }

  search(): void {
    this.target.set(null);
    this.adminApi.search(this.query.value.trim() || null, null, 0, 20).subscribe((page) => {
      this.searched.set(true);
      this.results.set(page.content.filter((r) => r.id !== this.source.id));
    });
  }

  confirm(): void {
    const target = this.target();
    if (target) {
      this.dialogRef.close(target);
    }
  }
}
