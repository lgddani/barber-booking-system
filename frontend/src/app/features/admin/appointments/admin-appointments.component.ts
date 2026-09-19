import { Component, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
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

const PAGE_SIZE = 8;

@Component({
  selector: 'app-admin-appointments',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
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
  readonly searchTerm = signal('');
  readonly currentPage = signal(1);
  readonly pageSize = PAGE_SIZE;

  readonly statusOptions = STATUS_OPTIONS;

  // Busca por cliente, servicio o barbero a la vez — cualquiera de los tres
  // que coincida cuenta, para que "buscar" se sienta como buscar en toda la
  // fila visible, no solo en el nombre del cliente.
  readonly filteredAppointments = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const all = this.appointments();
    if (!term) {
      return all;
    }
    const services = this.serviceNames();
    const barbers = this.barberNames();
    return all.filter((a) => {
      const haystack = [a.customerName, services[a.serviceId], barbers[a.barberId]]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return haystack.includes(term);
    });
  });

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredAppointments().length / this.pageSize)));

  readonly pagedAppointments = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.filteredAppointments().slice(start, start + this.pageSize);
  });

  readonly rangeStart = computed(() =>
    this.filteredAppointments().length === 0 ? 0 : (this.currentPage() - 1) * this.pageSize + 1
  );
  readonly rangeEnd = computed(() =>
    Math.min(this.currentPage() * this.pageSize, this.filteredAppointments().length)
  );

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
    this.currentPage.set(1);
    this.reload();
  }

  setSearchTerm(value: string): void {
    this.searchTerm.set(value);
    this.currentPage.set(1);
  }

  prevPage(): void {
    this.currentPage.update((p) => Math.max(1, p - 1));
  }

  nextPage(): void {
    this.currentPage.update((p) => Math.min(this.totalPages(), p + 1));
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
