import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { CatalogApiService } from '../../../core/api/catalog-api.service';
import { ServiceOffering, ServiceOfferingRequest } from '../../../core/models/service-offering.model';

const PAGE_SIZE = 8;

@Component({
  selector: 'app-admin-services',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatSlideToggleModule],
  templateUrl: './admin-services.component.html'
})
export class AdminServicesComponent {
  private readonly catalogApi = inject(CatalogApiService);

  readonly loading = signal(true);
  readonly services = signal<ServiceOffering[]>([]);
  readonly searchTerm = signal('');
  readonly currentPage = signal(1);
  readonly pageSize = PAGE_SIZE;

  readonly modalOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly togglingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly name = signal('');
  readonly description = signal('');
  readonly durationMinutes = signal(30);
  readonly price = signal(0);
  readonly activeToggle = signal(true);
  private originalActive = true;

  readonly isEditing = computed(() => this.editingId() !== null);

  readonly filteredServices = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const all = this.services();
    return term ? all.filter((s) => s.name.toLowerCase().includes(term)) : all;
  });

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredServices().length / this.pageSize)));

  readonly pagedServices = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.filteredServices().slice(start, start + this.pageSize);
  });

  readonly rangeStart = computed(() =>
    this.filteredServices().length === 0 ? 0 : (this.currentPage() - 1) * this.pageSize + 1
  );
  readonly rangeEnd = computed(() => Math.min(this.currentPage() * this.pageSize, this.filteredServices().length));

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.catalogApi.list(true).subscribe((services) => {
      this.services.set(services);
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
    this.name.set('');
    this.description.set('');
    this.durationMinutes.set(30);
    this.price.set(0);
    this.activeToggle.set(true);
    this.originalActive = true;
    this.errorMessage.set(null);
    this.modalOpen.set(true);
  }

  openEditModal(service: ServiceOffering): void {
    this.editingId.set(service.id);
    this.name.set(service.name);
    this.description.set(service.description ?? '');
    this.durationMinutes.set(service.durationMinutes);
    this.price.set(service.price);
    this.activeToggle.set(service.active);
    this.originalActive = service.active;
    this.errorMessage.set(null);
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
  }

  toggleActive(service: ServiceOffering): void {
    this.togglingId.set(service.id);
    const request$ = service.active ? this.catalogApi.deactivate(service.id) : this.catalogApi.activate(service.id);
    request$.subscribe({
      next: () => {
        this.togglingId.set(null);
        this.reload();
      },
      error: () => this.togglingId.set(null)
    });
  }

  save(): void {
    const request: ServiceOfferingRequest = {
      name: this.name(),
      description: this.description() || undefined,
      durationMinutes: this.durationMinutes(),
      price: this.price()
    };
    this.submitting.set(true);
    this.errorMessage.set(null);

    const editingId = this.editingId();
    const request$ = editingId ? this.catalogApi.update(editingId, request) : this.catalogApi.create(request);

    request$.subscribe({
      next: () => {
        // El estado (activo/inactivo) es una llamada aparte en el backend —
        // solo la disparamos si de verdad cambió, para no hacer una petición
        // extra innecesaria en cada edición.
        if (editingId && this.activeToggle() !== this.originalActive) {
          const stateChange$ = this.activeToggle()
            ? this.catalogApi.activate(editingId)
            : this.catalogApi.deactivate(editingId);
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

  private finishSave(): void {
    this.submitting.set(false);
    this.modalOpen.set(false);
    this.reload();
  }

  private handleSaveError(err: any): void {
    this.submitting.set(false);
    this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar el servicio.');
  }
}
