import { Photo } from './photo.model';

export type AnnouncementType = 'LOST' | 'FOUND';
export type AnnouncementStatus = 'ACTIVE' | 'RESOLVED';

export interface AnnouncementRequest {
  type: AnnouncementType;
  species: string;
  breed: string;
  color: string;
  distinctiveMarks: string;
  eventDate: string;
  latitude: number;
  longitude: number;
  area: string;
  contactName: string;
  contactPhone: string;
}

export interface Announcement extends AnnouncementRequest {
  id: number;
  status: AnnouncementStatus;
  createdAt: string;
  photos: Photo[];
}
