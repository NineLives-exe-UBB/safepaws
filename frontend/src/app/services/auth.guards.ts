import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, LOGIN_ROUTE } from './auth.service';

/** Pages that need an account: a guest goes to the login page, which brings them back after. */
export const loggedInGuard: CanActivateFn = (_route, state) => {
  if (inject(AuthService).isLoggedIn()) {
    return true;
  }
  return inject(Router).createUrlTree([LOGIN_ROUTE], { queryParams: { returnUrl: state.url } });
};

/** The login and register pages: a user who is already logged in goes to the announcements. */
export const guestGuard: CanActivateFn = () => {
  if (!inject(AuthService).isLoggedIn()) {
    return true;
  }
  return inject(Router).createUrlTree(['/announcements']);
};
