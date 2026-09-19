import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CatalogApiService } from '../../../core/api/catalog-api.service';
import { BarbersApiService } from '../../../core/api/barbers-api.service';
import { AvailabilityApiService } from '../../../core/api/availability-api.service';
import { AppointmentsApiService } from '../../../core/api/appointments-api.service';
import { ServiceOffering } from '../../../core/models/service-offering.model';
import { Barber } from '../../../core/models/barber.model';
import { TimeSlot } from '../../../core/models/time-slot.model';
import { Appointment } from '../../../core/models/appointment.model';

type Step = 1 | 2 | 3 | 4;

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './booking.component.html'
})
export class BookingComponent {
  private readonly catalogApi = inject(CatalogApiService);
  private readonly barbersApi = inject(BarbersApiService);
  private readonly availabilityApi = inject(AvailabilityApiService);
  private readonly appointmentsApi = inject(AppointmentsApiService);

  readonly step = signal<Step>(1);

  readonly services = signal<ServiceOffering[]>([]);
  readonly barbers = signal<Barber[]>([]);
  readonly loadingCatalog = signal(true);

  readonly selectedService = signal<ServiceOffering | null>(null);
  readonly selectedBarber = signal<Barber | null>(null);
  readonly selectedDate = signal<string>(this.todayIso());
  readonly slots = signal<TimeSlot[]>([]);
  readonly selectedTime = signal<string | null>(null);
  readonly loadingSlots = signal(false);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly confirmedAppointment = signal<Appointment | null>(null);

  readonly canGoPrevDay = computed(() => this.selectedDate() > this.todayIso());
  readonly formattedDate = computed(() => this.formatDate(this.selectedDate()));

  constructor() {
    this.catalogApi.list().subscribe((services) => {
      this.services.set(services.filter((s) => s.active));
      this.checkCatalogLoaded();
    });
    this.barbersApi.list().subscribe((barbers) => {
      this.barbers.set(barbers.filter((b) => b.active));
      this.checkCatalogLoaded();
    });
  }

  private loadedParts = 0;
  private checkCatalogLoaded(): void {
    this.loadedParts++;
    if (this.loadedParts >= 2) {
      this.loadingCatalog.set(false);
    }
  }

  pickService(service: ServiceOffering): void {
    this.selectedService.set(service);
    this.step.set(2);
  }

  pickBarber(barber: Barber): void {
    this.selectedBarber.set(barber);
    this.step.set(3);
    this.loadSlots();
  }

  shiftDate(days: number): void {
    const next = new Date(`${this.selectedDate()}T00:00:00`);
    next.setDate(next.getDate() + days);
    const iso = next.toISOString().slice(0, 10);
    if (iso < this.todayIso()) {
      return;
    }
    this.selectedDate.set(iso);
    this.selectedTime.set(null);
    this.loadSlots();
  }

  pickSlot(slot: TimeSlot): void {
    if (!slot.available) {
      return;
    }
    this.selectedTime.set(slot.time);
    this.step.set(4);
  }

  confirm(): void {
    const barber = this.selectedBarber();
    const service = this.selectedService();
    const time = this.selectedTime();
    if (!barber || !service || !time) {
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.appointmentsApi
      .create({
        barberId: barber.id,
        serviceId: service.id,
        date: this.selectedDate(),
        startTime: time
      })
      .subscribe({
        next: (appointment) => {
          this.submitting.set(false);
          this.confirmedAppointment.set(appointment);
        },
        error: (err) => {
          this.submitting.set(false);
          if (err?.status === 409) {
            this.errorMessage.set('Justo se ocupó ese horario. Elige otro disponible.');
            this.step.set(3);
            this.loadSlots();
          } else {
            this.errorMessage.set('No se pudo confirmar la reserva. Intenta de nuevo.');
          }
        }
      });
  }

  startOver(): void {
    this.step.set(1);
    this.selectedService.set(null);
    this.selectedBarber.set(null);
    this.selectedTime.set(null);
    this.slots.set([]);
    this.confirmedAppointment.set(null);
    this.errorMessage.set(null);
  }

  goBackTo(step: Step): void {
    this.step.set(step);
  }

  formatSlotTime(time: string): string {
    return time.slice(0, 5);
  }

  private loadSlots(): void {
    const barber = this.selectedBarber();
    const service = this.selectedService();
    if (!barber || !service) {
      return;
    }
    this.loadingSlots.set(true);
    this.availabilityApi.getSlots(barber.id, this.selectedDate(), service.id).subscribe({
      next: (slots) => {
        this.slots.set(slots);
        this.loadingSlots.set(false);
      },
      error: () => {
        this.slots.set([]);
        this.loadingSlots.set(false);
      }
    });
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private formatDate(iso: string): string {
    const date = new Date(`${iso}T00:00:00`);
    const formatted = new Intl.DateTimeFormat('es', { weekday: 'short', day: 'numeric', month: 'short' }).format(
      date
    );
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }
}
