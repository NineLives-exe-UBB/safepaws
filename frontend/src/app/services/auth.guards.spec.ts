import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { guestGuard, loggedInGuard } from './auth.guards';
import { AuthService } from './auth.service';

describe('auth guards', () => {
  function setUp(loggedIn: boolean): void {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isLoggedIn: () => loggedIn } },
      ],
    });
  }

  function run(guard: typeof loggedInGuard, url: string): boolean | UrlTree {
    return TestBed.runInInjectionContext(
      () =>
        guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot) as boolean | UrlTree,
    );
  }

  function serialize(result: boolean | UrlTree): string | boolean {
    return result instanceof UrlTree ? TestBed.inject(Router).serializeUrl(result) : result;
  }

  describe('loggedInGuard', () => {
    it('lets a logged-in user open the page', () => {
      setUp(true);

      expect(run(loggedInGuard, '/announcements/new')).toBe(true);
    });

    it('sends a guest to the login page with the page as returnUrl', () => {
      setUp(false);

      expect(serialize(run(loggedInGuard, '/announcements/new'))).toBe(
        '/login?returnUrl=%2Fannouncements%2Fnew',
      );
    });
  });

  describe('guestGuard', () => {
    it('lets a guest open the page', () => {
      setUp(false);

      expect(run(guestGuard, '/login')).toBe(true);
    });

    it('sends a logged-in user to the announcements', () => {
      setUp(true);

      expect(serialize(run(guestGuard, '/login'))).toBe('/announcements');
    });
  });
});
