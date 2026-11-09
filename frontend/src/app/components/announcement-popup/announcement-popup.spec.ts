import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AnnouncementSummary } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';
import { AnnouncementPopup } from './announcement-popup';

describe('AnnouncementPopup', () => {
  const announcement: AnnouncementSummary = {
    id: 15,
    type: 'LOST',
    petName: 'Misi',
    species: 'CAT',
    area: 'Mănăștur, Cluj-Napoca',
    latitude: 46.7712,
    longitude: 23.6236,
    eventDate: '2026-10-30T18:00:00',
    status: 'ACTIVE',
    photos: [{ id: 4, url: '/photos/4', isMain: true }],
  };

  let fixture: ComponentFixture<AnnouncementPopup>;
  let element: HTMLElement;

  async function render(value: AnnouncementSummary): Promise<void> {
    fixture = TestBed.createComponent(AnnouncementPopup);
    fixture.componentRef.setInput('announcement', value);
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AnnouncementService,
          useValue: { mainPhotoUrl: (photos: unknown[]) => (photos.length ? '/main.jpg' : null) },
        },
      ],
    });
  });

  it('should show the main photo', async () => {
    await render(announcement);

    expect(element.querySelector('img')?.getAttribute('src')).toBe('/main.jpg');
  });

  it('should show no photo when there is none', async () => {
    await render({ ...announcement, photos: [] });

    expect(element.querySelector('img')).toBeNull();
  });

  it('should show the pet name when there is one', async () => {
    await render(announcement);

    expect(element.querySelector('.title')?.textContent).toContain('Misi');
  });

  it('should show the species when there is no pet name', async () => {
    await render({ ...announcement, petName: null, species: 'DOG' });

    expect(element.querySelector('.title')?.textContent).toContain('Dog');
  });

  it('should show the area and the date', async () => {
    await render(announcement);

    expect(element.querySelector('.area')?.textContent).toContain('Mănăștur, Cluj-Napoca');
    expect(element.querySelector('.date')?.textContent).toContain('30 Oct 2026, 18:00');
  });

  it('should link to the page of the announcement', async () => {
    await render(announcement);
    const link = element.querySelector('.details-link');

    expect(link?.textContent).toContain('View details');
    expect(link?.getAttribute('href')).toBe('/announcements/15');
  });
});
