import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TimeSlot } from '../models/time-slot.model';

@Injectable({ providedIn: 'root' })
export class AvailabilityApiService {
  private readonly http = inject(HttpClient);

  getSlots(barberId: string, date: string, serviceId: string): Observable<TimeSlot[]> {
    return this.http.get<TimeSlot[]>(`${environment.apiUrl}/barbers/${barberId}/availability`, {
      params: { date, serviceId }
    });
  }
}
