import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { Announcement } from '../../models/announcement.model';
import { AnnouncementCard } from './announcement-card';

describe('AnnouncementCard', () => {
  const announcement: Announcement = {
    id: 15,
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
    status: 'ACTIVE',
    createdAt: '2026-11-01T14:30:00',
    photos: [
      { id: 3, url: '/photos/3', isMain: false },
      { id: 4, url: '/photos/4', isMain: true },
    ],
  };

  let fixture: ComponentFixture<AnnouncementCard>;

  async function render(value: Announcement): Promise<HTMLElement> {
    fixture = TestBed.createComponent(AnnouncementCard);
    fixture.componentRef.setInput('announcement', value);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnnouncementCard],
      providers: [provideHttpClient()],
    }).compileComponents();
  });

  it('should show the species, area and date', async () => {
    const compiled = await render(announcement);

    expect(compiled.querySelector('.species')?.textContent).toContain('Cat');
    expect(compiled.querySelector('.area')?.textContent).toContain('Mănăștur, Cluj-Napoca');
    expect(compiled.querySelector('.date')?.textContent).toContain('30 Oct 2026, 18:00');
  });

  it('should show the LOST label', async () => {
    const compiled = await render(announcement);
    const label = compiled.querySelector('.label');

    expect(label?.textContent).toContain('LOST');
    expect(label?.classList).toContain('lost');
  });

  it('should show the FOUND label', async () => {
    const compiled = await render({ ...announcement, type: 'FOUND' });
    const label = compiled.querySelector('.label');

    expect(label?.textContent).toContain('FOUND');
    expect(label?.classList).toContain('found');
  });

  it('should show the main photo', async () => {
    const compiled = await render(announcement);

    expect(compiled.querySelector('img')?.getAttribute('src')).toBe(
      `${environment.apiUrl}/photos/4`,
    );
  });

  it('should show the first photo when none is marked as main', async () => {
    const compiled = await render({
      ...announcement,
      photos: [{ id: 3, url: '/photos/3', isMain: false }],
    });

    expect(compiled.querySelector('img')?.getAttribute('src')).toBe(
      `${environment.apiUrl}/photos/3`,
    );
  });

  it('should show a placeholder when there are no photos', async () => {
    const compiled = await render({ ...announcement, photos: [] });

    expect(compiled.querySelector('img')).toBeNull();
    expect(compiled.querySelector('.no-photo')).toBeTruthy();
  });

  it('should link the poster button to the poster endpoint', async () => {
    const compiled = await render(announcement);
    const button = compiled.querySelector('.poster-button');

    expect(button?.textContent).toContain('Download poster');
    expect(button?.getAttribute('href')).toBe(`${environment.apiUrl}/announcements/15/poster`);
  });
});
