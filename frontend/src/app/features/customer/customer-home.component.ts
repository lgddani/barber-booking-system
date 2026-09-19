import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-customer-home',
  standalone: true,
  imports: [MatButtonModule],
  template: `
    <main class="min-h-screen flex items-center justify-center px-6">
      <div class="max-w-md w-full bg-surface border border-line rounded-2xl p-8 text-center">
        <h1 class="font-display text-2xl font-semibold text-ink m-0">Hola, {{ auth.user()?.fullName }}</h1>
        <p class="text-ink-soft mt-2">Panel de cliente — las pantallas reales llegan en la próxima fase.</p>
        <button mat-stroked-button class="mt-6" (click)="auth.logout()">Cerrar sesión</button>
      </div>
    </main>
  `
})
export class CustomerHomeComponent {
  readonly auth = inject(AuthService);
}
