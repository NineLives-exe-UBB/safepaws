import { AnnouncementType } from './announcement.model';

export interface Coordinates {
  latitude: number;
  longitude: number;
}

export interface MapPin extends Coordinates {
  type: AnnouncementType;
  label?: string;
}
