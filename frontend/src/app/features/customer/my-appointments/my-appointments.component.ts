import { Component, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AppointmentsApiService } from '../../../core/api/appointments-api.service';
import { CatalogApiService } from '../../../core/api/catalog-api.service';
import { BarbersApiService } from '../../../core/api/barbers-api.service';
import { Appointment, AppointmentStatus } from '../../../core/models/appointment.model';

const STATUS_LABEL: Record<AppointmentStatus, string> = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmada',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
  NO_SHOW: 'No se presentó'
};

const CANCELLABLE: AppointmentStatus[] = ['PENDING', 'CONFIRMED'];

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './my-appointments.component.html'
})
export class MyAppointmentsComponent {
  private readonly appointmentsApi = inject(AppointmentsApiService);
  private readonly catalogApi = inject(CatalogApiService);
  private readonly barbersApi = inject(BarbersApiService);

  readonly loading = signal(true);
  readonly appointments = signal<Appointment[]>([]);
  // string | undefined (no solo string): así el ?? del template es real para
  // TypeScript, no un "nunca pasa" — sí puede faltar si la cita referencia un
  // servicio/barbero que ya no está activo.
  readonly serviceNames = signal<Record<string, string | undefined>>({});
  readonly barberNames = signal<Record<string, string | undefined>>({});
  readonly cancellingId = signal<string | null>(null);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    forkJoin({
      appointments: this.appointmentsApi.list(),
      services: this.catalogApi.list(),
      barbers: this.barbersApi.list()
    }).subscribe(({ appointments, services, barbers }) => {
      this.serviceNames.set(Object.fromEntries(services.map((s) => [s.id, s.name])));
      this.barberNames.set(Object.fromEntries(barbers.map((b) => [b.id, b.fullName])));
      this.appointments.set(
        [...appointments].sort((a, b) => b.startAt.localeCompare(a.startAt))
      );
      this.loading.set(false);
    });
  }

  cancel(appointment: Appointment): void {
    this.cancellingId.set(appointment.id);
    this.appointmentsApi.cancel(appointment.id).subscribe({
      next: () => {
        this.cancellingId.set(null);
        this.reload();
      },
      error: () => this.cancellingId.set(null)
    });
  }

  canCancel(appointment: Appointment): boolean {
    return CANCELLABLE.includes(appointment.status);
  }

  statusLabel(status: AppointmentStatus): string {
    return STATUS_LABEL[status];
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
}
