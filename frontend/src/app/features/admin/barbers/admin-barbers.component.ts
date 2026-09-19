import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { BarbersApiService } from '../../../core/api/barbers-api.service';
import { Barber } from '../../../core/models/barber.model';

const PAGE_SIZE = 8;

@Component({
  selector: 'app-admin-barbers',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatSlideToggleModule],
  templateUrl: './admin-barbers.component.html'
})
export class AdminBarbersComponent {
  private readonly barbersApi = inject(BarbersApiService);

  readonly loading = signal(true);
  readonly barbers = signal<Barber[]>([]);
  readonly searchTerm = signal('');
  readonly currentPage = signal(1);
  readonly pageSize = PAGE_SIZE;

  readonly modalOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly togglingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly email = signal('');
  readonly password = signal('');
  readonly fullName = signal('');
  readonly phone = signal('');
  readonly bio = signal('');
  readonly activeToggle = signal(true);
  private originalActive = true;

  readonly isEditing = computed(() => this.editingId() !== null);

  readonly filteredBarbers = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const all = this.barbers();
    return term ? all.filter((b) => b.fullName.toLowerCase().includes(term)) : all;
  });

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredBarbers().length / this.pageSize)));

  readonly pagedBarbers = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.filteredBarbers().slice(start, start + this.pageSize);
  });

  readonly rangeStart = computed(() =>
    this.filteredBarbers().length === 0 ? 0 : (this.currentPage() - 1) * this.pageSize + 1
  );
  readonly rangeEnd = computed(() => Math.min(this.currentPage() * this.pageSize, this.filteredBarbers().length));

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

  openCreateModal(): void {
    this.editingId.set(null);
    this.email.set('');
    this.password.set('');
    this.fullName.set('');
    this.phone.set('');
    this.bio.set('');
    this.activeToggle.set(true);
    this.originalActive = true;
    this.errorMessage.set(null);
    this.modalOpen.set(true);
  }

  openEditModal(barber: Barber): void {
    this.editingId.set(barber.id);
    this.fullName.set(barber.fullName);
    this.phone.set(barber.phone ?? '');
    this.bio.set(barber.bio ?? '');
    this.activeToggle.set(barber.active);
    this.originalActive = barber.active;
    this.errorMessage.set(null);
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
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
        // Igual que en Servicios: el estado es una llamada aparte del
        // backend, solo se dispara si de verdad cambió.
        if (editingId && this.activeToggle() !== this.originalActive) {
          const stateChange$ = this.activeToggle()
            ? this.barbersApi.activate(editingId)
            : this.barbersApi.deactivate(editingId);
          stateChange$.subscribe({
            next: () => this.finishSave(),
            error: (err) => this.handleSaveError(err)
          });
        } else {
          this.finishSave();
        }
      },
      error: (err) => this.handleSaveError(err)
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

  private finishSave(): void {
    this.submitting.set(false);
    this.modalOpen.set(false);
    this.reload();
  }

  private handleSaveError(err: any): void {
    this.submitting.set(false);
    this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar el barbero.');
  }
}
