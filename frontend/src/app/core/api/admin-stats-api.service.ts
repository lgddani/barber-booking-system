import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminStats } from '../models/admin-stats.model';

@Injectable({ providedIn: 'root' })
export class AdminStatsApiService {
  private readonly http = inject(HttpClient);

  getStats(from?: string, to?: string): Observable<AdminStats> {
    const params: Record<string, string> = {};
    if (from) {
      params['from'] = from;
    }
    if (to) {
      params['to'] = to;
    }
    return this.http.get<AdminStats>(`${environment.apiUrl}/admin/stats`, { params });
  }
}
