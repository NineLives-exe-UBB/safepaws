import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { Sighting, SightingRequest } from '../models/sighting.model';
import { SightingService } from './sighting.service';

describe('SightingService', () => {
  const sighting: Sighting = {
    id: 4,
    latitude: 46.7571,
    longitude: 23.5547,
    seenAt: '2026-11-24T19:30:00',
    note: 'Under a parked car',
    reporter: { id: 2, displayName: 'Mihai Rus' },
    createdAt: '2026-11-24T19:45:00',
  };

  let service: SightingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SightingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should get the trail of an announcement', () => {
    let result: Sighting[] | undefined;

    service.getByAnnouncement(9).subscribe((sightings) => (result = sightings));
    const request = http.expectOne(`${environment.apiUrl}/announcements/9/sightings`);
    request.flush([sighting]);

    expect(request.request.method).toBe('GET');
    expect(result).toEqual([sighting]);
  });

  it('should add a sighting to an announcement', () => {
    const body: SightingRequest = {
      latitude: 46.7571,
      longitude: 23.5547,
      seenAt: '2026-11-24T19:30:00',
      note: 'Under a parked car',
    };
    let result: Sighting | undefined;

    service.create(9, body).subscribe((created) => (result = created));
    const request = http.expectOne(`${environment.apiUrl}/announcements/9/sightings`);
    request.flush(sighting);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(result).toEqual(sighting);
  });

  it('should delete a sighting', () => {
    let done = false;

    service.delete(4).subscribe(() => (done = true));
    const request = http.expectOne(`${environment.apiUrl}/sightings/4`);
    request.flush(null);

    expect(request.request.method).toBe('DELETE');
    expect(done).toBe(true);
  });
});
