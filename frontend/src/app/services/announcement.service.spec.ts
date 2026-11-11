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
    latitude: 46.7712,
    longitude: 23.6236,
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

  it('should only send the default status and sort when no filter is set', () => {
    service.getAll().subscribe();

    const req = httpController.expectOne((request) => request.url === announcementsUrl);
    expect(req.request.params.keys().sort()).toEqual(['sort', 'status']);
    expect(req.request.params.get('status')).toBe('ACTIVE');
    expect(req.request.params.get('sort')).toBe('newest');
    req.flush([]);
  });

  it('should send the filters as query parameters', () => {
    service
      .getAll({
        ...DEFAULT_FILTERS,
        type: 'LOST',
        species: 'CAT',
        status: 'RESOLVED',
        color: 'grey',
        breed: 'European',
        area: 'Cluj',
        sort: 'oldest',
      })
      .subscribe();

    const req = httpController.expectOne((request) => request.url === announcementsUrl);
    expect(req.request.params.get('type')).toBe('LOST');
    expect(req.request.params.get('species')).toBe('CAT');
    expect(req.request.params.get('status')).toBe('RESOLVED');
    expect(req.request.params.get('color')).toBe('grey');
    expect(req.request.params.get('breed')).toBe('European');
    expect(req.request.params.get('area')).toBe('Cluj');
    expect(req.request.params.get('sort')).toBe('oldest');
    req.flush([]);
  });

  it('should send the location and the radius when a radius is chosen', () => {
    service
      .getAll({ ...DEFAULT_FILTERS, radiusKm: 5 }, { latitude: 46.77, longitude: 23.6 })
      .subscribe();

    const req = httpController.expectOne((request) => request.url === announcementsUrl);
    expect(req.request.params.get('lat')).toBe('46.77');
    expect(req.request.params.get('lng')).toBe('23.6');
    expect(req.request.params.get('radiusKm')).toBe('5');
    req.flush([]);
  });

  it('should ask for the largest radius when sorting by distance without a radius', () => {
    service
      .getAll({ ...DEFAULT_FILTERS, sort: 'closest' }, { latitude: 46.77, longitude: 23.6 })
      .subscribe();

    const req = httpController.expectOne((request) => request.url === announcementsUrl);
    expect(req.request.params.get('sort')).toBe('closest');
    expect(req.request.params.get('radiusKm')).toBe('100');
    req.flush([]);
  });

  it('should not send the location when the filters do not need it', () => {
    service.getAll(DEFAULT_FILTERS, { latitude: 46.77, longitude: 23.6 }).subscribe();

    const req = httpController.expectOne((request) => request.url === announcementsUrl);
    expect(req.request.params.has('lat')).toBe(false);
    expect(req.request.params.has('lng')).toBe(false);
    expect(req.request.params.has('radiusKm')).toBe(false);
    req.flush([]);
  });

  it('should not send a radius without a location', () => {
    service.getAll({ ...DEFAULT_FILTERS, radiusKm: 5 }).subscribe();

    const req = httpController.expectOne((request) => request.url === announcementsUrl);
    expect(req.request.params.has('radiusKm')).toBe(false);
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

  it('should update the status of an announcement', () => {
    const resolved: Announcement = { ...announcement, status: 'RESOLVED' };

    service.updateStatus(15, 'RESOLVED').subscribe((result) => expect(result).toEqual(resolved));

    const req = httpController.expectOne(`${announcementsUrl}/15/status`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'RESOLVED' });
    req.flush(resolved);
  });

  it('should build the photo url from the backend url', () => {
    expect(service.photoUrl({ id: 3, url: '/photos/3', isMain: true })).toBe(
      `${environment.apiUrl}/photos/3`,
    );
  });

  it('should use the main photo as the main photo url', () => {
    expect(
      service.mainPhotoUrl([
        { id: 3, url: '/photos/3', isMain: false },
        { id: 4, url: '/photos/4', isMain: true },
      ]),
    ).toBe(`${environment.apiUrl}/photos/4`);
  });

  it('should use the first photo when none is the main one', () => {
    expect(service.mainPhotoUrl([{ id: 3, url: '/photos/3', isMain: false }])).toBe(
      `${environment.apiUrl}/photos/3`,
    );
  });

  it('should have no main photo url without photos', () => {
    expect(service.mainPhotoUrl([])).toBeNull();
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
