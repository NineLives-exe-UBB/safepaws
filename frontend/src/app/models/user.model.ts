export interface User {
  id: number;
  email: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  displayName: string;
}

/** The answer of POST /auth/login. */
export interface LoginResponse {
  token: string;
  /** Without a time zone, like every date of the API. */
  expiresAt: string;
  user: User;
}

/** What is kept in the browser while the user is logged in. */
export interface Session {
  token: string;
  /** When the token stops being valid, in milliseconds since 1970. */
  expiresAtMs: number;
  user: User;
}
