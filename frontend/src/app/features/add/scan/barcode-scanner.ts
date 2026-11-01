import {
  Component,
  DestroyRef,
  ElementRef,
  afterRenderEffect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import type { IScannerControls } from '@zxing/browser';

/**
 * Camerabeeld met barcodeherkenning (EAN-13, UPC-A, EAN-8, UPC-E) via @zxing/browser.
 * Zolang {@link active} onwaar is, loopt de camera door maar worden codes genegeerd; zo hoeft de camera
 * in batch-modus niet steeds opnieuw te starten. Werkt alleen in een secure context (HTTPS of localhost).
 */
@Component({
  selector: 'app-barcode-scanner',
  imports: [MatIconModule],
  templateUrl: './barcode-scanner.html',
  styleUrl: './barcode-scanner.scss',
})
export class BarcodeScanner {
  /** Alleen als {@code true} worden herkende codes doorgegeven. */
  readonly active = input(true);
  readonly scanned = output<string>();

  protected readonly error = signal<string | null>(null);
  protected readonly starting = signal(false);

  private readonly video = viewChild.required<ElementRef<HTMLVideoElement>>('video');
  private controls: IScannerControls | null = null;
  private destroyed = false;

  constructor() {
    // Eén keer starten zodra het video-element in de DOM staat.
    afterRenderEffect({ write: () => void this.start() });
    inject(DestroyRef).onDestroy(() => {
      this.destroyed = true;
      this.controls?.stop();
      this.controls = null;
    });
  }

  private async start(): Promise<void> {
    if (this.controls || this.starting() || this.error()) {
      return;
    }
    if (!navigator.mediaDevices?.getUserMedia) {
      this.error.set(
        'De camera is alleen beschikbaar via HTTPS. Vul de barcode hieronder handmatig in.',
      );
      return;
    }
    this.starting.set(true);
    try {
      const [{ BrowserMultiFormatReader }, { BarcodeFormat, DecodeHintType }] = await Promise.all([
        import('@zxing/browser'),
        import('@zxing/library'),
      ]);
      const hints = new Map<unknown, unknown>([
        [
          DecodeHintType.POSSIBLE_FORMATS,
          [BarcodeFormat.EAN_13, BarcodeFormat.UPC_A, BarcodeFormat.EAN_8, BarcodeFormat.UPC_E],
        ],
      ]);
      const reader = new BrowserMultiFormatReader(hints as never, {
        delayBetweenScanAttempts: 150,
      });
      const controls = await reader.decodeFromConstraints(
        { video: { facingMode: { ideal: 'environment' } }, audio: false },
        this.video().nativeElement,
        (result) => {
          if (result && this.active()) {
            this.scanned.emit(result.getText());
          }
        },
      );
      if (this.destroyed) {
        controls.stop();
      } else {
        this.controls = controls;
      }
    } catch (err) {
      this.error.set(cameraErrorMessage(err));
    } finally {
      this.starting.set(false);
    }
  }
}

export function cameraErrorMessage(err: unknown): string {
  const name = (err as { name?: string } | null)?.name;
  switch (name) {
    case 'NotAllowedError':
    case 'SecurityError':
      return 'Geen toegang tot de camera. Sta cameratoegang toe in je browser, of vul de barcode handmatig in.';
    case 'NotFoundError':
    case 'OverconstrainedError':
      return 'Geen camera gevonden. Vul de barcode handmatig in.';
    case 'NotReadableError':
      return 'De camera is in gebruik door een andere app.';
    default:
      return 'De camera kon niet gestart worden. Vul de barcode handmatig in.';
  }
}
