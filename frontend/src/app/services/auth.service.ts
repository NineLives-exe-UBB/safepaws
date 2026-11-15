import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, switchMap, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse, RegisterRequest, Session, User } from '../models/user.model';

export const LOGIN_ROUTE = '/login';
export const SESSION_STORAGE_KEY = 'safepaws.session';

/** setTimeout cannot wait longer than this; the token is valid for 24 hours, far below it. */
const MAX_TIMER_MS = 2_147_483_647;

/** Who is logged in. Everything that needs the user or the token asks this service. */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authUrl = `${environment.apiUrl}/auth`;

  private readonly session = signal<Session | null>(null);
  private expiryTimer?: ReturnType<typeof setTimeout>;

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);

  constructor() {
    const stored = this.readStoredSession();
    if (stored) {
      this.start(stored);
    }
  }

  login(request: LoginRequest): Observable<User> {
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, request).pipe(
      tap((response) => this.start(this.toSession(response))),
      map((response) => response.user),
    );
  }

  /** Creates the account and logs in with it, because registering does not log the user in. */
  register(request: RegisterRequest): Observable<User> {
    return this.http
      .post<User>(`${this.authUrl}/register`, request)
      .pipe(switchMap(() => this.login({ email: request.email, password: request.password })));
  }

  /** The token can not be revoked by the backend, so logging out only forgets it here. */
  logout(): void {
    this.stop();
  }

  /** The token for the Authorization header, or null when nobody is logged in. */
  token(): string | null {
    return this.session()?.token ?? null;
  }

  /**
   * The backend refused the token, or it ran out: forget it and send the user to the login page,
   * with the page they were on, so the login can bring them back.
   */
  sessionExpired(): void {
    this.stop();
    const returnUrl = this.router.url;
    if (!returnUrl.startsWith(LOGIN_ROUTE)) {
      this.router.navigate([LOGIN_ROUTE], { queryParams: { returnUrl } });
    }
  }

  private start(session: Session): void {
    this.stop();
    this.session.set(session);
    this.storeSession(session);
    this.expiryTimer = setTimeout(
      () => this.sessionExpired(),
      Math.min(Math.max(session.expiresAtMs - Date.now(), 0), MAX_TIMER_MS),
    );
  }

  private stop(): void {
    clearTimeout(this.expiryTimer);
    this.expiryTimer = undefined;
    this.session.set(null);
    this.storeSession(null);
  }

  private toSession(response: LoginResponse): Session {
    return {
      token: response.token,
      expiresAtMs: expiryOf(response.token, response.expiresAt),
      user: response.user,
    };
  }

  private readStoredSession(): Session | null {
    try {
      const stored: unknown = JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY) ?? 'null');
      if (isSession(stored) && stored.expiresAtMs > Date.now()) {
        return stored;
      }
      this.storeSession(null);
    } catch {
      this.storeSession(null);
    }
    return null;
  }

  /** The browser can refuse storage (private windows, blocked site data); the login still works. */
  private storeSession(session: Session | null): void {
    try {
      if (session) {
        localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
      } else {
        localStorage.removeItem(SESSION_STORAGE_KEY);
      }
    } catch {
      // nothing to do: the user stays logged in until the page is closed
    }
  }
}

/**
 * The expiry in the token is a UTC instant. The `expiresAt` of the answer has no time zone, so it
 * is only used when the token cannot be read.
 */
export function expiryOf(token: string, expiresAt: string): number {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (typeof payload.exp === 'number') {
      return payload.exp * 1000;
    }
  } catch {
    // not a readable JWT: use expiresAt
  }
  return Date.parse(expiresAt);
}

function isSession(value: unknown): value is Session {
  const session = value as Session | null;
  return (
    typeof session?.token === 'string' &&
    typeof session.expiresAtMs === 'number' &&
    typeof session.user?.id === 'number' &&
    typeof session.user.email === 'string' &&
    typeof session.user.displayName === 'string'
  );
}
