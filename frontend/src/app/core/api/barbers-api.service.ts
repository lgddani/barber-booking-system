import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Barber } from '../models/barber.model';

@Injectable({ providedIn: 'root' })
export class BarbersApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<Barber[]> {
    return this.http.get<Barber[]>(`${environment.apiUrl}/barbers`);
  }

  get(id: string): Observable<Barber> {
    return this.http.get<Barber>(`${environment.apiUrl}/barbers/${id}`);
  }
}
