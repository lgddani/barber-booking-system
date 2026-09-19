import { Component, computed, inject, signal } from '@angular/core';
import { Observable, forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AppointmentsApiService } from '../../../core/api/appointments-api.service';
import { CatalogApiService } from '../../../core/api/catalog-api.service';
import { Appointment, AppointmentStatus } from '../../../core/models/appointment.model';

const STATUS_LABEL: Record<AppointmentStatus, string> = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmada',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
  NO_SHOW: 'No se presentó'
};

@Component({
  selector: 'app-barber-agenda',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './barber-agenda.component.html'
})
export class BarberAgendaComponent {
  private readonly appointmentsApi = inject(AppointmentsApiService);
  private readonly catalogApi = inject(CatalogApiService);

  readonly loading = signal(true);
  readonly appointments = signal<Appointment[]>([]);
  readonly serviceNames = signal<Record<string, string | undefined>>({});
  readonly selectedDate = signal<string>(this.todayIso());
  readonly actingId = signal<string | null>(null);

  readonly formattedDate = computed(() => this.formatDate(this.selectedDate()));
  readonly appointmentsForDay = computed(() =>
    this.appointments()
      .filter((a) => a.startAt.slice(0, 10) === this.selectedDate())
      .sort((a, b) => a.startAt.localeCompare(b.startAt))
  );

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    forkJoin({
      appointments: this.appointmentsApi.list(),
      services: this.catalogApi.list()
    }).subscribe(({ appointments, services }) => {
      this.serviceNames.set(Object.fromEntries(services.map((s) => [s.id, s.name])));
      this.appointments.set(appointments);
      this.loading.set(false);
    });
  }

  shiftDate(days: number): void {
    const next = new Date(`${this.selectedDate()}T00:00:00`);
    next.setDate(next.getDate() + days);
    this.selectedDate.set(next.toISOString().slice(0, 10));
  }

  canAct(appointment: Appointment): boolean {
    return appointment.status === 'CONFIRMED';
  }

  complete(appointment: Appointment): void {
    this.act(appointment.id, this.appointmentsApi.complete(appointment.id));
  }

  noShow(appointment: Appointment): void {
    this.act(appointment.id, this.appointmentsApi.noShow(appointment.id));
  }

  cancel(appointment: Appointment): void {
    this.act(appointment.id, this.appointmentsApi.cancel(appointment.id));
  }

  statusLabel(status: AppointmentStatus): string {
    return STATUS_LABEL[status];
  }

  formatTimeRange(startAt: string, endAt: string): string {
    return `${this.formatTime(startAt)}–${this.formatTime(endAt)}`;
  }

  private act(id: string, request: Observable<Appointment>): void {
    this.actingId.set(id);
    request.subscribe({
      next: () => {
        this.actingId.set(null);
        this.reload();
      },
      error: () => this.actingId.set(null)
    });
  }

  private formatTime(iso: string): string {
    return new Intl.DateTimeFormat('es', { hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private formatDate(iso: string): string {
    const date = new Date(`${iso}T00:00:00`);
    const formatted = new Intl.DateTimeFormat('es', { weekday: 'long', day: 'numeric', month: 'long' }).format(date);
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }
}
