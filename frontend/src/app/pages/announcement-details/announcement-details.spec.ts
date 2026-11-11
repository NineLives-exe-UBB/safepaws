import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import type { Mock } from 'vitest';
import { Announcement } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';
import { AnnouncementDetails } from './announcement-details';

describe('AnnouncementDetails', () => {
  const announcement: Announcement = {
    id: 9,
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

  let getById: Mock<(id: number) => Observable<Announcement>>;
  let updateStatus: Mock<(id: number, status: string) => Observable<Announcement>>;
  let fixture: ComponentFixture<AnnouncementDetails>;
  let element: HTMLElement;

  async function render(id: string | null = '9'): Promise<void> {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(id === null ? {} : { id }) } },
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

  const text = (selector: string) => element.querySelector(selector)?.textContent?.trim();

  beforeEach(() => {
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

    it('should put the name of the pet in the browser tab', async () => {
      await render();

      expect(TestBed.inject(Title).getTitle()).toBe('Misi | SafePaws');
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
});
