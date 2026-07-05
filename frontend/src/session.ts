import type { AuthResponse } from "./types";

export const SESSION_KEY = "jingda-private-user";
export const LAST_ACTIVITY_KEY = "jingda-private-last-activity";
export const SESSION_TIMEOUT_MS = 30 * 60 * 1000;

const LEGACY_SESSION_KEY = "learning-app-user";
const LEGACY_LAST_ACTIVITY_KEY = "learning-app-last-activity";

export function readStoredUser(): AuthResponse | null {
  if (isSessionExpired()) {
    clearStoredSession();
    return null;
  }

  const value = localStorage.getItem(SESSION_KEY) ?? localStorage.getItem(LEGACY_SESSION_KEY);
  if (!value) return null;
  try {
    const user = JSON.parse(value) as AuthResponse;
    if (!user.id || !user.sessionToken) {
      clearStoredSession();
      return null;
    }
    if (!localStorage.getItem(LAST_ACTIVITY_KEY)) {
      touchSession();
    }
    return user;
  } catch {
    clearStoredSession();
    return null;
  }
}

export function writeStoredUser(user: AuthResponse): void {
  localStorage.setItem(SESSION_KEY, JSON.stringify(user));
  localStorage.removeItem(LEGACY_SESSION_KEY);
  touchSession();
}

export function clearStoredSession(): void {
  localStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(LAST_ACTIVITY_KEY);
  localStorage.removeItem(LEGACY_SESSION_KEY);
  localStorage.removeItem(LEGACY_LAST_ACTIVITY_KEY);
}

export function touchSession(): void {
  localStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()));
  localStorage.removeItem(LEGACY_LAST_ACTIVITY_KEY);
}

export function isSessionExpired(): boolean {
  const value = localStorage.getItem(LAST_ACTIVITY_KEY) ?? localStorage.getItem(LEGACY_LAST_ACTIVITY_KEY);
  if (!value) return false;
  const lastActivityAt = Number(value);
  return !Number.isFinite(lastActivityAt) || Date.now() - lastActivityAt >= SESSION_TIMEOUT_MS;
}
