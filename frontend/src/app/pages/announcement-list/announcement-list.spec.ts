import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnnouncementList } from './announcement-list';

describe('AnnouncementList', () => {
  let fixture: ComponentFixture<AnnouncementList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnnouncementList],
    }).compileComponents();

    fixture = TestBed.createComponent(AnnouncementList);
    await fixture.whenStable();
  });

  it('should show the page title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Announcements');
  });
});
