import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Barber } from '../models/barber.model';
import { WorkingHoursItem } from '../models/working-hours.model';
import { ScheduleException, ScheduleExceptionRequest } from '../models/schedule-exception.model';

@Injectable({ providedIn: 'root' })
export class BarbersApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<Barber[]> {
    return this.http.get<Barber[]>(`${environment.apiUrl}/barbers`);
  }

  get(id: string): Observable<Barber> {
    return this.http.get<Barber>(`${environment.apiUrl}/barbers/${id}`);
  }

  getWorkingHours(barberId: string): Observable<WorkingHoursItem[]> {
    return this.http.get<WorkingHoursItem[]>(`${environment.apiUrl}/barbers/${barberId}/working-hours`);
  }

  replaceWorkingHours(barberId: string, items: WorkingHoursItem[]): Observable<WorkingHoursItem[]> {
    return this.http.put<WorkingHoursItem[]>(`${environment.apiUrl}/barbers/${barberId}/working-hours`, items);
  }

  listExceptions(barberId: string, from?: string): Observable<ScheduleException[]> {
    return this.http.get<ScheduleException[]>(`${environment.apiUrl}/barbers/${barberId}/exceptions`, {
      params: from ? { from } : {}
    });
  }

  createException(barberId: string, request: ScheduleExceptionRequest): Observable<ScheduleException> {
    return this.http.post<ScheduleException>(`${environment.apiUrl}/barbers/${barberId}/exceptions`, request);
  }

  deleteException(barberId: string, exceptionId: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/barbers/${barberId}/exceptions/${exceptionId}`);
  }
}
