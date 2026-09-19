import { Component, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
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

const STATUS_OPTIONS: { value: AppointmentStatus | ''; label: string }[] = [
  { value: '', label: 'Todos los estados' },
  { value: 'CONFIRMED', label: 'Confirmada' },
  { value: 'COMPLETED', label: 'Completada' },
  { value: 'CANCELLED', label: 'Cancelada' },
  { value: 'NO_SHOW', label: 'No se presentó' }
];

@Component({
  selector: 'app-admin-appointments',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  templateUrl: './admin-appointments.component.html'
})
export class AdminAppointmentsComponent {
  private readonly appointmentsApi = inject(AppointmentsApiService);
  private readonly catalogApi = inject(CatalogApiService);
  private readonly barbersApi = inject(BarbersApiService);

  readonly loading = signal(true);
  readonly appointments = signal<Appointment[]>([]);
  readonly serviceNames = signal<Record<string, string | undefined>>({});
  readonly barberNames = signal<Record<string, string | undefined>>({});
  readonly statusFilter = signal<AppointmentStatus | ''>('');

  readonly statusOptions = STATUS_OPTIONS;

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    const status = this.statusFilter() || undefined;
    forkJoin({
      appointments: this.appointmentsApi.list(status),
      services: this.catalogApi.list(true),
      barbers: this.barbersApi.list(true)
    }).subscribe(({ appointments, services, barbers }) => {
      this.serviceNames.set(Object.fromEntries(services.map((s) => [s.id, s.name])));
      this.barberNames.set(Object.fromEntries(barbers.map((b) => [b.id, b.fullName])));
      this.appointments.set([...appointments].sort((a, b) => b.startAt.localeCompare(a.startAt)));
      this.loading.set(false);
    });
  }

  setStatusFilter(value: string): void {
    this.statusFilter.set(value as AppointmentStatus | '');
    this.reload();
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
