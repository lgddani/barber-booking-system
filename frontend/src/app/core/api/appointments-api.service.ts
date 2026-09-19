import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Appointment, AppointmentCreateRequest } from '../models/appointment.model';

@Injectable({ providedIn: 'root' })
export class AppointmentsApiService {
  private readonly http = inject(HttpClient);

  create(request: AppointmentCreateRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${environment.apiUrl}/appointments`, request);
  }

  list(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${environment.apiUrl}/appointments`);
  }

  cancel(id: string): Observable<Appointment> {
    return this.http.patch<Appointment>(`${environment.apiUrl}/appointments/${id}/cancel`, {});
  }
}
