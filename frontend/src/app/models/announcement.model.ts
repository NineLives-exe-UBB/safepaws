
import { Photo } from './photo.model';

export type AnnouncementType = 'LOST' | 'FOUND';
export type AnnouncementStatus = 'ACTIVE' | 'RESOLVED';
export type Species = 'CAT' | 'DOG' | 'OTHER';

export const SPECIES_LABELS: Record<Species, string> = {
  CAT: 'Cat',
  DOG: 'Dog',
  OTHER: 'Other',
};

export interface AnnouncementRequest {
  type: AnnouncementType;
  species: Species;
  petName?: string;
  breed?: string;
  color?: string;
  distinctiveMarks?: string;
  eventDate: string;
  latitude?: number;
  longitude?: number;
  area: string;
  contactName: string;
  contactPhone?: string | null;
}

export interface AnnouncementSummary {
  id: number;
  type: AnnouncementType;
  petName: string | null;
  species: Species;
  area: string;
  latitude: number | null;
  longitude: number | null;
  eventDate: string;
  status: AnnouncementStatus;
  photos: Photo[];
  distanceKm?: number | null;
}

export interface Announcement extends AnnouncementSummary {
  ownerId: number | null;
  breed: string | null;
  color: string | null;
  distinctiveMarks: string | null;
  contactName: string;
  contactPhone: string | null;
  createdAt: string;
}
