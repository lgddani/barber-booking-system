import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './barber-working-hours.component.html'
})
export class BarberWorkingHoursComponent {
  private readonly auth = inject(AuthService);
  private readonly barbersApi = inject(BarbersApiService);
  private readonly route = inject(ActivatedRoute);

  private barberId = '';

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly days = signal<DayGroup[]>([]);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  // Cuando un admin edita el horario de OTRO barbero (en vez del propio) via
  // /admin/barberos/:barberId/horarios, guardamos su nombre para el título.
  // null significa "estoy viendo mi propio horario" (ruta /barbero/horarios).
  readonly viewingBarberName = signal<string | null>(null);

  constructor() {
    // paramMap (no snapshot): si un admin navega del horario de un barbero
    // directo al de otro, Angular reutiliza la misma instancia del
    // componente para la misma ruta — sin esto, se quedaría mostrando los
    // datos del primero.
    this.route.paramMap.subscribe((params) => {
      const routeBarberId = params.get('barberId');
      const targetId = routeBarberId ?? this.auth.user()?.id;
      if (!targetId) {
        this.loading.set(false);
        return;
      }
      this.barberId = targetId;

      if (routeBarberId) {
        this.barbersApi.get(routeBarberId).subscribe((barber) => this.viewingBarberName.set(barber.fullName));
      } else {
        this.viewingBarberName.set(null);
      }

      this.loadHours();
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
    const items: WorkingHoursItem[] = this.days().flatMap((d) =>
      d.blocks.map((b) => ({ dayOfWeek: d.dayOfWeek, startTime: b.startTime, endTime: b.endTime }))
    );

    this.saving.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);
    this.barbersApi.replaceWorkingHours(this.barberId, items).subscribe({
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

  private loadHours(): void {
    this.loading.set(true);
    this.barbersApi.getWorkingHours(this.barberId).subscribe((items) => {
      this.days.set(this.buildDays(items));
      this.loading.set(false);
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
