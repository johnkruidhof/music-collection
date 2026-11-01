import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CollectionApiService } from '../../../core/api/collection-api.service';
import { LookupApiService } from '../../../core/api/lookup-api.service';
import { ReleaseCandidate } from '../../../core/models/release';
import { CandidateCard } from '../../../shared/candidate-card';
import { WishlistActions } from '../../../shared/wishlist-actions';
import { errorMessage } from '../../../shared/http-error';
import { scanFeedback } from '../../../shared/scan-feedback';
import { BarcodeScanner } from './barcode-scanner';

export type ScanState =
  | { kind: 'scanning' }
  | { kind: 'lookup'; barcode: string }
  | { kind: 'candidates'; barcode: string; candidates: ReleaseCandidate[] }
  | { kind: 'not-found'; barcode: string }
  | { kind: 'error'; barcode: string; message: string };

/**
 * Snelle-toevoeg-flow (CLAUDE.md §6): scannen → kandidaat bevestigen of kiezen → toevoegen →
 * direct weer scannen (batch-modus).
 */
@Component({
  selector: 'app-scan-page',
  providers: [WishlistActions],
  imports: [
    BarcodeScanner,
    CandidateCard,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './scan-page.html',
  styleUrl: './scan-page.scss',
})
export class ScanPage {
  protected readonly wishlist = inject(WishlistActions);
  private readonly lookupApi = inject(LookupApiService);
  private readonly collectionApi = inject(CollectionApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  readonly state = signal<ScanState>({ kind: 'scanning' });
  readonly adding = signal<ReleaseCandidate | null>(null);
  readonly addedCount = signal(0);

  protected readonly manualBarcode = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/^[\d\s-]*$/), barcodeLength],
  });

  protected readonly barcode = computed(() => {
    const s = this.state();
    return s.kind === 'scanning' ? '' : s.barcode;
  });
  protected readonly candidates = computed(() => {
    const s = this.state();
    return s.kind === 'candidates' ? s.candidates : [];
  });
  protected readonly errorText = computed(() => {
    const s = this.state();
    return s.kind === 'error' ? s.message : '';
  });

  onScanned(barcode: string): void {
    // De scanner kan dezelfde code meerdere keren achter elkaar melden.
    if (this.state().kind !== 'scanning') {
      return;
    }
    scanFeedback();
    this.lookup(barcode);
  }

  lookupManual(): void {
    this.manualBarcode.markAsTouched();
    if (this.manualBarcode.invalid) {
      return;
    }
    this.lookup(this.manualBarcode.value.replace(/[\s-]/g, ''));
  }

  lookup(barcode: string): void {
    this.state.set({ kind: 'lookup', barcode });
    this.lookupApi.byBarcode(barcode).subscribe({
      next: (candidates) =>
        this.state.set(
          candidates.length > 0
            ? { kind: 'candidates', barcode, candidates }
            : { kind: 'not-found', barcode },
        ),
      error: (err) => this.state.set({ kind: 'error', barcode, message: errorMessage(err) }),
    });
  }

  add(candidate: ReleaseCandidate): void {
    if (this.adding()) {
      return;
    }
    this.adding.set(candidate);
    this.collectionApi.addCandidate(candidate).subscribe({
      next: (item) => {
        this.adding.set(null);
        this.addedCount.update((n) => n + 1);
        this.manualBarcode.reset();
        this.snackBar
          .open(`Toegevoegd: ${item.release.artist} – ${item.release.title}`, 'Bekijken', {
            duration: 4000,
          })
          .onAction()
          .subscribe(() => void this.router.navigate(['/collectie', item.id]));
        this.rescan();
      },
      error: (err) => {
        this.adding.set(null);
        this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 });
      },
    });
  }

  rescan(): void {
    this.state.set({ kind: 'scanning' });
  }
}

function barcodeLength(control: { value: string }) {
  const digits = (control.value ?? '').replace(/[\s-]/g, '');
  return digits.length === 0 || (digits.length >= 8 && digits.length <= 14)
    ? null
    : { barcodeLength: true };
}
