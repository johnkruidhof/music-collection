let audioContext: AudioContext | null = null;

/** Korte trilling en piep na een herkende barcode; stil falen waar de browser het niet ondersteunt. */
export function scanFeedback(): void {
  try {
    navigator.vibrate?.(80);
  } catch {
    // niet ondersteund (o.a. iOS)
  }
  try {
    audioContext ??= new AudioContext();
    const oscillator = audioContext.createOscillator();
    const gain = audioContext.createGain();
    oscillator.frequency.value = 880;
    gain.gain.value = 0.1;
    oscillator.connect(gain).connect(audioContext.destination);
    oscillator.start();
    oscillator.stop(audioContext.currentTime + 0.12);
  } catch {
    // geen audio beschikbaar
  }
}
