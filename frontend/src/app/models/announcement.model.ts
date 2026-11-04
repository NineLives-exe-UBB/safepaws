
import { Photo } from './photo.model';

export type AnnouncementType = 'LOST' | 'FOUND';
export type AnnouncementStatus = 'ACTIVE' | 'RESOLVED';
export type Species = 'CAT' | 'DOG' | 'OTHER';

export interface AnnouncementRequest {
  type: AnnouncementType;
  species: Species;
  petName: string;
  breed: string;
  color: string;
  distinctiveMarks: string;
  eventDate: string;
  latitude: number | null;
  longitude: number | null;
  area: string;
  contactName: string;
  contactPhone: string;
}

export interface Announcement extends Omit<AnnouncementRequest, 'species'> {
  id: number;
  species: string;
  status: AnnouncementStatus;
  createdAt: string;
  photos: Photo[];
}
