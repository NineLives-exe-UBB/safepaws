
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { MessageService } from '../../services/message.service';
import { Header } from './header';

@Component({ template: '' })
class Page {}

describe('Header', () => {
  let fixture: ComponentFixture<Header>;
  let compiled: HTMLElement;

  const loggedIn = signal(false);

  const currentUser = signal<{
    id: number;
    email: string;
    displayName: string;
  } | null>(null);

  const authMock = {
    isLoggedIn: loggedIn,
    user: currentUser,
    logout: () => {
      loggedIn.set(false);
      currentUser.set(null);
    },
  };

  const messageServiceMock = {
    getUnreadCount: () => of({ count: 0 }),
  };

  beforeEach(async () => {
    loggedIn.set(false);
    currentUser.set(null);

    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [
        provideRouter([
          { path: 'announcements/new', component: Page },
          { path: 'announcements', component: Page },
          { path: 'map', component: Page },
          { path: 'messages', component: Page },
          { path: 'login', component: Page },
          { path: 'register', component: Page },
        ]),
        {
          provide: AuthService,
          useValue: authMock,
        },
        {
          provide: MessageService,
          useValue: messageServiceMock,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    compiled = fixture.nativeElement as HTMLElement;
  });

  it('should show the SafePaws brand', () => {
    expect(compiled.querySelector('.brand')?.textContent).toContain('SafePaws');
  });

  it('should show login and register links when logged out', () => {
    const hrefs = Array.from(compiled.querySelectorAll('.nav a')).map((link) =>
      link.getAttribute('href'),
    );

    expect(hrefs).toEqual([
      '/announcements',
      '/map',
      '/login',
      '/register',
    ]);
  });

  it('should show the Messages link when logged in', () => {
    loggedIn.set(true);
    currentUser.set({
      id: 1,
      email: 'test@example.com',
      displayName: 'Test User',
    });

    fixture.detectChanges();

    const hrefs = Array.from(compiled.querySelectorAll('.nav a')).map((link) =>
      link.getAttribute('href'),
    );

    expect(hrefs).toContain('/messages');
    expect(hrefs).toContain('/announcements/new');
    expect(hrefs).not.toContain('/login');
    expect(hrefs).not.toContain('/register');
  });

  it('should show the post announcement link when logged in', () => {
    loggedIn.set(true);
    currentUser.set({
      id: 1,
      email: 'test@example.com',
      displayName: 'Test User',
    });

    fixture.detectChanges();

    expect(compiled.querySelector('.user-name')?.textContent).toContain(
      'Test User',
    );
  });

  it('should not show the unread counter when the count is zero', () => {
    loggedIn.set(true);
    currentUser.set({
      id: 1,
      email: 'test@example.com',
      displayName: 'Test User',
    });

    fixture.detectChanges();

    expect(compiled.querySelector('.unread-count')).toBeNull();
  });

  async function activeLinks(url: string): Promise<(string | null)[]> {
    await TestBed.inject(Router).navigateByUrl(url);
    await fixture.whenStable();
    fixture.detectChanges();

    return Array.from(compiled.querySelectorAll('.nav a.active')).map((link) =>
      link.getAttribute('href'),
    );
  }

  it('should highlight the list link on the list page', async () => {
    expect(await activeLinks('/announcements')).toEqual(['/announcements']);
  });

  it('should keep the list link highlighted when filters are in the URL', async () => {
    expect(await activeLinks('/announcements?species=CAT&color=grey')).toEqual([
      '/announcements',
    ]);
  });

  it('should highlight only the form link on the form page', async () => {
    loggedIn.set(true);
    currentUser.set({
      id: 1,
      email: 'test@example.com',
      displayName: 'Test User',
    });

    fixture.detectChanges();

    expect(await activeLinks('/announcements/new')).toEqual([
      '/announcements/new',
    ]);
  });

  it('should highlight only the map link on the map page', async () => {
    expect(await activeLinks('/map')).toEqual(['/map']);
  });

  it('should highlight the Messages link on the messages page', async () => {
    loggedIn.set(true);
    currentUser.set({
      id: 1,
      email: 'test@example.com',
      displayName: 'Test User',
    });

    fixture.detectChanges();

    expect(await activeLinks('/messages')).toEqual(['/messages']);
  });
});
