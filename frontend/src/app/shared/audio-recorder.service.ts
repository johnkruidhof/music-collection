import { Injectable } from '@angular/core';

export interface Recording {
  blob: Blob;
  /** Met extensie die bij het formaat past (helpt de herkenningsdienst). */
  fileName: string;
}

/** Formaten in volgorde van voorkeur; Chrome/Firefox doen WebM/Opus, Safari MP4/AAC. */
const FORMATS = [
  { mimeType: 'audio/webm;codecs=opus', extension: 'webm' },
  { mimeType: 'audio/webm', extension: 'webm' },
  { mimeType: 'audio/mp4', extension: 'm4a' },
  { mimeType: 'audio/ogg;codecs=opus', extension: 'ogg' },
];

/**
 * Neemt een kort fragment op via de microfoon (F-31). Echo-/ruisonderdrukking en automatische versterking
 * staan uit: die zijn gemaakt voor spraak en vervormen muziek. Werkt alleen in een secure context.
 */
@Injectable({ providedIn: 'root' })
export class AudioRecorderService {
  /** Of opnemen in deze browser kan. */
  supported(): boolean {
    return typeof MediaRecorder !== 'undefined' && !!navigator.mediaDevices?.getUserMedia;
  }

  /**
   * @param onLevel krijgt ~10× per seconde het geluidsniveau (0–1), voor een niveaumeter
   */
  async record(durationMs: number, onLevel?: (level: number) => void): Promise<Recording> {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: false, noiseSuppression: false, autoGainControl: false },
    });
    const format = FORMATS.find((f) => MediaRecorder.isTypeSupported(f.mimeType));
    const recorder = new MediaRecorder(stream, format ? { mimeType: format.mimeType } : undefined);
    const chunks: Blob[] = [];
    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        chunks.push(event.data);
      }
    };
    const stopMeter = onLevel ? startLevelMeter(stream, onLevel) : () => undefined;

    try {
      await new Promise<void>((resolve, reject) => {
        recorder.onstop = () => resolve();
        recorder.onerror = () => reject(new Error('Opnemen mislukt.'));
        recorder.start();
        setTimeout(() => recorder.state !== 'inactive' && recorder.stop(), durationMs);
      });
    } finally {
      stopMeter();
      stream.getTracks().forEach((track) => track.stop());
    }
    const type = recorder.mimeType || format?.mimeType || 'audio/webm';
    const extension =
      FORMATS.find((f) => type.startsWith(f.mimeType.split(';')[0]))?.extension ?? 'webm';
    return { blob: new Blob(chunks, { type }), fileName: `opname.${extension}` };
  }
}

function startLevelMeter(stream: MediaStream, onLevel: (level: number) => void): () => void {
  try {
    const context = new AudioContext();
    const analyser = context.createAnalyser();
    analyser.fftSize = 512;
    context.createMediaStreamSource(stream).connect(analyser);
    const samples = new Uint8Array(analyser.fftSize);
    const timer = setInterval(() => {
      analyser.getByteTimeDomainData(samples);
      let peak = 0;
      for (const sample of samples) {
        peak = Math.max(peak, Math.abs(sample - 128) / 128);
      }
      onLevel(peak);
    }, 100);
    return () => {
      clearInterval(timer);
      void context.close();
    };
  } catch {
    return () => undefined;
  }
}

export function microphoneErrorMessage(err: unknown): string {
  const name = (err as { name?: string } | null)?.name;
  switch (name) {
    case 'NotAllowedError':
    case 'SecurityError':
      return 'Geen toegang tot de microfoon. Sta microfoontoegang toe in je browser.';
    case 'NotFoundError':
      return 'Geen microfoon gevonden.';
    case 'NotReadableError':
      return 'De microfoon is in gebruik door een andere app.';
    default:
      return 'Opnemen is mislukt. Probeer het opnieuw.';
  }
}
