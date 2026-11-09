import { provideHttpClient } from '@angular/common/http';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Observable, of, throwError } from 'rxjs';
import { FilterBar } from '../../components/filter-bar/filter-bar';
import { AnnouncementSummary } from '../../models/announcement.model';
import { DEFAULT_FILTERS, UserLocation } from '../../models/announcement-filters.model';
import { AnnouncementService } from '../../services/announcement.service';
import { LocationService } from '../../services/location.service';
import { AnnouncementList } from './announcement-list';

describe('AnnouncementList', () => {
  const clujNapoca: UserLocation = { latitude: 46.7712, longitude: 23.6236 };
  const summary = (id: number, eventDate: string, extra: Partial<AnnouncementSummary> = {}) =>
    ({
      id,
      type: 'LOST',
      petName: null,
      species: 'CAT',
      area: 'Centru, Cluj-Napoca',
      latitude: 46.77,
      longitude: 23.6,
      eventDate,
      status: 'ACTIVE',
      photos: [],
      ...extra,
    }) as AnnouncementSummary;
  const announcements = [
    summary(1, '2026-10-30T18:00:00'),
    summary(2, '2026-11-02T09:00:00', { type: 'FOUND', species: 'DOG' }),
  ];

  let getAll: ReturnType<typeof vi.fn>;
  let harness: RouterTestingHarness;
  let fixture: ComponentFixture<AnnouncementList>;
  let element: HTMLElement;

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
        provideRouter([{ path: 'announcements', component: AnnouncementList }]),
        {
          provide: AnnouncementService,
          useValue: { getAll, mainPhotoUrl: () => null, posterUrl: () => '#' },
        },
        {
          provide: LocationService,
          useValue: { getCurrentLocation: () => options.location ?? Promise.resolve(null) },
        },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(options.url ?? '/announcements');
    fixture = harness.fixture as unknown as ComponentFixture<AnnouncementList>;
    await settle();
  }

  async function settle(): Promise<void> {
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  const filterBar = (): DebugElement => fixture.debugElement.query(By.directive(FilterBar));
  const cardIds = () =>
    Array.from(element.querySelectorAll('.date')).map((date) => date.textContent?.trim());

  it('should show the page title and a card for each announcement', async () => {
    await setup();

    expect(element.querySelector('h1')?.textContent).toContain('Announcements');
    expect(element.querySelectorAll('app-announcement-card').length).toBe(2);
    expect(element.querySelector('app-filter-bar')).toBeTruthy();
  });

  it('should ask for the default filters when the URL has none', async () => {
    await setup();

    expect(getAll).toHaveBeenCalledWith(DEFAULT_FILTERS, null);
  });

  it('should read the filters from the URL', async () => {
    await setup({ url: '/announcements?species=CAT&color=grey' });

    expect(getAll).toHaveBeenCalledWith(
      { ...DEFAULT_FILTERS, species: 'CAT', color: 'grey' },
      null,
    );
    expect(element.querySelector<HTMLSelectElement>('#filter-species')?.value).toBe('CAT');
    expect(element.querySelector<HTMLInputElement>('#filter-color')?.value).toBe('grey');
  });

  it('should put the filters in the URL when they change', async () => {
    await setup();
    filterBar().componentInstance.filtersChange.emit({
      ...DEFAULT_FILTERS,
      species: 'DOG',
      area: 'Cluj',
    });
    await settle();

    expect(TestBed.inject(Router).url).toBe('/announcements?species=DOG&area=Cluj');
    expect(getAll).toHaveBeenLastCalledWith(
      { ...DEFAULT_FILTERS, species: 'DOG', area: 'Cluj' },
      null,
    );
  });

  it('should leave the URL clean when the filters are cleared', async () => {
    await setup({ url: '/announcements?species=CAT' });
    filterBar().componentInstance.filtersChange.emit(DEFAULT_FILTERS);
    await settle();

    expect(TestBed.inject(Router).url).toBe('/announcements');
  });

  it('should show a loading message until the announcements arrive', async () => {
    const pending = new Observable<AnnouncementSummary[]>();
    await setup({ response: pending });

    expect(element.textContent).toContain('Loading announcements');
    expect(element.querySelector('app-announcement-card')).toBeNull();
  });

  it('should show a message when the backend does not respond', async () => {
    await setup({ response: throwError(() => new Error('Server error')) });

    expect(element.querySelector('.error')?.textContent).toContain('Could not load');
    expect(element.querySelector('app-announcement-card')).toBeNull();
  });

  it('should show a message when there are no announcements at all', async () => {
    await setup({ response: of([]) });

    expect(element.querySelector('.empty')?.textContent).toContain(
      'There are no announcements yet',
    );
  });

  it('should show a message when no announcement matches the filters', async () => {
    await setup({ url: '/announcements?species=OTHER', response: of([]) });

    expect(element.querySelector('.empty')?.textContent).toContain(
      'No announcements match your filters',
    );
  });

  it('should enable the radius when the user allows the location', async () => {
    await setup({ location: Promise.resolve(clujNapoca) });

    expect(element.querySelector<HTMLSelectElement>('#filter-radius')?.disabled).toBe(false);
  });

  it('should not send the location when the filters do not need it', async () => {
    await setup({ location: Promise.resolve(clujNapoca) });

    expect(getAll).toHaveBeenCalledTimes(1);
    expect(getAll).toHaveBeenCalledWith(DEFAULT_FILTERS, null);
  });

  it('should send the location when a radius is chosen', async () => {
    await setup({ url: '/announcements?radiusKm=5', location: Promise.resolve(clujNapoca) });

    expect(getAll).toHaveBeenCalledWith({ ...DEFAULT_FILTERS, radiusKm: 5 }, clujNapoca);
  });

  it('should ask for resolved announcements when the URL says so', async () => {
    await setup({ url: '/announcements?status=RESOLVED' });

    expect(getAll).toHaveBeenCalledWith({ ...DEFAULT_FILTERS, status: 'RESOLVED' }, null);
  });

  it('should disable the radius and explain why when the user refuses', async () => {
    await setup();

    expect(element.querySelector<HTMLSelectElement>('#filter-radius')?.disabled).toBe(true);
    expect(element.querySelector('.location-message')?.textContent).toContain(
      'Allow location access to see announcements near you',
    );
  });

  it('should not ask the backend for distance filters when the location is refused', async () => {
    await setup({ url: '/announcements?radiusKm=5&sort=closest&species=CAT' });

    expect(getAll).toHaveBeenLastCalledWith({ ...DEFAULT_FILTERS, species: 'CAT' }, null);
  });

  it('should wait for the location before asking for distance filters', async () => {
    let allow: (location: UserLocation) => void = () => undefined;
    const location = new Promise<UserLocation | null>((resolve) => (allow = resolve));
    await setup({ url: '/announcements?radiusKm=5', location });

    expect(getAll).not.toHaveBeenCalled();
    expect(element.textContent).toContain('Loading announcements');

    allow(clujNapoca);
    await settle();

    expect(getAll).toHaveBeenCalledWith({ ...DEFAULT_FILTERS, radiusKm: 5 }, clujNapoca);
  });

  it('should show the announcements in the order the backend returns them', async () => {
    await setup();

    expect(cardIds()).toEqual(['30 Oct 2026, 18:00', '02 Nov 2026, 09:00']);
  });
});
