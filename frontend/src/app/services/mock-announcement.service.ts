import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Announcement } from '../models/announcement.model';
import { Photo } from '../models/photo.model';
import { AnnouncementService } from './announcement.service';
import { MOCK_ANNOUNCEMENTS } from './mock-announcements';

@Injectable()
export class MockAnnouncementService extends AnnouncementService {
  override getAll(): Observable<Announcement[]> {
    return of(MOCK_ANNOUNCEMENTS);
  }

  override photoUrl(photo: Photo): string {
    return photo.url;
  }
}
