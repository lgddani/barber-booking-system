import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdminStatsApiService } from '../../../core/api/admin-stats-api.service';
import { AdminStats } from '../../../core/models/admin-stats.model';
import { AppointmentStatus } from '../../../core/models/appointment.model';

const STATUS_LABEL: Record<AppointmentStatus, string> = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmada',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
  NO_SHOW: 'No se presentó'
};

@Component({
  selector: 'app-admin-stats',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './admin-stats.component.html'
})
export class AdminStatsComponent {
  private readonly statsApi = inject(AdminStatsApiService);

  readonly loading = signal(true);
  readonly stats = signal<AdminStats | null>(null);
  readonly fromInput = signal('');
  readonly toInput = signal('');

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    const from = this.fromInput() || undefined;
    const to = this.toInput() || undefined;
    this.statsApi.getStats(from, to).subscribe((stats) => {
      this.stats.set(stats);
      this.fromInput.set(stats.from);
      this.toInput.set(stats.to);
      this.loading.set(false);
    });
  }

  statusLabel(status: string): string {
    return STATUS_LABEL[status as AppointmentStatus] ?? status;
  }

  statusRows(): { status: string; label: string; count: number }[] {
    const stats = this.stats();
    if (!stats) {
      return [];
    }
    return Object.entries(stats.byStatus).map(([status, count]) => ({
      status,
      label: this.statusLabel(status),
      count: count ?? 0
    }));
  }

  statusPercent(count: number): number {
    const total = this.stats()?.totalAppointments ?? 0;
    return total === 0 ? 0 : Math.round((count / total) * 100);
  }
}
