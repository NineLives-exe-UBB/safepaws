import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { Announcement, AnnouncementRequest } from '../models/announcement.model';
import { AnnouncementService } from './announcement.service';

describe('AnnouncementService', () => {
  const announcementsUrl = `${environment.apiUrl}/announcements`;
  const request: AnnouncementRequest = {
    type: 'LOST',
    species: 'Cat',
    breed: 'European',
    color: 'Grey with white paws',
    distinctiveMarks: 'Red collar',
    eventDate: '2026-10-30T18:00:00',
    latitude: 46.7712,
    longitude: 23.6236,
    area: 'Mănăștur, Cluj-Napoca',
    contactName: 'Ana Pop',
    contactPhone: '0712345678',
  };
  const announcement: Announcement = {
    ...request,
    id: 15,
    status: 'ACTIVE',
    createdAt: '2026-11-01T14:30:00',
    photos: [],
  };

  let service: AnnouncementService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AnnouncementService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpController.verify();
  });

  it('should get all announcements', () => {
    let result: Announcement[] = [];
    service.getAll().subscribe((announcements) => (result = announcements));

    const req = httpController.expectOne(announcementsUrl);
    expect(req.request.method).toBe('GET');
    req.flush([announcement]);

    expect(result).toEqual([announcement]);
  });

  it('should get one announcement by id', () => {
    let result: Announcement | undefined;
    service.getById(15).subscribe((found) => (result = found));

    const req = httpController.expectOne(`${announcementsUrl}/15`);
    expect(req.request.method).toBe('GET');
    req.flush(announcement);

    expect(result).toEqual(announcement);
  });

  it('should create an announcement', () => {
    let result: Announcement | undefined;
    service.create(request).subscribe((created) => (result = created));

    const req = httpController.expectOne(announcementsUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(announcement);

    expect(result).toEqual(announcement);
  });

  it('should build the photo url from the backend url', () => {
    expect(service.photoUrl({ id: 3, url: '/photos/3', isMain: true })).toBe(
      `${environment.apiUrl}/photos/3`,
    );
  });

  it('should build the poster url of an announcement', () => {
    expect(service.posterUrl(15)).toBe(`${announcementsUrl}/15/poster`);
  });

  it('should upload photos as multipart form data', () => {
    const file = new File(['photo'], 'cat.png', { type: 'image/png' });
    service.uploadPhotos(15, [file]).subscribe();

    const req = httpController.expectOne(`${announcementsUrl}/15/photos`);
    expect(req.request.method).toBe('POST');
    expect((req.request.body as FormData).getAll('files')).toEqual([file]);
    req.flush([{ id: 3, url: '/photos/3', isMain: true }]);
  });
});
