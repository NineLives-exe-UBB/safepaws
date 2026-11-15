import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { fakeToken } from './auth.testing';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  const apiUrl = `${environment.apiUrl}/announcements`;
  let http: HttpClient;
  let httpController: HttpTestingController;
  let auth: { token: ReturnType<typeof vi.fn>; sessionExpired: ReturnType<typeof vi.fn> };

  function setup(token: string | null): void {
    auth = { token: vi.fn().mockReturnValue(token), sessionExpired: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpController = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    httpController.verify();
  });

  it('should send the token to the backend when somebody is logged in', () => {
    setup('the-token');
    http.get(apiUrl).subscribe();

    const req = httpController.expectOne(apiUrl);
    expect(req.request.headers.get('Authorization')).toBe('Bearer the-token');
    req.flush([]);
  });

  it('should send the token with every kind of request', () => {
    setup('the-token');
    http.post(apiUrl, {}).subscribe();
    http.patch(`${apiUrl}/1/status`, {}).subscribe();

    const requests = httpController.match(() => true);
    expect(requests.map((r) => r.request.headers.get('Authorization'))).toEqual([
      'Bearer the-token',
      'Bearer the-token',
    ]);
    requests.forEach((r) => r.flush({}));
  });

  it('should send no Authorization header when nobody is logged in', () => {
    setup(null);
    http.get(apiUrl).subscribe();

    const req = httpController.expectOne(apiUrl);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('should never send the token to another site', () => {
    setup('the-token');
    http.get('https://tile.openstreetmap.org/1/2/3.png').subscribe();

    const req = httpController.expectOne('https://tile.openstreetmap.org/1/2/3.png');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush('');
  });

  it('should log the user out when the backend says the token is not valid', () => {
    setup('the-token');
    let status = 0;
    http.get(apiUrl).subscribe({ error: (error) => (status = error.status) });
    httpController.expectOne(apiUrl).flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(auth.sessionExpired).toHaveBeenCalledTimes(1);
    expect(status).toBe(401);
  });

  it('should not log the user out for a 401 when no token was sent', () => {
    setup(null);
    http.post(`${environment.apiUrl}/auth/login`, {}).subscribe({ error: () => undefined });
    httpController
      .expectOne(`${environment.apiUrl}/auth/login`)
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(auth.sessionExpired).not.toHaveBeenCalled();
  });

  it.each([400, 403, 404, 500])('should keep the user logged in after a %i', (status) => {
    setup('the-token');
    http.get(apiUrl).subscribe({ error: () => undefined });
    httpController.expectOne(apiUrl).flush({}, { status, statusText: 'Error' });

    expect(auth.sessionExpired).not.toHaveBeenCalled();
  });
});

describe('authInterceptor with the real AuthService', () => {
  const authUrl = `${environment.apiUrl}/auth`;
  const announcementsUrl = `${environment.apiUrl}/announcements`;
  let auth: AuthService;
  let http: HttpClient;
  let httpController: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpClient);
    httpController = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    localStorage.clear();
  });

  function logIn(): string {
    const token = fakeToken(Date.now() + 24 * 60 * 60 * 1000);
    auth.login({ email: 'ana@safepaws.dev', password: 'SafePaws1!' }).subscribe();
    httpController.expectOne(`${authUrl}/login`).flush({
      token,
      expiresAt: '2026-11-16T14:30:00',
      user: { id: 1, email: 'ana@safepaws.dev', displayName: 'Ana Pop' },
    });
    return token;
  }

  it('should send the token of the login with the next request, and none after the logout', () => {
    const token = logIn();
    http.post(announcementsUrl, {}).subscribe();
    const withToken = httpController.expectOne(announcementsUrl);
    expect(withToken.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    withToken.flush({});

    auth.logout();
    http.post(announcementsUrl, {}).subscribe();
    const withoutToken = httpController.expectOne(announcementsUrl);
    expect(withoutToken.request.headers.has('Authorization')).toBe(false);
    withoutToken.flush({});
  });

  it('should not send the token with the login itself', () => {
    auth.login({ email: 'ana@safepaws.dev', password: 'SafePaws1!' }).subscribe();

    const req = httpController.expectOne(`${authUrl}/login`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should log out when the backend refuses the token', () => {
    logIn();
    http.post(announcementsUrl, {}).subscribe({ error: () => undefined });
    httpController
      .expectOne(announcementsUrl)
      .flush(
        { message: 'Authentication is required' },
        { status: 401, statusText: 'Unauthorized' },
      );

    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.token()).toBeNull();
  });

  it('should keep the user logged in after a 403', () => {
    logIn();
    http.post(announcementsUrl, {}).subscribe({ error: () => undefined });
    httpController
      .expectOne(announcementsUrl)
      .flush(
        { message: 'You can only change your own announcements' },
        { status: 403, statusText: 'Forbidden' },
      );

    expect(auth.isLoggedIn()).toBe(true);
  });
});
