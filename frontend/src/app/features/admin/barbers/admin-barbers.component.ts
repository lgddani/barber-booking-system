import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BarbersApiService } from '../../../core/api/barbers-api.service';
import { Barber } from '../../../core/models/barber.model';

@Component({
  selector: 'app-admin-barbers',
  standalone: true,
  imports: [MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './admin-barbers.component.html'
})
export class AdminBarbersComponent {
  private readonly barbersApi = inject(BarbersApiService);

  readonly loading = signal(true);
  readonly barbers = signal<Barber[]>([]);
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly togglingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly email = signal('');
  readonly password = signal('');
  readonly fullName = signal('');
  readonly phone = signal('');
  readonly bio = signal('');

  readonly isEditing = computed(() => this.editingId() !== null);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.barbersApi.list(true).subscribe((barbers) => {
      this.barbers.set(barbers);
      this.loading.set(false);
    });
  }

  startCreate(): void {
    this.editingId.set(null);
    this.email.set('');
    this.password.set('');
    this.fullName.set('');
    this.phone.set('');
    this.bio.set('');
    this.errorMessage.set(null);
  }

  startEdit(barber: Barber): void {
    this.editingId.set(barber.id);
    this.fullName.set(barber.fullName);
    this.phone.set(barber.phone ?? '');
    this.bio.set(barber.bio ?? '');
    this.errorMessage.set(null);
  }

  save(): void {
    this.submitting.set(true);
    this.errorMessage.set(null);
    const editingId = this.editingId();

    const request$ = editingId
      ? this.barbersApi.update(editingId, {
          fullName: this.fullName(),
          phone: this.phone() || undefined,
          bio: this.bio() || undefined
        })
      : this.barbersApi.create({
          email: this.email(),
          password: this.password(),
          fullName: this.fullName(),
          phone: this.phone() || undefined,
          bio: this.bio() || undefined
        });

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.startCreate();
        this.reload();
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar el barbero.');
      }
    });
  }

  toggleActive(barber: Barber): void {
    this.togglingId.set(barber.id);
    const request$ = barber.active ? this.barbersApi.deactivate(barber.id) : this.barbersApi.activate(barber.id);
    request$.subscribe({
      next: () => {
        this.togglingId.set(null);
        this.reload();
      },
      error: () => this.togglingId.set(null)
    });
  }
}
