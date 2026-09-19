import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/auth.service';
import { BarbersApiService } from '../../../core/api/barbers-api.service';
import { DayOfWeek, WorkingHoursItem } from '../../../core/models/working-hours.model';

interface DayBlock {
  startTime: string;
  endTime: string;
}

interface DayGroup {
  dayOfWeek: DayOfWeek;
  label: string;
  blocks: DayBlock[];
}

const DAYS: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Lunes' },
  { value: 'TUESDAY', label: 'Martes' },
  { value: 'WEDNESDAY', label: 'Miércoles' },
  { value: 'THURSDAY', label: 'Jueves' },
  { value: 'FRIDAY', label: 'Viernes' },
  { value: 'SATURDAY', label: 'Sábado' },
  { value: 'SUNDAY', label: 'Domingo' }
];

@Component({
  selector: 'app-barber-working-hours',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './barber-working-hours.component.html'
})
export class BarberWorkingHoursComponent {
  private readonly auth = inject(AuthService);
  private readonly barbersApi = inject(BarbersApiService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly days = signal<DayGroup[]>([]);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    const barberId = this.auth.user()?.id;
    if (!barberId) {
      this.loading.set(false);
      return;
    }
    this.barbersApi.getWorkingHours(barberId).subscribe((items) => {
      this.days.set(this.buildDays(items));
      this.loading.set(false);
    });
  }

  addBlock(dayOfWeek: DayOfWeek): void {
    this.days.update((days) =>
      days.map((d) =>
        d.dayOfWeek === dayOfWeek ? { ...d, blocks: [...d.blocks, { startTime: '09:00', endTime: '13:00' }] } : d
      )
    );
  }

  removeBlock(dayOfWeek: DayOfWeek, index: number): void {
    this.days.update((days) =>
      days.map((d) => (d.dayOfWeek === dayOfWeek ? { ...d, blocks: d.blocks.filter((_, i) => i !== index) } : d))
    );
  }

  updateBlockTime(dayOfWeek: DayOfWeek, index: number, field: 'startTime' | 'endTime', value: string): void {
    this.days.update((days) =>
      days.map((d) =>
        d.dayOfWeek === dayOfWeek
          ? { ...d, blocks: d.blocks.map((b, i) => (i === index ? { ...b, [field]: value } : b)) }
          : d
      )
    );
  }

  save(): void {
    const barberId = this.auth.user()?.id;
    if (!barberId) {
      return;
    }
    const items: WorkingHoursItem[] = this.days().flatMap((d) =>
      d.blocks.map((b) => ({ dayOfWeek: d.dayOfWeek, startTime: b.startTime, endTime: b.endTime }))
    );

    this.saving.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);
    this.barbersApi.replaceWorkingHours(barberId, items).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.days.set(this.buildDays(updated));
        this.successMessage.set('Horario guardado.');
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar el horario.');
      }
    });
  }

  private buildDays(items: WorkingHoursItem[]): DayGroup[] {
    return DAYS.map(({ value, label }) => ({
      dayOfWeek: value,
      label,
      blocks: items
        .filter((i) => i.dayOfWeek === value)
        .sort((a, b) => a.startTime.localeCompare(b.startTime))
        .map((i) => ({ startTime: i.startTime.slice(0, 5), endTime: i.endTime.slice(0, 5) }))
    }));
  }
}
