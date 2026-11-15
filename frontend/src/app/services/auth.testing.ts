/** A token that looks like the ones of the backend (header.payload.signature), for the tests. */
export function fakeToken(expiresAtMs: number): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'HS256' })}.${encode({ sub: '1', exp: Math.floor(expiresAtMs / 1000) })}.signature`;
}
