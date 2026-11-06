import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import {
  Announcement,
  AnnouncementRequest,
  AnnouncementSummary,
} from '../models/announcement.model';
import { DEFAULT_FILTERS } from '../models/announcement-filters.model';
import { AnnouncementService } from './announcement.service';

describe('AnnouncementService', () => {
  const announcementsUrl = `${environment.apiUrl}/announcements`;
  const request: AnnouncementRequest = {
    type: 'LOST',
    species: 'CAT',
    petName: 'Misi',
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
    id: 15,
    type: 'LOST',
    species: 'CAT',
    petName: 'Misi',
    breed: 'European',
    color: 'Grey with white paws',
    distinctiveMarks: 'Red collar',
    eventDate: '2026-10-30T18:00:00',
    latitude: 46.7712,
    longitude: 23.6236,
    area: 'Mănăștur, Cluj-Napoca',
    contactName: 'Ana Pop',
    contactPhone: '0712345678',
    status: 'ACTIVE',
    createdAt: '2026-11-01T14:30:00',
    photos: [],
  };
  const summary: AnnouncementSummary = {
    id: 15,
    type: 'LOST',
    petName: 'Misi',
    species: 'CAT',
    area: 'Mănăștur, Cluj-Napoca',
    eventDate: '2026-10-30T18:00:00',
    status: 'ACTIVE',
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
    let result: AnnouncementSummary[] = [];
    service.getAll().subscribe((announcements) => (result = announcements));

    const req = httpController.expectOne((request) => request.url === announcementsUrl);
    expect(req.request.method).toBe('GET');
    req.flush([summary]);

    expect(result).toEqual([summary]);
  });

  it('should only ask for the default sort when no filter is set', () => {
    service.getAll().subscribe();

    const req = httpController.expectOne((request) => request.url === announcementsUrl);
    expect(req.request.params.keys()).toEqual(['sort']);
    expect(req.request.params.get('sort')).toBe('NEWEST');
    req.flush([]);
  });

  it('should send the filters as query parameters', () => {
    service
      .getAll({ ...DEFAULT_FILTERS, species: 'CAT', color: 'grey', radiusKm: 5, sort: 'NEAREST' })
      .subscribe();

    const req = httpController.expectOne((request) => request.url === announcementsUrl);
    expect(req.request.params.get('species')).toBe('CAT');
    expect(req.request.params.get('color')).toBe('grey');
    expect(req.request.params.get('radiusKm')).toBe('5');
    expect(req.request.params.get('sort')).toBe('NEAREST');
    expect(req.request.params.has('type')).toBe(false);
    req.flush([]);
  });

  it('should send the user location when it is known', () => {
    service.getAll(DEFAULT_FILTERS, { latitude: 46.77, longitude: 23.6 }).subscribe();

    const req = httpController.expectOne((request) => request.url === announcementsUrl);
    expect(req.request.params.get('userLatitude')).toBe('46.77');
    expect(req.request.params.get('userLongitude')).toBe('23.6');
    req.flush([]);
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
