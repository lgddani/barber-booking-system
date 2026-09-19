import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ServiceOffering, ServiceOfferingRequest } from '../models/service-offering.model';

@Injectable({ providedIn: 'root' })
export class CatalogApiService {
  private readonly http = inject(HttpClient);

  list(includeInactive = false): Observable<ServiceOffering[]> {
    return this.http.get<ServiceOffering[]>(`${environment.apiUrl}/services`, {
      params: { includeInactive }
    });
  }

  create(request: ServiceOfferingRequest): Observable<ServiceOffering> {
    return this.http.post<ServiceOffering>(`${environment.apiUrl}/services`, request);
  }

  update(id: string, request: ServiceOfferingRequest): Observable<ServiceOffering> {
    return this.http.put<ServiceOffering>(`${environment.apiUrl}/services/${id}`, request);
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/services/${id}`);
  }

  activate(id: string): Observable<void> {
    return this.http.patch<void>(`${environment.apiUrl}/services/${id}/activate`, {});
  }
}
