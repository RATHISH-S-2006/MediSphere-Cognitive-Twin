import { Injectable, signal } from '@angular/core';

const TOKEN_KEY = 'medisphere.dev.token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenState = signal(localStorage.getItem(TOKEN_KEY) ?? 'ADMIN-admin-1');
  readonly token = this.tokenState.asReadonly();

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.tokenState.set(token);
  }

  clearToken(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.tokenState.set('');
  }

  isAuthenticated(): boolean {
    return this.tokenState().length > 0;
  }
}
