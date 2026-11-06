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
  UserLocation,
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
    const params: Record<string, string> = { ...toQueryParams(filters), sort: filters.sort };
    if (location) {
      params['userLatitude'] = String(location.latitude);
      params['userLongitude'] = String(location.longitude);
    }
    return this.http.get<AnnouncementSummary[]>(this.announcementsUrl, { params });
  }

  getById(id: number): Observable<Announcement> {
    return this.http.get<Announcement>(`${this.announcementsUrl}/${id}`);
  }

  create(announcement: AnnouncementRequest): Observable<Announcement> {
    return this.http.post<Announcement>(this.announcementsUrl, announcement);
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
