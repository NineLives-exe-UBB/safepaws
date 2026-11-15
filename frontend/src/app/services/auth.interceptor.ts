import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

/**
 * Sends the token with every request to the backend, and logs the user out when the backend says
 * the token is not valid (any more).
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.token();

  if (!token || !request.url.startsWith(environment.apiUrl)) {
    return next(request);
  }

  const withToken = request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  return next(withToken).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        auth.sessionExpired();
      }
      return throwError(() => error);
    }),
  );
};
