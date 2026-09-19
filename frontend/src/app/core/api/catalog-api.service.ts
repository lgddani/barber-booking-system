import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ServiceOffering } from '../models/service-offering.model';

@Injectable({ providedIn: 'root' })
export class CatalogApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<ServiceOffering[]> {
    return this.http.get<ServiceOffering[]>(`${environment.apiUrl}/services`);
  }
}
