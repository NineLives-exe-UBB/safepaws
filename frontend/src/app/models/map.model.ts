import { AnnouncementType } from './announcement.model';

export interface Coordinates {
  latitude: number;
  longitude: number;
}

/**
 * LOST and FOUND are announcements. SIGHTING is a place where a lost pet was seen. DRAFT is a place
 * the user just clicked, not saved yet: it does not count when the map zooms to show all the pins.
 */
export type PinType = AnnouncementType | 'SIGHTING' | 'DRAFT';

export interface MapPin extends Coordinates {
  type: PinType;
  /** Shown as the tooltip of the pin. */
  label?: string;
  /** Lets the page find what the pin stands for, e.g. to fill its popup. */
  id?: number;
}

/** What a popup template receives: the pin that was clicked. */
export interface PinPopupContext {
  $implicit: MapPin;
}
