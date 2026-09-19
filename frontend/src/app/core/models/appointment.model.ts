export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface Appointment {
  id: string;
  customerId: string;
  customerName: string;
  barberId: string;
  serviceId: string;
  startAt: string;
  endAt: string;
  status: AppointmentStatus;
  priceAtBooking: number;
  notes: string | null;
}

export interface AppointmentCreateRequest {
  barberId: string;
  serviceId: string;
  date: string;
  startTime: string;
}
