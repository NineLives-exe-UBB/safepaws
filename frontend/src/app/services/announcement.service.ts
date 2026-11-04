
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Announcement,
  AnnouncementRequest,
} from '../models/announcement.model';
import { Photo } from '../models/photo.model';
import { MOCK_ANNOUNCEMENTS } from './mock-announcements';

@Injectable({
  providedIn: 'root',
})
export class AnnouncementService {
  private readonly http = inject(HttpClient);
  private readonly announcementsUrl = `${environment.apiUrl}/announcements`;

  private readonly mockAnnouncements: Announcement[] = [
    ...MOCK_ANNOUNCEMENTS.map((announcement) => ({
      ...announcement,
      photos: [...announcement.photos],
    })),
  ];

  getAll(): Observable<Announcement[]> {
    if (environment.useMockData) {
      return of(
        this.mockAnnouncements.map((announcement) => ({
          ...announcement,
          photos: [...announcement.photos],
        })),
      );
    }

    return this.http.get<Announcement[]>(this.announcementsUrl);
  }

  getById(id: number): Observable<Announcement> {
    if (environment.useMockData) {
      const announcement = this.mockAnnouncements.find(
        (item) => item.id === id,
      );

      if (!announcement) {
        return throwError(() => new Error('Announcement not found.'));
      }

      return of({
        ...announcement,
        photos: [...announcement.photos],
      });
    }

    return this.http.get<Announcement>(`${this.announcementsUrl}/${id}`);
  }

  create(announcement: AnnouncementRequest): Observable<Announcement> {
    if (environment.useMockData) {
      const newId =
        Math.max(0, ...this.mockAnnouncements.map((item) => item.id)) + 1;

      const createdAnnouncement: Announcement = {
        ...announcement,
        id: newId,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
        photos: [],
      };

      this.mockAnnouncements.unshift(createdAnnouncement);

      return of({
        ...createdAnnouncement,
        photos: [],
      });
    }

    return this.http.post<Announcement>(this.announcementsUrl, announcement);
  }

  photoUrl(photo: Photo): string {
    if (photo.url.startsWith('blob:')) {
      return photo.url;
    }

    return `${environment.apiUrl}${photo.url}`;
  }

  posterUrl(id: number): string {
    return `${this.announcementsUrl}/${id}/poster`;
  }

  uploadPhotos(id: number, files: File[]): Observable<Photo[]> {
    if (environment.useMockData) {
      const announcement = this.mockAnnouncements.find(
        (item) => item.id === id,
      );

      if (!announcement) {
        return throwError(() => new Error('Announcement not found.'));
      }

      const existingPhotoIds = this.mockAnnouncements.flatMap(
        (item) => item.photos.map((photo) => photo.id),
      );

      let nextPhotoId = Math.max(0, ...existingPhotoIds) + 1;

      const uploadedPhotos: Photo[] = files.map((file, index) => ({
        id: nextPhotoId++,
        url: URL.createObjectURL(file),
        isMain: announcement.photos.length === 0 && index === 0,
      }));

      announcement.photos.push(...uploadedPhotos);

      return of([...uploadedPhotos]);
    }

    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));

    return this.http.post<Photo[]>(
      `${this.announcementsUrl}/${id}/photos`,
      formData,
    );
  }
}
