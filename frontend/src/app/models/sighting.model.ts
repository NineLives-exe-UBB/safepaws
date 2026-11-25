export interface SightingReporter {
  id: number;
  displayName: string;
}

/** One point of the trail of a lost pet, as GET /announcements/{id}/sightings returns it. */
export interface Sighting {
  id: number;
  latitude: number;
  longitude: number;
  /** When the pet was seen, without a time zone, like every date of the API. */
  seenAt: string;
  note: string | null;
  reporter: SightingReporter;
  createdAt: string;
}

export interface SightingRequest {
  latitude: number;
  longitude: number;
  seenAt: string;
  note?: string;
}

export const MAX_NOTE_LENGTH = 500;
