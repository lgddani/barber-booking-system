import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/auth.service';
import { BarbersApiService } from '../../../core/api/barbers-api.service';
import { ExceptionType, ScheduleException } from '../../../core/models/schedule-exception.model';

const TYPE_LABEL: Record<ExceptionType, string> = {
  CLOSED: 'Día cerrado',
  CUSTOM_HOURS: 'Horario especial'
};

@Component({
  selector: 'app-barber-exceptions',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './barber-exceptions.component.html'
})
export class BarberExceptionsComponent {
  private readonly auth = inject(AuthService);
  private readonly barbersApi = inject(BarbersApiService);

  readonly loading = signal(true);
  readonly exceptions = signal<ScheduleException[]>([]);
  readonly deletingId = signal<string | null>(null);

  readonly newDate = signal(this.tomorrowIso());
  readonly newType = signal<ExceptionType>('CLOSED');
  readonly newStart = signal('09:00');
  readonly newEnd = signal('13:00');
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly minDate = this.todayIso();
  readonly isCustomHours = computed(() => this.newType() === 'CUSTOM_HOURS');

  constructor() {
    this.reload();
  }

  reload(): void {
    const barberId = this.auth.user()?.id;
    if (!barberId) {
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.barbersApi.listExceptions(barberId, this.todayIso()).subscribe((exceptions) => {
      this.exceptions.set(exceptions);
      this.loading.set(false);
    });
  }

  typeLabel(type: ExceptionType): string {
    return TYPE_LABEL[type];
  }

  setType(type: ExceptionType): void {
    this.newType.set(type);
  }

  create(): void {
    const barberId = this.auth.user()?.id;
    if (!barberId) {
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.barbersApi
      .createException(barberId, {
        date: this.newDate(),
        type: this.newType(),
        ...(this.isCustomHours() ? { startTime: this.newStart(), endTime: this.newEnd() } : {})
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.newDate.set(this.tomorrowIso());
          this.reload();
        },
        error: (err) => {
          this.submitting.set(false);
          this.errorMessage.set(err?.error?.message ?? 'No se pudo crear el bloqueo.');
        }
      });
  }

  delete(exception: ScheduleException): void {
    const barberId = this.auth.user()?.id;
    if (!barberId) {
      return;
    }
    this.deletingId.set(exception.id);
    this.barbersApi.deleteException(barberId, exception.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.reload();
      },
      error: () => this.deletingId.set(null)
    });
  }

  formatDate(iso: string): string {
    const date = new Date(`${iso}T00:00:00`);
    const formatted = new Intl.DateTimeFormat('es', { weekday: 'short', day: 'numeric', month: 'short' }).format(
      date
    );
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private tomorrowIso(): string {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().slice(0, 10);
  }
}
