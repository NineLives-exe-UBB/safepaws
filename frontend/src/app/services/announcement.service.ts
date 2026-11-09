import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Announcement,
  AnnouncementRequest,
  AnnouncementSummary,
} from '../models/announcement.model';
import {
  AnnouncementFilters,
  DEFAULT_FILTERS,
  MAX_RADIUS_KM,
  UserLocation,
  needsLocation,
  toQueryParams,
} from '../models/announcement-filters.model';
import { Photo } from '../models/photo.model';

@Injectable({
  providedIn: 'root',
})
export class AnnouncementService {
  private readonly http = inject(HttpClient);
  private readonly announcementsUrl = `${environment.apiUrl}/announcements`;

  getAll(
    filters: AnnouncementFilters = DEFAULT_FILTERS,
    location: UserLocation | null = null,
  ): Observable<AnnouncementSummary[]> {
    const params: Record<string, string> = {
      ...toQueryParams(filters),
      status: filters.status,
      sort: filters.sort,
    };
    delete params['radiusKm'];
    // With a location the backend also filters by radius (10 km when none is sent) and leaves out
    // announcements without coordinates, so it is only sent when the user asked for distance.
    if (location && needsLocation(filters)) {
      params['lat'] = String(location.latitude);
      params['lng'] = String(location.longitude);
      params['radiusKm'] = String(filters.radiusKm ?? MAX_RADIUS_KM);
    }
    return this.http.get<AnnouncementSummary[]>(this.announcementsUrl, { params });
  }

  getById(id: number): Observable<Announcement> {
    return this.http.get<Announcement>(`${this.announcementsUrl}/${id}`);
  }

  create(announcement: AnnouncementRequest): Observable<Announcement> {
    return this.http.post<Announcement>(this.announcementsUrl, announcement);
  }

  /** The photo marked as main, or the first one; null when there are no photos. */
  mainPhotoUrl(photos: Photo[]): string | null {
    const mainPhoto = photos.find((photo) => photo.isMain) ?? photos[0];
    return mainPhoto ? this.photoUrl(mainPhoto) : null;
  }

  photoUrl(photo: Photo): string {
    return `${environment.apiUrl}${photo.url}`;
  }

  posterUrl(id: number): string {
    return `${this.announcementsUrl}/${id}/poster`;
  }

  uploadPhotos(id: number, files: File[]): Observable<Photo[]> {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    return this.http.post<Photo[]>(`${this.announcementsUrl}/${id}/photos`, formData);
  }
}
