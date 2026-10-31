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
  template: `
    <div class="layout">
      <section class="scanner">
        <app-barcode-scanner [active]="state().kind === 'scanning'" (scanned)="onScanned($event)" />

        <form class="manual" (submit)="$event.preventDefault(); lookupManual()">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>Barcode handmatig invoeren</mat-label>
            <input matInput [formControl]="manualBarcode" inputmode="numeric" autocomplete="off" />
            @if (manualBarcode.touched && manualBarcode.invalid) {
              <mat-error>8 tot 14 cijfers</mat-error>
            }
          </mat-form-field>
          <button mat-stroked-button type="submit"><mat-icon>search</mat-icon> Opzoeken</button>
        </form>

        @if (addedCount() > 0) {
          <p class="counter">
            <mat-icon>library_add_check</mat-icon>
            {{ addedCount() }} {{ addedCount() === 1 ? 'release' : 'releases' }} toegevoegd in deze
            sessie
          </p>
        }
      </section>

      <section class="result" aria-live="polite">
        @switch (state().kind) {
          @case ('scanning') {
            <p class="hint">Richt de camera op de barcode van een plaat, cd, dvd of blu-ray.</p>
          }
          @case ('lookup') {
            <p>
              Barcode <strong>{{ barcode() }}</strong> opzoeken…
            </p>
            <mat-progress-bar mode="indeterminate" />
          }
          @case ('candidates') {
            <h2>
              {{
                candidates().length === 1
                  ? 'Gevonden'
                  : candidates().length + ' persingen gevonden — kies de juiste'
              }}
            </h2>
            <div class="candidates">
              @for (candidate of candidates(); track candidate.source + candidate.externalId) {
                <app-candidate-card
                  [candidate]="candidate"
                  [wishable]="true"
                  [wishBusy]="wishlist.isBusy(candidate)"
                  [wishedLocally]="wishlist.isWished(candidate)"
                  (wish)="wishlist.add($event)"
                  [busy]="adding() === candidate"
                  (add)="add($event)"
                />
              }
            </div>
            <button mat-button type="button" (click)="rescan()">
              <mat-icon>qr_code_scanner</mat-icon> Niet de goede — opnieuw scannen
            </button>
          }
          @case ('not-found') {
            <h2>Niets gevonden voor {{ barcode() }}</h2>
            <p>
              Deze barcode staat niet in Discogs of MusicBrainz. Zoek op artiest en titel, of voer
              de release zelf in.
            </p>
            <div class="actions">
              <a mat-flat-button routerLink="../zoeken" [queryParams]="{ barcode: barcode() }">
                <mat-icon>search</mat-icon> Zoeken
              </a>
              <a
                mat-stroked-button
                routerLink="../handmatig"
                [queryParams]="{ barcode: barcode() }"
              >
                <mat-icon>edit</mat-icon> Handmatig invoeren
              </a>
              <button mat-button type="button" (click)="rescan()">Opnieuw scannen</button>
            </div>
          }
          @case ('error') {
            <h2>Opzoeken mislukt</h2>
            <p class="error">{{ errorText() }}</p>
            <div class="actions">
              <button mat-flat-button type="button" (click)="lookup(barcode())">
                <mat-icon>refresh</mat-icon> Opnieuw proberen
              </button>
              <button mat-button type="button" (click)="rescan()">Opnieuw scannen</button>
            </div>
          }
        }
      </section>
    </div>
  `,
  styles: `
    .layout {
      display: grid;
      gap: 24px;
    }
    @media (min-width: 960px) {
      .layout {
        grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
        align-items: start;
      }
    }
    .manual {
      display: flex;
      gap: 8px;
      align-items: center;
      margin-top: 16px;
      max-width: 640px;
    }
    .manual mat-form-field {
      flex: 1;
    }
    .counter {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--mat-sys-primary);
    }
    .candidates {
      display: grid;
      gap: 12px;
      margin-bottom: 8px;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    .hint {
      color: var(--mat-sys-on-surface-variant);
    }
    .error {
      color: var(--mat-sys-error);
    }
  `,
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
