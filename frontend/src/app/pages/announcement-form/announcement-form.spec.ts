import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnnouncementForm } from './announcement-form';

describe('AnnouncementForm', () => {
  let fixture: ComponentFixture<AnnouncementForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnnouncementForm],
    }).compileComponents();

    fixture = TestBed.createComponent(AnnouncementForm);
    await fixture.whenStable();
  });

  it('should show the page title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Post announcement');
  });
});
