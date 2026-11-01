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
  templateUrl: './merge-dialog.html',
  styleUrl: './merge-dialog.scss',
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
