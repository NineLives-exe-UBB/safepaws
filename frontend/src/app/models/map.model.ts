import { AnnouncementType } from './announcement.model';

export interface Coordinates {
  latitude: number;
  longitude: number;
}

export interface MapPin extends Coordinates {
  type: AnnouncementType;
  /** Shown as the tooltip of the pin. */
  label?: string;
  /** Lets the page find what the pin stands for, e.g. to fill its popup. */
  id?: number;
}

/** What a popup template receives: the pin that was clicked. */
export interface PinPopupContext {
  $implicit: MapPin;
}
