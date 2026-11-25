/** The format of every date shown in the app, e.g. "30 Oct 2026, 18:00". */
export const DATE_FORMAT = 'dd MMM yyyy, HH:mm';

/** The day and the time of a sighting are shown on separate lines, e.g. "30 Oct 2026" and "18:00". */
export const DAY_FORMAT = 'dd MMM yyyy';
export const TIME_FORMAT = 'HH:mm';

/** Formats a date the way a datetime-local input expects it: YYYY-MM-DDTHH:mm, local time. */
export function toDateTimeInputValue(date: Date): string {
  const localTime = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localTime.toISOString().slice(0, 16);
}
