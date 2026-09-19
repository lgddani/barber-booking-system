import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, of, switchMap, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Role, User } from '../models/user.model';

const TOKEN_KEY = 'bb_token';

interface LoginPayload {
  email: string;
  password: string;
}

interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
}

interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: Role;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly userSignal = signal<User | null>(null);
  private readonly readySignal = signal(false);

  readonly token = this.tokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly ready = this.readySignal.asReadonly();
  readonly isAuthenticated = computed(() => this.userSignal() !== null);
  readonly role = computed<Role | null>(() => this.userSignal()?.role ?? null);

  login(payload: LoginPayload): Observable<User | null> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, payload).pipe(
      tap((res) => this.storeToken(res.token)),
      switchMap(() => this.fetchCurrentUser())
    );
  }

  register(payload: RegisterPayload): Observable<User | null> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload).pipe(
      tap((res) => this.storeToken(res.token)),
      switchMap(() => this.fetchCurrentUser())
    );
  }

  // Se llama una vez al arrancar la app (ver provideAppInitializer en
  // app.config.ts): si había un token guardado de una sesión anterior, lo
  // valida contra /users/me antes de dejar entrar a ninguna ruta protegida.
  restoreSession(): Observable<User | null> {
    if (!this.tokenSignal()) {
      this.readySignal.set(true);
      return of(null);
    }
    return this.fetchCurrentUser();
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    this.router.navigateByUrl('/login');
  }

  private storeToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.tokenSignal.set(token);
  }

  private fetchCurrentUser(): Observable<User | null> {
    return this.http.get<User>(`${environment.apiUrl}/users/me`).pipe(
      tap((user) => {
        this.userSignal.set(user);
        this.readySignal.set(true);
      }),
      catchError(() => {
        // Token vencido o inválido: se limpia la sesión en vez de dejarla a
        // medias (con token pero sin usuario).
        localStorage.removeItem(TOKEN_KEY);
        this.tokenSignal.set(null);
        this.userSignal.set(null);
        this.readySignal.set(true);
        return of(null);
      })
    );
  }
}
