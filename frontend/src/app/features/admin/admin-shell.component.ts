import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { AppointmentsApiService } from '../../core/api/appointments-api.service';
import { Appointment } from '../../core/models/appointment.model';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './admin-shell.component.html'
})
export class AdminShellComponent {
  readonly auth = inject(AuthService);
  private readonly appointmentsApi = inject(AppointmentsApiService);
  private readonly router = inject(Router);

  readonly notificationsOpen = signal(false);
  readonly profileOpen = signal(false);
  readonly loadingActivity = signal(false);
  readonly recentAppointments = signal<Appointment[]>([]);
  private activityLoaded = false;

  readonly initial = computed(() => (this.auth.user()?.fullName ?? '?').charAt(0).toUpperCase());

  toggleNotifications(): void {
    this.profileOpen.set(false);
    this.notificationsOpen.update((open) => !open);
    if (this.notificationsOpen() && !this.activityLoaded) {
      this.loadActivity();
    }
  }

  toggleProfile(): void {
    this.notificationsOpen.set(false);
    this.profileOpen.update((open) => !open);
  }

  closeDropdowns(): void {
    this.notificationsOpen.set(false);
    this.profileOpen.set(false);
  }

  goToAppointments(): void {
    this.closeDropdowns();
    this.router.navigateByUrl('/admin/citas');
  }

  formatWhen(startAt: string): string {
    const date = new Date(startAt);
    const formatted = new Intl.DateTimeFormat('es', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }

  private loadActivity(): void {
    this.loadingActivity.set(true);
    // "Simple": las confirmadas mas recientemente reservadas (createdAt), sin
    // concepto real de leido/no-leido — eso necesitaria guardar por admin
    // que citas ya vio, que quedo fuera de esta pasada a proposito.
    this.appointmentsApi.list('CONFIRMED').subscribe({
      next: (appointments) => {
        this.recentAppointments.set(
          [...appointments].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 5)
        );
        this.loadingActivity.set(false);
        this.activityLoaded = true;
      },
      error: () => this.loadingActivity.set(false)
    });
  }
}
