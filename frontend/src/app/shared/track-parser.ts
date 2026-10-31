/** Hulpfuncties voor het (handmatig) invoeren van tracklijsten. */

export interface ParsedTrack {
  discNumber: number;
  position: string | null;
  title: string;
  duration: string | null;
}

const POSITION = /^((?:CD|DVD)?\d{1,2}[-.]\d{1,3}|[A-Z]{1,2}\d{0,3}[a-z]?|\d{1,3})[.)]?\s+(.+)$/;
const DURATION = /^(.*?)\s*[-–(]?\s*(\d{1,3}:\d{2}(?::\d{2})?)\)?$/;

/**
 * Schijfnummer uit de positie, zoals de backend: "2-5" → 2, "C1" → 2 (kant C = tweede plaat),
 * anders 1.
 */
export function discFromPosition(position: string | null | undefined): number {
  const value = (position ?? '').trim();
  const discTrack = /^[A-Za-z]*(\d+)[-.]\d+/.exec(value);
  if (discTrack) {
    return Math.max(1, Number(discTrack[1]));
  }
  const side = /^([A-Z])\d*[a-z]?$/.exec(value);
  if (side) {
    return Math.floor((side[1].charCodeAt(0) - 65) / 2) + 1;
  }
  return 1;
}

/**
 * Leest een geplakte tracklist, één track per regel, bv. "A1 Airbag 4:44", "1. Airbag (4:44)" of
 * alleen "Airbag". Lege regels worden overgeslagen.
 */
export function parseTrackLines(text: string): ParsedTrack[] {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line !== '')
    .map((line) => {
      const positioned = POSITION.exec(line);
      const position = positioned ? positioned[1] : null;
      const rest = positioned ? positioned[2] : line;
      const timed = DURATION.exec(rest);
      const title = (timed ? timed[1] : rest).trim().replace(/\s*[-–]\s*$/, '');
      return {
        discNumber: discFromPosition(position),
        position,
        title: title || rest,
        duration: timed ? timed[2] : null,
      };
    });
}
