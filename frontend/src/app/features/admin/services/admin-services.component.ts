import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CatalogApiService } from '../../../core/api/catalog-api.service';
import { ServiceOffering, ServiceOfferingRequest } from '../../../core/models/service-offering.model';

@Component({
  selector: 'app-admin-services',
  standalone: true,
  imports: [MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './admin-services.component.html'
})
export class AdminServicesComponent {
  private readonly catalogApi = inject(CatalogApiService);

  readonly loading = signal(true);
  readonly services = signal<ServiceOffering[]>([]);
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly togglingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly name = signal('');
  readonly description = signal('');
  readonly durationMinutes = signal(30);
  readonly price = signal(0);

  readonly isEditing = computed(() => this.editingId() !== null);

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

  startCreate(): void {
    this.editingId.set(null);
    this.name.set('');
    this.description.set('');
    this.durationMinutes.set(30);
    this.price.set(0);
    this.errorMessage.set(null);
  }

  startEdit(service: ServiceOffering): void {
    this.editingId.set(service.id);
    this.name.set(service.name);
    this.description.set(service.description ?? '');
    this.durationMinutes.set(service.durationMinutes);
    this.price.set(service.price);
    this.errorMessage.set(null);
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
        this.submitting.set(false);
        this.startCreate();
        this.reload();
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar el servicio.');
      }
    });
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
}
