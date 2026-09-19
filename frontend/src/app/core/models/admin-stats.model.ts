import { AppointmentStatus } from './appointment.model';

export interface BusiestBarber {
  barberId: string;
  fullName: string;
  appointmentCount: number;
}

export interface AdminStats {
  from: string;
  to: string;
  totalAppointments: number;
  byStatus: Partial<Record<AppointmentStatus, number>>;
  estimatedRevenue: number;
  busiestBarber: BusiestBarber | null;
}
