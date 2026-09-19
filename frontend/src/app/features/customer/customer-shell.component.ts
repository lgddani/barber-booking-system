import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-customer-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, MatButtonModule],
  template: `
    <header class="border-b border-line bg-surface px-6 py-4 flex items-center justify-between">
      <span class="font-display text-lg font-semibold text-ink">Bella Barba</span>
      <nav class="flex items-center gap-5">
        <a
          routerLink="reservar"
          routerLinkActive="text-clay font-semibold"
          [routerLinkActiveOptions]="{ exact: false }"
          class="text-sm text-ink-soft"
        >
          Reservar
        </a>
        <a routerLink="mis-citas" routerLinkActive="text-clay font-semibold" class="text-sm text-ink-soft">
          Mis citas
        </a>
        <button mat-stroked-button (click)="auth.logout()">Salir</button>
      </nav>
    </header>
    <router-outlet />
  `
})
export class CustomerShellComponent {
  readonly auth = inject(AuthService);
}
