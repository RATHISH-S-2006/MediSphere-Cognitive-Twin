import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { Alert, Page } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AlertService {
  private readonly http = inject(HttpClient);

  list(patientId: string): Observable<Page<Alert>> {
    return this.http.get<Page<Alert>>(`${API_BASE_URL}/alerts/${patientId}`, { params: { size: 50 } });
  }

  active(patientId: string): Observable<Page<Alert>> {
    return this.http.get<Page<Alert>>(`${API_BASE_URL}/alerts/${patientId}/active`, { params: { size: 50 } });
  }

  history(patientId: string): Observable<Page<Alert>> {
    return this.http.get<Page<Alert>>(`${API_BASE_URL}/alerts/${patientId}/history`, { params: { size: 50 } });
  }

  acknowledge(alertId: string): Observable<Alert> {
    return this.http.post<Alert>(`${API_BASE_URL}/alerts/by-id/${alertId}/acknowledge`, {});
  }

  resolve(alertId: string): Observable<Alert> {
    return this.http.post<Alert>(`${API_BASE_URL}/alerts/by-id/${alertId}/resolve`, {});
  }
}
