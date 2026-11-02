import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Header } from './header';

describe('Header', () => {
  let fixture: ComponentFixture<Header>;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    await fixture.whenStable();
    compiled = fixture.nativeElement as HTMLElement;
  });

  it('should show the SafePaws brand', () => {
    expect(compiled.querySelector('.brand')?.textContent).toContain('SafePaws');
  });

  it('should link to both pages', () => {
    const hrefs = Array.from(compiled.querySelectorAll('.nav a')).map((link) =>
      link.getAttribute('href'),
    );
    expect(hrefs).toEqual(['/announcements', '/announcements/new']);
  });
});
