import { provideHttpClient } from '@angular/common/http';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import * as L from 'leaflet';
import { Observable, of, throwError } from 'rxjs';
import { FilterBar } from '../../components/filter-bar/filter-bar';
import { MapView } from '../../components/map/map';
import { AnnouncementSummary } from '../../models/announcement.model';
import { DEFAULT_FILTERS, UserLocation } from '../../models/announcement-filters.model';
import { AnnouncementService } from '../../services/announcement.service';
import { LocationService } from '../../services/location.service';
import { MapPage } from './map-page';

describe('MapPage', () => {
  const clujNapoca: UserLocation = { latitude: 46.7712, longitude: 23.6236 };
  const announcement = (
    id: number,
    extra: Partial<AnnouncementSummary> = {},
  ): AnnouncementSummary => ({
    id,
    type: 'LOST',
    petName: 'Misi',
    species: 'CAT',
    area: 'Mănăștur, Cluj-Napoca',
    latitude: 46.755,
    longitude: 23.556,
    eventDate: '2026-10-30T18:00:00',
    status: 'ACTIVE',
    photos: [],
    ...extra,
  });
  const announcements = [
    announcement(1),
    announcement(2, {
      type: 'FOUND',
      species: 'DOG',
      petName: null,
      area: 'Timișoara',
      latitude: 45.7489,
      longitude: 21.2087,
    }),
  ];

  let getAll: ReturnType<typeof vi.fn>;
  let harness: RouterTestingHarness;
  let fixture: ComponentFixture<MapPage>;
  let element: HTMLElement;

  // jsdom has no layout, so Leaflet would see a map of size 0 and could not zoom or center
  beforeAll(() => {
    Object.defineProperty(HTMLElement.prototype, 'clientWidth', { configurable: true, value: 800 });
    Object.defineProperty(HTMLElement.prototype, 'clientHeight', {
      configurable: true,
      value: 400,
    });
  });

  afterAll(() => {
    Reflect.deleteProperty(HTMLElement.prototype, 'clientWidth');
    Reflect.deleteProperty(HTMLElement.prototype, 'clientHeight');
  });

  async function setup(
    options: {
      url?: string;
      response?: Observable<AnnouncementSummary[]>;
      location?: Promise<UserLocation | null>;
    } = {},
  ): Promise<void> {
    getAll = vi.fn().mockReturnValue(options.response ?? of(announcements));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideRouter([{ path: 'map', component: MapPage }]),
        {
          provide: AnnouncementService,
          useValue: {
            getAll,
            mainPhotoUrl: (photos: unknown[]) => (photos.length ? '/main.jpg' : null),
          },
        },
        {
          provide: LocationService,
          useValue: { getCurrentLocation: () => options.location ?? Promise.resolve(null) },
        },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(options.url ?? '/map');
    fixture = harness.fixture as unknown as ComponentFixture<MapPage>;
    await settle();
  }

  async function settle(): Promise<void> {
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  const debug = (type: typeof FilterBar | typeof MapView): DebugElement =>
    fixture.debugElement.query(By.directive(type));
  const leafletMap = () => debug(MapView).componentInstance['map'] as L.Map;
  const markers = () =>
    (debug(MapView).componentInstance['pinLayer'] as L.LayerGroup).getLayers() as L.Marker[];

  it('should show the page title, the filter bar and the map', async () => {
    await setup();

    expect(element.querySelector('h1')?.textContent).toContain('Map');
    expect(element.querySelector('app-filter-bar')).toBeTruthy();
    expect(element.querySelector('.leaflet-container')).toBeTruthy();
  });

  it('should show one pin for each announcement', async () => {
    await setup();

    expect(element.querySelectorAll('.leaflet-marker-icon').length).toBe(2);
  });

  it('should color the pins red for LOST and green for FOUND', async () => {
    await setup();

    expect(element.querySelectorAll('.map-pin-lost').length).toBe(1);
    expect(element.querySelectorAll('.map-pin-found').length).toBe(1);
  });

  it('should leave out announcements without a position and say so', async () => {
    await setup({
      response: of([...announcements, announcement(3, { latitude: null, longitude: null })]),
    });

    expect(element.querySelectorAll('.leaflet-marker-icon').length).toBe(2);
    expect(element.querySelector('.note')?.textContent).toContain(
      '1 announcement has no location and is not on the map',
    );
  });

  it('should count the announcements without a position', async () => {
    const withoutPosition = { latitude: null, longitude: null };
    await setup({
      response: of([announcement(3, withoutPosition), announcement(4, withoutPosition)]),
    });

    expect(element.querySelectorAll('.leaflet-marker-icon').length).toBe(0);
    expect(element.querySelector('.note')?.textContent).toContain(
      '2 announcements have no location and are not on the map',
    );
  });

  it('should not show a note when every announcement is on the map', async () => {
    await setup();

    expect(element.querySelector('.note')).toBeNull();
  });

  it('should open a popup with the pet name, the area, the date and a link to the details', async () => {
    await setup();
    markers()[0].openPopup();
    const popup = element.querySelector('.leaflet-popup') as HTMLElement;

    expect(popup.querySelector('.title')?.textContent).toContain('Misi');
    expect(popup.querySelector('.area')?.textContent).toContain('Mănăștur, Cluj-Napoca');
    expect(popup.querySelector('.date')?.textContent).toContain('30 Oct 2026, 18:00');
    expect(popup.querySelector('.details-link')?.getAttribute('href')).toBe('/announcements/1');
  });

  it('should show the species and the photo in the popup when there is no pet name', async () => {
    await setup({
      response: of([
        announcement(2, {
          petName: null,
          species: 'DOG',
          photos: [{ id: 1, url: '/photos/1', isMain: true }],
        }),
      ]),
    });
    markers()[0].openPopup();
    const popup = element.querySelector('.leaflet-popup') as HTMLElement;

    expect(popup.querySelector('.title')?.textContent).toContain('Dog');
    expect(popup.querySelector('img')?.getAttribute('src')).toBe('/main.jpg');
  });

  it('should center on the user when the location is allowed', async () => {
    await setup({ location: Promise.resolve(clujNapoca) });

    expect(leafletMap().getCenter().lat).toBeCloseTo(clujNapoca.latitude, 4);
    expect(leafletMap().getCenter().lng).toBeCloseTo(clujNapoca.longitude, 4);
    expect(leafletMap().getZoom()).toBe(13);
  });

  it('should zoom so that all the pins fit when the location is refused', async () => {
    await setup();
    const bounds = leafletMap().getBounds();

    expect(bounds.contains([46.755, 23.556])).toBe(true);
    expect(bounds.contains([45.7489, 21.2087])).toBe(true);
    expect(leafletMap().getZoom()).toBeGreaterThan(6);
  });

  it('should show a loading message until the announcements arrive', async () => {
    await setup({ response: new Observable<AnnouncementSummary[]>() });

    expect(element.textContent).toContain('Loading announcements');
  });

  it('should show a message when the backend does not respond', async () => {
    await setup({ response: throwError(() => new Error('Server error')) });

    expect(element.querySelector('.error')?.textContent).toContain('Could not load');
    expect(element.querySelectorAll('.leaflet-marker-icon').length).toBe(0);
  });

  it('should show a message when there are no announcements at all', async () => {
    await setup({ response: of([]) });

    expect(element.querySelector('.empty')?.textContent).toContain(
      'There are no announcements yet',
    );
  });

  it('should show a message when no announcement matches the filters', async () => {
    await setup({ url: '/map?species=OTHER', response: of([]) });

    expect(element.querySelector('.empty')?.textContent).toContain(
      'No announcements match your filters',
    );
  });

  it('should ask the backend with the same filters as the list does', async () => {
    await setup({ url: '/map?species=CAT&color=grey' });

    expect(getAll).toHaveBeenCalledWith(
      { ...DEFAULT_FILTERS, species: 'CAT', color: 'grey' },
      null,
    );
    expect(element.querySelector<HTMLSelectElement>('#filter-species')?.value).toBe('CAT');
  });

  it('should put the filters in the URL when they change', async () => {
    await setup();
    debug(FilterBar).componentInstance.filtersChange.emit({ ...DEFAULT_FILTERS, type: 'FOUND' });
    await settle();

    expect(TestBed.inject(Router).url).toBe('/map?type=FOUND');
    expect(getAll).toHaveBeenLastCalledWith({ ...DEFAULT_FILTERS, type: 'FOUND' }, null);
  });

  it('should send the location when a radius is chosen', async () => {
    await setup({ url: '/map?radiusKm=5', location: Promise.resolve(clujNapoca) });

    expect(getAll).toHaveBeenCalledWith({ ...DEFAULT_FILTERS, radiusKm: 5 }, clujNapoca);
  });
});
