import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Header } from './header';

@Component({ template: '' })
class Page {}

describe('Header', () => {
  let fixture: ComponentFixture<Header>;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [
        provideRouter([
          { path: 'announcements', component: Page },
          { path: 'announcements/new', component: Page },
          { path: 'map', component: Page },
        ]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    await fixture.whenStable();
    compiled = fixture.nativeElement as HTMLElement;
  });

  it('should show the SafePaws brand', () => {
    expect(compiled.querySelector('.brand')?.textContent).toContain('SafePaws');
  });

  it('should link to every page', () => {
    const hrefs = Array.from(compiled.querySelectorAll('.nav a')).map((link) =>
      link.getAttribute('href'),
    );
    expect(hrefs).toEqual(['/announcements', '/map', '/announcements/new']);
  });

  async function activeLinks(url: string): Promise<(string | null)[]> {
    await TestBed.inject(Router).navigateByUrl(url);
    await fixture.whenStable();
    return Array.from(compiled.querySelectorAll('.nav a.active')).map((link) =>
      link.getAttribute('href'),
    );
  }

  it('should highlight the list link on the list page', async () => {
    expect(await activeLinks('/announcements')).toEqual(['/announcements']);
  });

  it('should keep the list link highlighted when filters are in the URL', async () => {
    expect(await activeLinks('/announcements?species=CAT&color=grey')).toEqual(['/announcements']);
  });

  it('should highlight only the form link on the form page', async () => {
    expect(await activeLinks('/announcements/new')).toEqual(['/announcements/new']);
  });

  it('should highlight only the map link on the map page', async () => {
    expect(await activeLinks('/map')).toEqual(['/map']);
  });
});
