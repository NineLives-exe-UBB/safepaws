import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Announcement, AnnouncementRequest } from '../models/announcement.model';
import { Photo } from '../models/photo.model';

@Injectable({
  providedIn: 'root',
})
export class AnnouncementService {
  private readonly http = inject(HttpClient);
  private readonly announcementsUrl = `${environment.apiUrl}/announcements`;

  getAll(): Observable<Announcement[]> {
    return this.http.get<Announcement[]>(this.announcementsUrl);
  }

  getById(id: number): Observable<Announcement> {
    return this.http.get<Announcement>(`${this.announcementsUrl}/${id}`);
  }

  create(announcement: AnnouncementRequest): Observable<Announcement> {
    return this.http.post<Announcement>(this.announcementsUrl, announcement);
  }

  uploadPhotos(id: number, files: File[]): Observable<Photo[]> {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    return this.http.post<Photo[]>(`${this.announcementsUrl}/${id}/photos`, formData);
  }
}
