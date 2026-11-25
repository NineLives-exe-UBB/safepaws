import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By, Title as PageTitle } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import * as L from 'leaflet';
import { Observable, Subject, of, throwError } from 'rxjs';
import type { Mock } from 'vitest';
import { Announcement } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';
import { AnnouncementDetails } from './announcement-details';
import { signal } from '@angular/core';
import { User } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';
import { SightingService } from '../../services/sighting.service';
import { Sighting, SightingRequest } from '../../models/sighting.model';
import { MapView } from '../../components/map/map';

describe('AnnouncementDetails', () => {
  const announcement: Announcement = {
    id: 9,
    ownerId: 5,
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
    photos: [
      { id: 1, url: '/photos/1', isMain: false },
      { id: 2, url: '/photos/2', isMain: true },
    ],
  };

  const currentUser = signal<User | null>({
    id: 5,
    email: 'owner@example.com',
    displayName: 'Test Owner',
  });


  let getSightings: Mock<(id: number) => Observable<Sighting[]>>;
  let createSighting: Mock<(id: number, request: SightingRequest) => Observable<Sighting>>;
  let deleteSighting: Mock<(id: number) => Observable<void>>;
  let getById: Mock<(id: number) => Observable<Announcement>>;
  let updateStatus: Mock<(id: number, status: string) => Observable<Announcement>>;
  let fixture: ComponentFixture<AnnouncementDetails>;
  let element: HTMLElement;

  async function render(id: string | null = '9'): Promise<void> {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { user: currentUser },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(id === null ? {} : { id }) } },
        },
        {
          provide: SightingService,
          useValue: {
            getByAnnouncement: getSightings,
            create: createSighting,
            delete: deleteSighting,
          },
        },
        {
          provide: AnnouncementService,
          useValue: {
            getById,
            updateStatus,
            photoUrl: (photo: { url: string }) => `http://api${photo.url}`,
            posterUrl: (announcementId: number) =>
              `http://api/announcements/${announcementId}/poster`,
          },
        },
      ],
    });
    fixture = TestBed.createComponent(AnnouncementDetails);
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  // jsdom has no layout, so Leaflet would see a map of size 0 and could not zoom to the points
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

  const text = (selector: string) => element.querySelector(selector)?.textContent?.trim();

  beforeEach(() => {
    currentUser.set({
    id: 5,
    email: 'owner@example.com',
    displayName: 'Test Owner',
    });
    getSightings = vi.fn((_id: number) => of([]));
    createSighting = vi.fn((_id: number, request: SightingRequest) =>
      of({
        id: 50,
        ...request,
        note: request.note ?? null,
        reporter: { id: 5, displayName: 'Test Owner' },
        createdAt: '2026-11-03T12:00:00',
      }),
    );
    deleteSighting = vi.fn((_id: number) => of(undefined));
    getById = vi.fn((_id: number) => of(announcement));
    updateStatus = vi.fn((_id: number, status: string) =>
      of({ ...announcement, status } as Announcement),
    );
  });

  describe('loading', () => {
    it('should ask the backend for the announcement in the URL', async () => {
      await render('9');

      expect(getById).toHaveBeenCalledWith(9);
    });

    it('should show a loading message until the announcement arrives', async () => {
      getById.mockReturnValue(new Subject<Announcement>());
      await render();

      expect(text('.message')).toBe('Loading announcement...');
    });

    it.each(['abc', '0', '-3', '1.5', null])(
      'should show not found without calling the backend for the id %s',
      async (id) => {
        await render(id);

        expect(getById).not.toHaveBeenCalled();
        expect(text('h1')).toBe('Announcement not found');
      },
    );

    it('should show not found when the backend answers 404', async () => {
      getById.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
      await render();

      expect(text('h1')).toBe('Announcement not found');
    });

    it('should show an error when the backend fails for another reason', async () => {
      getById.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
      await render();

      expect(text('.message.error')).toBe(
        'Could not load the announcement. Please try again later.',
      );
    });

    it('should link back to the list', async () => {
      await render();

      expect(element.querySelector('.back-link')?.getAttribute('href')).toBe('/announcements');
    });
  });

  describe('information', () => {
    it('should show the name, the type and the species', async () => {
      await render();

      expect(text('h1')).toBe('Misi');
      expect(text('.subtitle')).toBe('Lost cat');
    });

    it('should use the species as the title when the pet has no name', async () => {
      getById.mockReturnValue(of({ ...announcement, petName: null, type: 'FOUND' }));
      await render();

      expect(text('h1')).toBe('Cat');
      expect(text('.subtitle')).toBe('Found cat');
    });

    it('should hide the phone number when it is not provided', async () => {
  getById.mockReturnValue(
    of({ ...announcement, contactPhone: null }),
  );

  await render();

  expect(text('.contact p')).toBe('Ana Pop');
  expect(element.querySelector('.phone')).toBeNull();
});

    it('should put the name of the pet in the browser tab', async () => {
      await render();

      expect(TestBed.inject(PageTitle).getTitle()).toBe('Misi | SafePaws');
    });

    it('should show every detail that is filled in', async () => {
      await render();
      const facts = text('.facts');

      expect(facts).toContain('European');
      expect(facts).toContain('Grey with white paws');
      expect(facts).toContain('Red collar');
      expect(facts).toContain('Lost on');
      expect(facts).toContain('30 Oct 2026, 18:00');
      expect(facts).toContain('Mănăștur, Cluj-Napoca');
      expect(facts).toContain('01 Nov 2026, 14:30');
    });

    it('should leave out the details that are empty', async () => {
      getById.mockReturnValue(
        of({ ...announcement, breed: null, color: null, distinctiveMarks: null, type: 'FOUND' }),
      );
      await render();
      const labels = [...element.querySelectorAll('.facts dt')].map((dt) => dt.textContent?.trim());

      expect(labels).toEqual(['Found on', 'Area', 'Posted on']);
    });

    it('should let the visitor call the contact person', async () => {
      await render();
      const phone = element.querySelector('.phone');

      expect(text('.contact p')).toBe('Ana Pop');
      expect(phone?.getAttribute('href')).toBe('tel:0712345678');
      expect(phone?.textContent?.trim()).toBe('0712345678');
    });


it('should show a Send message link for another logged-in user', async () => {
  currentUser.set({
    id: 8,
    email: 'other@example.com',
    displayName: 'Other User',
  });

  await render();

  const messageLink = element.querySelector<HTMLAnchorElement>(
    '.actions a.button.message-link',
  );

  expect(messageLink?.textContent?.trim()).toBe('Send message');
  expect(messageLink?.getAttribute('href')).toBe('/messages/9/5');
});

it('should send logged-out visitors to login', async () => {
  currentUser.set(null);

  await render();

  const messageLink = element.querySelector<HTMLAnchorElement>(
    '.actions a.button.message-link',
  );

  expect(messageLink?.textContent?.trim()).toBe('Send message');
  expect(messageLink?.getAttribute('href')).toBe('/login');
});

it('should not show Send message to the announcement owner', async () => {
  await render();

  const messageLink = element.querySelector(
    '.actions a.button.message-link',
  );

  expect(messageLink).toBeNull();
});


    it('should link to the poster of the announcement', async () => {
      await render();

      expect(element.querySelector('a[download]')?.getAttribute('href')).toBe(
        'http://api/announcements/9/poster',
      );
    });
  });

  describe('photos', () => {
    const shownPhoto = () => element.querySelector('.main-photo img')?.getAttribute('src');

    it('should show the main photo first', async () => {
      await render();

      expect(shownPhoto()).toBe('http://api/photos/2');
    });

    it('should switch the photo when a thumbnail is clicked', async () => {
      await render();
      const thumbnails = element.querySelectorAll<HTMLButtonElement>('.thumbnail');

      thumbnails[1].click();
      await fixture.whenStable();

      expect(thumbnails).toHaveLength(2);
      expect(shownPhoto()).toBe('http://api/photos/1');
      expect(thumbnails[1].getAttribute('aria-pressed')).toBe('true');
      expect(thumbnails[0].getAttribute('aria-pressed')).toBe('false');
    });

    it('should not show thumbnails for a single photo', async () => {
      getById.mockReturnValue(of({ ...announcement, photos: [announcement.photos[0]] }));
      await render();

      expect(element.querySelector('.thumbnails')).toBeNull();
      expect(shownPhoto()).toBe('http://api/photos/1');
    });

    it('should say when there is no photo', async () => {
      getById.mockReturnValue(of({ ...announcement, photos: [] }));
      await render();

      expect(element.querySelector('.main-photo img')).toBeNull();
      expect(text('.no-photo')).toBe('No photo');
    });
  });

  describe('status', () => {
    const statusButton = () => element.querySelector<HTMLButtonElement>('.button.secondary')!;


    it('should show the status button to the owner', async () => {
      await render();

      expect(statusButton()).not.toBeNull();
      expect(statusButton().textContent?.trim()).toBe('Mark as resolved');
    });

    it('should hide the status button from another user', async () => {
      currentUser.set({
        id: 8,
        email: 'other@example.com',
        displayName: 'Other User',
      });

      await render();

      expect(element.querySelector('.button.secondary')).toBeNull();
      expect(updateStatus).not.toHaveBeenCalled();
    });

  it('should hide the status button when logged out', async () => {
    currentUser.set(null);

    await render();

    expect(element.querySelector('.button.secondary')).toBeNull();
    expect(updateStatus).not.toHaveBeenCalled();
  });


    it('should mark an active announcement as resolved', async () => {
      await render();

      statusButton().click();
      await fixture.whenStable();

      expect(updateStatus).toHaveBeenCalledWith(9, 'RESOLVED');
      expect(text('.resolved-badge')).toBe('Resolved');
      expect(element.querySelector('.details')?.classList).toContain('resolved');
      expect(statusButton().textContent?.trim()).toBe('Mark as active');
    });

    it('should mark a resolved announcement as active again', async () => {
      getById.mockReturnValue(of({ ...announcement, status: 'RESOLVED' }));
      await render();

      statusButton().click();
      await fixture.whenStable();

      expect(updateStatus).toHaveBeenCalledWith(9, 'ACTIVE');
      expect(element.querySelector('.resolved-badge')).toBeNull();
      expect(statusButton().textContent?.trim()).toBe('Mark as resolved');
    });

    it('should disable the button while the status is being saved', async () => {
      updateStatus.mockReturnValue(new Subject<Announcement>());
      await render();

      statusButton().click();
      statusButton().click();
      await fixture.whenStable();

      expect(statusButton().disabled).toBe(true);
      expect(updateStatus).toHaveBeenCalledTimes(1);
    });

    it('should keep the status and show an error when saving fails', async () => {
      updateStatus.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
      await render();

      statusButton().click();
      await fixture.whenStable();

      expect(text('.status-error')).toBe('Could not update the status. Please try again.');
      expect(element.querySelector('.resolved-badge')).toBeNull();
      expect(statusButton().disabled).toBe(false);
    });


    it('should hide the status button when the announcement has no owner', async () => {
      getById.mockReturnValue(of({ ...announcement, ownerId: null }));

      await render();

      expect(statusButton()).toBeNull();
      expect(updateStatus).not.toHaveBeenCalled();
});

  });

  describe('location', () => {
    it('should show the location on a map', async () => {
      await render();

      expect(text('.location h2')).toBe('Location');
      expect(element.querySelector('.location app-map')).not.toBeNull();
    });

    it('should not show a map without coordinates', async () => {
      getById.mockReturnValue(of({ ...announcement, latitude: null, longitude: null }));
      await render();

      expect(element.querySelector('.location')).toBeNull();
    });
  });
  describe('sightings', () => {
    const sighting = (id: number, seenAt: string, extra: Partial<Sighting> = {}): Sighting => ({
      id,
      latitude: 46.77 + id / 1000,
      longitude: 23.62 + id / 1000,
      seenAt,
      note: null,
      reporter: { id: 8, displayName: 'Mihai Rus' },
      createdAt: '2026-11-03T12:00:00',
      ...extra,
    });

    const trail = [
      sighting(1, '2026-11-01T09:30:00', { note: 'Under a parked car' }),
      sighting(2, '2026-11-02T18:05:00'),
      sighting(3, '2026-11-03T07:45:00', { reporter: { id: 9, displayName: 'Elena Dan' } }),
    ];

    const otherUser = { id: 8, email: 'other@example.com', displayName: 'Mihai Rus' };

    const map = () =>
      fixture.debugElement.query(By.directive(MapView)).componentInstance as MapView;
    const leafletMap = () => map()['map'] as L.Map;
    const markers = () => (map()['pinLayer'] as L.LayerGroup).getLayers() as L.Marker[];
    const trailLines = () => (map()['trailLayer'] as L.LayerGroup).getLayers() as L.Polyline[];
    const titles = () => markers().map((marker) => marker.options.title);

    async function click(latitude: number, longitude: number): Promise<void> {
      leafletMap().fire('click', { latlng: L.latLng(latitude, longitude) });
      await fixture.whenStable();
    }

    function type(selector: string, value: string): void {
      const field = element.querySelector<HTMLInputElement>(selector)!;
      field.value = value;
      field.dispatchEvent(new Event('input'));
    }

    async function submit(): Promise<void> {
      element.querySelector<HTMLFormElement>('.sighting-form')!.dispatchEvent(new Event('submit'));
      await fixture.whenStable();
    }

    describe('the trail', () => {
      it('should ask for the sightings of a lost pet', async () => {
        await render('9');

        expect(getSightings).toHaveBeenCalledWith(9);
      });

      it('should not ask for the sightings of a found pet', async () => {
        getById.mockReturnValue(of({ ...announcement, type: 'FOUND' }));
        await render();

        expect(getSightings).not.toHaveBeenCalled();
        expect(element.querySelector('.trail-note')).toBeNull();
      });

      it('should show where it was lost and then every sighting in the order it was seen', async () => {
        getSightings.mockReturnValue(of([trail[2], trail[0], trail[1]]));
        await render();

        expect(titles()).toEqual([
          'Mănăștur, Cluj-Napoca',
          'Seen on 01 Nov 2026, 09:30',
          'Seen on 02 Nov 2026, 18:05',
          'Seen on 03 Nov 2026, 07:45',
        ]);
      });

      it('should join the points with a line in the same order', async () => {
        getSightings.mockReturnValue(of([trail[1], trail[0], trail[2]]));
        await render();
        const points = (trailLines()[0].getLatLngs() as L.LatLng[]).map((point) => point.lat);

        expect(trailLines()).toHaveLength(1);
        [46.7712, 46.771, 46.772, 46.773].forEach((latitude, i) =>
          expect(points[i]).toBeCloseTo(latitude, 6),
        );
      });

      it('should zoom to show all the points', async () => {
        getSightings.mockReturnValue(of(trail));
        await render();
        const bounds = leafletMap().getBounds();

        expect(bounds.contains([46.7712, 23.6236])).toBe(true);
        trail.forEach((point) => expect(bounds.contains([point.latitude, point.longitude])).toBe(true));
      });

      it('should draw no line when there are no sightings', async () => {
        await render();

        expect(trailLines()).toHaveLength(0);
        expect(markers()).toHaveLength(1);
        expect(text('.trail-note')).toBe('No one has reported seeing it yet.');
      });

      it('should still draw the line when the announcement has no location', async () => {
        getById.mockReturnValue(of({ ...announcement, latitude: null, longitude: null }));
        getSightings.mockReturnValue(of(trail));
        await render();

        expect(markers()).toHaveLength(3);
        expect(trailLines()).toHaveLength(1);
      });

      it('should explain the line when there are sightings', async () => {
        getSightings.mockReturnValue(of(trail));
        await render();

        expect(text('.trail-note')).toContain('in the order it was seen.');
      });

      it('should keep the map and say so when the sightings cannot be loaded', async () => {
        getSightings.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        await render();

        expect(text('.trail-note .error')).toBe('Could not load the sightings.');
        expect(markers()).toHaveLength(1);
      });
    });

    describe('the point of a sighting', () => {
      beforeEach(() => {
        getSightings.mockReturnValue(of(trail));
      });

      const popup = () => element.querySelector('.leaflet-popup .sighting-popup');

      it('should show the date, the time, the note and who added it', async () => {
        await render();
        markers()[1].openPopup();
        await fixture.whenStable();

        expect(popup()?.querySelector('.sighting-when')?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
          '01 Nov 2026 at 09:30',
        );
        expect(popup()?.querySelector('.sighting-note')?.textContent).toBe('Under a parked car');
        expect(popup()?.querySelector('.sighting-author')?.textContent).toBe('Added by Mihai Rus.');
      });

      it('should leave out the note when there is none', async () => {
        await render();
        markers()[2].openPopup();
        await fixture.whenStable();

        expect(popup()?.querySelector('.sighting-note')).toBeNull();
      });

      it('should say where and when the pet was lost on its own point', async () => {
        await render();
        markers()[0].openPopup();
        await fixture.whenStable();

        expect(popup()?.textContent?.trim()).toBe('Lost here, on 30 Oct 2026, 18:00.');
      });

      it('should say what the temporary point is', async () => {
        await render();
        await click(46.78, 23.63);
        markers()[markers().length - 1].openPopup();
        await fixture.whenStable();

        expect(popup()?.textContent?.trim()).toBe('Your sighting will be added here.');
      });
    });

    describe('deleting', () => {
      const deleteButton = () =>
        element.querySelector<HTMLButtonElement>('.leaflet-popup .button.danger');

      async function open(index: number): Promise<void> {
        markers()[index].openPopup();
        await fixture.whenStable();
      }

      beforeEach(() => {
        getSightings.mockReturnValue(of(trail));
      });

      it('should offer Delete to the owner of the announcement on every sighting', async () => {
        await render();
        await open(3);

        expect(deleteButton()?.textContent?.trim()).toBe('Delete');
      });

      it('should offer Delete to the author of the sighting', async () => {
        currentUser.set(otherUser);
        await render();
        await open(1);

        expect(deleteButton()?.textContent?.trim()).toBe('Delete');
      });

      it('should not offer Delete on the sighting of someone else', async () => {
        currentUser.set(otherUser);
        await render();
        await open(3);

        expect(deleteButton()).toBeNull();
      });

      it('should not offer Delete when logged out', async () => {
        currentUser.set(null);
        await render();
        await open(1);

        expect(deleteButton()).toBeNull();
      });

      it('should delete the sighting and take its point off the map', async () => {
        await render();
        await open(2);

        deleteButton()!.click();
        await fixture.whenStable();

        expect(deleteSighting).toHaveBeenCalledWith(2);
        expect(markers()).toHaveLength(3);
        expect(titles()).not.toContain('Seen on 02 Nov 2026, 18:05');
        expect((trailLines()[0].getLatLngs() as L.LatLng[]).length).toBe(3);
      });

      it('should keep the sighting and say so when deleting fails', async () => {
        deleteSighting.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        await render();
        await open(2);

        deleteButton()!.click();
        await fixture.whenStable();

        expect(text('.sighting-error')).toBe('Could not delete the sighting. Please try again.');
        expect(markers()).toHaveLength(4);
      });

      it('should say when the user is not allowed to delete it', async () => {
        deleteSighting.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
        await render();
        await open(2);

        deleteButton()!.click();
        await fixture.whenStable();

        expect(text('.sighting-error')).toBe(
          'You can only delete your own sightings or sightings on your announcements.',
        );
      });

      it('should drop a sighting that somebody else already deleted', async () => {
        deleteSighting.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
        await render();
        await open(2);

        deleteButton()!.click();
        await fixture.whenStable();

        expect(element.querySelector('.sighting-error')).toBeNull();
        expect(markers()).toHaveLength(3);
      });

      it('should not delete when the user is not allowed to', async () => {
        currentUser.set(otherUser);
        await render();
        const page = fixture.componentInstance as unknown as {
          deleteSighting(sighting: Sighting): void;
        };

        page.deleteSighting(trail[2]);

        expect(deleteSighting).not.toHaveBeenCalled();
      });
    });

    describe('without an account', () => {
      beforeEach(() => {
        currentUser.set(null);
      });

      it('should send "I saw it here" to the login page, which comes back to this page', async () => {
        await render();
        const link = element.querySelector<HTMLAnchorElement>('.location a.button');

        expect(link?.textContent?.trim()).toBe('I saw it here');
        expect(link?.getAttribute('href')).toBe('/login?returnUrl=%2Fannouncements%2F9');
      });

      it('should not start a sighting when the map is clicked', async () => {
        await render();
        await click(46.78, 23.63);

        expect(element.querySelector('.sighting-form')).toBeNull();
        expect(markers()).toHaveLength(1);
      });
    });

    describe('adding', () => {
      it('should tell a logged-in user to click on the map', async () => {
        await render();

        expect(element.querySelector('.location a.button')).toBeNull();
        expect(element.querySelectorAll('.trail-note')[1]?.textContent?.trim()).toBe(
          'Click on the map where you saw it.',
        );
      });

      it('should open the form with the time set to now and mark the place', async () => {
        await render();
        await click(46.78, 23.63);
        const seenAt = element.querySelector<HTMLInputElement>('#seen-at')!;

        expect(element.querySelector('.sighting-form h3')?.textContent).toBe('I saw it here');
        expect(Math.abs(new Date(seenAt.value).getTime() - Date.now())).toBeLessThan(120_000);
        expect(markers()).toHaveLength(2);
        expect(markers()[1].options.title).toBe('New sighting');
      });

      it('should not move the map when the temporary point appears', async () => {
        getSightings.mockReturnValue(of(trail));
        await render();
        leafletMap().setView([44, 26], 8);
        await click(46.78, 23.63);

        expect(leafletMap().getCenter().lat).toBeCloseTo(44, 4);
      });

      it('should forget the place when the user cancels', async () => {
        await render();
        await click(46.78, 23.63);

        element.querySelector<HTMLButtonElement>('.sighting-form .secondary')!.click();
        await fixture.whenStable();

        expect(element.querySelector('.sighting-form')).toBeNull();
        expect(markers()).toHaveLength(1);
      });

      it('should save the place, the date and the note and show the new point', async () => {
        await render();
        await click(46.78, 23.63);
        type('#seen-at', '2026-11-02T18:05');
        type('#sighting-note', '  Behind the church  ');

        await submit();

        expect(createSighting).toHaveBeenCalledWith(9, {
          latitude: 46.78,
          longitude: 23.63,
          seenAt: '2026-11-02T18:05:00',
          note: 'Behind the church',
        });
        expect(element.querySelector('.sighting-form')).toBeNull();
        expect(text('.sighting-success')).toBe('Thank you, the sighting was added.');
        expect(titles()).toEqual(['Mănăștur, Cluj-Napoca', 'Seen on 02 Nov 2026, 18:05']);
      });

      it('should not send a note that is empty', async () => {
        await render();
        await click(46.78, 23.63);
        type('#seen-at', '2026-11-02T18:05');
        type('#sighting-note', '   ');

        await submit();

        expect(createSighting.mock.calls[0][1]).not.toHaveProperty('note');
      });

      it('should zoom to include the new point', async () => {
        await render();
        await click(46.9, 23.9);
        type('#seen-at', '2026-11-02T18:05');
        await submit();

        expect(leafletMap().getBounds().contains([46.9, 23.9])).toBe(true);
      });

      it('should ask for the date', async () => {
        await render();
        await click(46.78, 23.63);
        type('#seen-at', '');

        await submit();

        expect(createSighting).not.toHaveBeenCalled();
        expect(text('.field-error')).toBe('Enter the date and the time.');
      });

      it('should not accept a date in the future', async () => {
        await render();
        await click(46.78, 23.63);
        type('#seen-at', '2099-01-01T10:00');

        await submit();

        expect(createSighting).not.toHaveBeenCalled();
        expect(text('.field-error')).toBe('The date cannot be in the future.');
      });

      it('should not accept a date before the pet was lost', async () => {
        await render();
        await click(46.78, 23.63);
        type('#seen-at', '2026-10-01T10:00');

        await submit();

        expect(createSighting).not.toHaveBeenCalled();
        expect(text('.field-error')).toBe('The date cannot be before the pet was lost.');
      });

      it('should not accept a note over 500 characters', async () => {
        await render();
        await click(46.78, 23.63);
        type('#sighting-note', 'x'.repeat(501));

        await submit();

        expect(createSighting).not.toHaveBeenCalled();
        expect(text('.field-error')).toBe('The note can have at most 500 characters.');
      });

      it('should show the messages of the backend with a full stop', async () => {
        createSighting.mockReturnValue(
          throwError(
            () =>
              new HttpErrorResponse({
                status: 400,
                error: {
                  fieldErrors: [
                    { field: 'seenAt', message: 'Seen at cannot be in the future' },
                    { field: 'note', message: 'Note is not valid.' },
                  ],
                },
              }),
          ),
        );
        await render();
        await click(46.78, 23.63);

        await submit();

        expect(text('.sighting-form [role=alert]')).toBe(
          'Seen at cannot be in the future. Note is not valid.',
        );
        expect(element.querySelector('.sighting-form')).not.toBeNull();
      });

      it('should show the message of the backend when it is not about a field', async () => {
        createSighting.mockReturnValue(
          throwError(
            () =>
              new HttpErrorResponse({
                status: 400,
                error: { message: 'Sightings can only be added to active lost pet announcements' },
              }),
          ),
        );
        await render();
        await click(46.78, 23.63);

        await submit();

        expect(text('.sighting-form [role=alert]')).toBe(
          'Sightings can only be added to active lost pet announcements.',
        );
      });

      it('should say so when saving fails for another reason', async () => {
        createSighting.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        await render();
        await click(46.78, 23.63);

        await submit();

        expect(text('.sighting-form [role=alert]')).toBe(
          'Could not save the sighting. Please try again.',
        );
        expect(markers()).toHaveLength(2);
      });

      it('should save only once while it is being saved', async () => {
        createSighting.mockReturnValue(new Subject<Sighting>());
        await render();
        await click(46.78, 23.63);

        await submit();
        await submit();
        await click(46.9, 23.9);

        expect(createSighting).toHaveBeenCalledTimes(1);
        expect(element.querySelector<HTMLButtonElement>('.sighting-form .button')!.disabled).toBe(
          true,
        );
      });

      it('should not save without a place', async () => {
        await render();
        const page = fixture.componentInstance as unknown as { saveSighting(): void };

        page.saveSighting();

        expect(createSighting).not.toHaveBeenCalled();
      });

      it('should hide the thank you message when the user deletes something', async () => {
        getSightings.mockReturnValue(of([sighting(1, '2026-11-01T09:30:00', { reporter: { id: 5, displayName: 'Test Owner' } })]));
        await render();
        await click(46.78, 23.63);
        await submit();
        expect(element.querySelector('.sighting-success')).not.toBeNull();

        markers()[1].openPopup();
        await fixture.whenStable();
        element.querySelector<HTMLButtonElement>('.leaflet-popup .button.danger')!.click();
        await fixture.whenStable();

        expect(element.querySelector('.sighting-success')).toBeNull();
      });
    });

    describe('when the pet cannot be reported any more', () => {
      it('should not ask for sightings of a resolved announcement, but still show the trail', async () => {
        getById.mockReturnValue(of({ ...announcement, status: 'RESOLVED' }));
        getSightings.mockReturnValue(of(trail));
        await render();
        await click(46.78, 23.63);

        expect(markers()).toHaveLength(4);
        expect(element.querySelector('.sighting-form')).toBeNull();
        expect(element.querySelectorAll('.trail-note')).toHaveLength(1);
      });

      it('should not send a visitor to the login page for a resolved announcement', async () => {
        currentUser.set(null);
        getById.mockReturnValue(of({ ...announcement, status: 'RESOLVED' }));
        await render();

        expect(element.querySelector('.location a.button')).toBeNull();
      });

      it('should not start a sighting on a found pet', async () => {
        getById.mockReturnValue(of({ ...announcement, type: 'FOUND' }));
        await render();
        await click(46.78, 23.63);

        expect(element.querySelector('.sighting-form')).toBeNull();
        expect(markers()).toHaveLength(1);
      });
    });
  });
});
