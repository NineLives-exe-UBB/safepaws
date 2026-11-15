import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { LoginResponse, Session, User } from '../models/user.model';
import { AuthService, expiryOf, LOGIN_ROUTE, SESSION_STORAGE_KEY } from './auth.service';
import { fakeToken } from './auth.testing';

@Component({ template: '' })
class Page {}

describe('AuthService', () => {
  const user: User = { id: 1, email: 'ana@safepaws.dev', displayName: 'Ana Pop' };
  const credentials = { email: 'ana@safepaws.dev', password: 'SafePaws1!' };
  const oneDayMs = 24 * 60 * 60 * 1000;
  const authUrl = `${environment.apiUrl}/auth`;

  let service: AuthService;
  let httpController: HttpTestingController;
  let router: Router;

  function loginResponse(expiresAtMs = Date.now() + oneDayMs): LoginResponse {
    return { token: fakeToken(expiresAtMs), expiresAt: '2026-11-16T14:30:00', user };
  }

  function storeSession(overrides: Partial<Session> = {}): Session {
    const session: Session = {
      token: fakeToken(Date.now() + oneDayMs),
      expiresAtMs: Date.now() + oneDayMs,
      user,
      ...overrides,
    };
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    return session;
  }

  function createService(): void {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'login', component: Page },
          { path: 'announcements', component: Page },
        ]),
      ],
    });
    service = TestBed.inject(AuthService);
    httpController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  }

  function logIn(response = loginResponse()): void {
    service.login(credentials).subscribe();
    httpController.expectOne(`${authUrl}/login`).flush(response);
  }

  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    vi.useRealTimers();
    localStorage.clear();
  });

  it('should start with nobody logged in', () => {
    createService();

    expect(service.user()).toBeNull();
    expect(service.isLoggedIn()).toBe(false);
    expect(service.token()).toBeNull();
  });

  describe('login', () => {
    beforeEach(createService);

    it('should send the email and the password', () => {
      service.login(credentials).subscribe();

      const req = httpController.expectOne(`${authUrl}/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);
      req.flush(loginResponse());
    });

    it('should remember the user and the token', () => {
      const response = loginResponse();
      let result: User | undefined;
      service.login(credentials).subscribe((loggedIn) => (result = loggedIn));
      httpController.expectOne(`${authUrl}/login`).flush(response);

      expect(result).toEqual(user);
      expect(service.user()).toEqual(user);
      expect(service.isLoggedIn()).toBe(true);
      expect(service.token()).toBe(response.token);
    });

    it('should keep the session in the browser', () => {
      const response = loginResponse();
      logIn(response);

      const stored = JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY) ?? 'null');
      expect(stored.token).toBe(response.token);
      expect(stored.user).toEqual(user);
    });

    it('should stay logged out and pass on the error when the login is refused', () => {
      let status = 0;
      service.login(credentials).subscribe({ error: (error) => (status = error.status) });
      httpController
        .expectOne(`${authUrl}/login`)
        .flush(
          { status: 401, message: 'Invalid email or password' },
          { status: 401, statusText: 'Unauthorized' },
        );

      expect(status).toBe(401);
      expect(service.isLoggedIn()).toBe(false);
      expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
    });

    it('should replace the user of an earlier login', () => {
      logIn();
      const other: User = { id: 2, email: 'mihai@safepaws.dev', displayName: 'Mihai Rus' };
      service.login(credentials).subscribe();
      httpController.expectOne(`${authUrl}/login`).flush({ ...loginResponse(), user: other });

      expect(service.user()).toEqual(other);
    });
  });

  describe('register', () => {
    const registration = { ...credentials, displayName: 'Ana Pop' };

    beforeEach(createService);

    it('should create the account and then log in with it', () => {
      let result: User | undefined;
      service.register(registration).subscribe((registered) => (result = registered));

      const register = httpController.expectOne(`${authUrl}/register`);
      expect(register.request.method).toBe('POST');
      expect(register.request.body).toEqual(registration);
      register.flush(user, { status: 201, statusText: 'Created' });

      const login = httpController.expectOne(`${authUrl}/login`);
      expect(login.request.body).toEqual(credentials);
      login.flush(loginResponse());

      expect(result).toEqual(user);
      expect(service.user()).toEqual(user);
    });

    it('should not log in when the email is already registered', () => {
      let status = 0;
      service.register(registration).subscribe({ error: (error) => (status = error.status) });
      httpController
        .expectOne(`${authUrl}/register`)
        .flush(
          { status: 409, message: 'Email is already registered' },
          { status: 409, statusText: 'Conflict' },
        );

      httpController.expectNone(`${authUrl}/login`);
      expect(status).toBe(409);
      expect(service.isLoggedIn()).toBe(false);
    });

    it('should pass on the validation errors of the backend', () => {
      let fieldErrors: unknown;
      service
        .register(registration)
        .subscribe({ error: (error) => (fieldErrors = error.error.fieldErrors) });
      const errors = [{ field: 'password', message: 'Password is required' }];
      httpController
        .expectOne(`${authUrl}/register`)
        .flush({ fieldErrors: errors }, { status: 400, statusText: 'Bad Request' });

      expect(fieldErrors).toEqual(errors);
    });
  });

  describe('logout', () => {
    beforeEach(createService);

    it('should forget the user, the token and the stored session', () => {
      logIn();
      service.logout();

      expect(service.user()).toBeNull();
      expect(service.isLoggedIn()).toBe(false);
      expect(service.token()).toBeNull();
      expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
    });

    it('should be fine when nobody is logged in', () => {
      expect(() => service.logout()).not.toThrow();
    });
  });

  describe('after a page refresh', () => {
    it('should still be logged in', () => {
      const session = storeSession();
      createService();

      expect(service.user()).toEqual(user);
      expect(service.token()).toBe(session.token);
    });

    it('should forget a session that has expired', () => {
      storeSession({ expiresAtMs: Date.now() - 1000 });
      createService();

      expect(service.isLoggedIn()).toBe(false);
      expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
    });

    it('should ignore stored data that is not a session', () => {
      localStorage.setItem(SESSION_STORAGE_KEY, '{"token":1}');
      createService();

      expect(service.isLoggedIn()).toBe(false);
      expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
    });

    it('should ignore stored data that is not JSON', () => {
      localStorage.setItem(SESSION_STORAGE_KEY, 'not json');
      createService();

      expect(service.isLoggedIn()).toBe(false);
    });

    it('should still work when the browser refuses storage', () => {
      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('quota');
      });
      createService();
      logIn();

      expect(service.isLoggedIn()).toBe(true);
    });
  });

  describe('when the token expires', () => {
    it('should log out and send the user to the login page when the time is up', async () => {
      createService();
      await router.navigateByUrl('/announcements');
      vi.useFakeTimers();
      // a token expires at a whole second
      vi.setSystemTime(Date.UTC(2026, 10, 15, 12, 0, 0));
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
      logIn(loginResponse(Date.now() + 5000));

      vi.advanceTimersByTime(4999);
      expect(service.isLoggedIn()).toBe(true);

      vi.advanceTimersByTime(2);
      expect(service.isLoggedIn()).toBe(false);
      expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
      expect(navigate).toHaveBeenCalledWith([LOGIN_ROUTE], {
        queryParams: { returnUrl: '/announcements' },
      });
    });

    it('should not wait for a token that is not valid any more', async () => {
      createService();
      vi.useFakeTimers();
      logIn(loginResponse(Date.now() - 1000));
      vi.advanceTimersByTime(1);

      expect(service.isLoggedIn()).toBe(false);
    });

    it('should not log out later because of an earlier login that was replaced', () => {
      createService();
      vi.useFakeTimers();
      logIn(loginResponse(Date.now() + 1000));
      logIn(loginResponse(Date.now() + oneDayMs));
      vi.advanceTimersByTime(2000);

      expect(service.isLoggedIn()).toBe(true);
    });

    it('should not wait any more after a logout', () => {
      createService();
      vi.useFakeTimers();
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
      logIn(loginResponse(Date.now() + 1000));
      service.logout();
      vi.advanceTimersByTime(2000);

      expect(navigate).not.toHaveBeenCalled();
    });

    it('should send the user to the login page when the backend refuses the token', async () => {
      createService();
      logIn();
      await router.navigateByUrl('/announcements');
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
      service.sessionExpired();

      expect(service.isLoggedIn()).toBe(false);
      expect(navigate).toHaveBeenCalledWith([LOGIN_ROUTE], {
        queryParams: { returnUrl: '/announcements' },
      });
    });

    it('should not send the user to the login page when already there', async () => {
      createService();
      await router.navigateByUrl('/login?returnUrl=%2Fannouncements');
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
      service.sessionExpired();

      expect(navigate).not.toHaveBeenCalled();
    });
  });

  describe('expiryOf', () => {
    it('should read the expiry of the token', () => {
      const expiresAtMs = Date.UTC(2026, 10, 16, 14, 30);

      expect(expiryOf(fakeToken(expiresAtMs), '2000-01-01T00:00:00')).toBe(expiresAtMs);
    });

    it('should use expiresAt when the token cannot be read', () => {
      expect(expiryOf('not-a-token', '2026-11-16T14:30:00')).toBe(
        Date.parse('2026-11-16T14:30:00'),
      );
    });

    it('should use expiresAt when the token has no expiry', () => {
      const token = `${btoa('{}')}.${btoa('{"sub":"1"}')}.x`;

      expect(expiryOf(token, '2026-11-16T14:30:00')).toBe(Date.parse('2026-11-16T14:30:00'));
    });
  });
});
