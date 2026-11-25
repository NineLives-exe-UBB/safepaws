import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Sighting, SightingRequest } from '../models/sighting.model';

@Injectable({
  providedIn: 'root',
})
export class SightingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /** The trail of an announcement, in the order the pet was seen. Public. */
  getByAnnouncement(announcementId: number): Observable<Sighting[]> {
    return this.http.get<Sighting[]>(`${this.apiUrl}/announcements/${announcementId}/sightings`);
  }

  create(announcementId: number, request: SightingRequest): Observable<Sighting> {
    return this.http.post<Sighting>(
      `${this.apiUrl}/announcements/${announcementId}/sightings`,
      request,
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/sightings/${id}`);
  }
}
