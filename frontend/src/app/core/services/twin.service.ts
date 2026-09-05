import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { HealthTwin } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class TwinService {
  private readonly http = inject(HttpClient);
  get(patientId: string): Observable<HealthTwin> { return this.http.get<HealthTwin>(`${API_BASE_URL}/twins/${patientId}`); }
  completeness(patientId: string): Observable<HealthTwin> { return this.http.get<HealthTwin>(`${API_BASE_URL}/twins/${patientId}/completeness`); }
}
