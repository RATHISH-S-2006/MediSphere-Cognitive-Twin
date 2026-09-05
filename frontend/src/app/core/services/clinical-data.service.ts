import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { Consent, ConsentVerification, HealthTwin, LabResult, Page, Vitals } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ClinicalDataService {
  private readonly http = inject(HttpClient);

  twin(patientId: string): Observable<HealthTwin> {
    return this.http.get<HealthTwin>(`${API_BASE_URL}/twins/${patientId}`);
  }

  completeness(patientId: string): Observable<HealthTwin> {
    return this.http.get<HealthTwin>(`${API_BASE_URL}/twins/${patientId}/completeness`);
  }

  vitals(patientId: string): Observable<Page<Vitals>> {
    return this.http.get<Page<Vitals>>(`${API_BASE_URL}/vitals/${patientId}`, { params: { size: 20 } });
  }

  labs(patientId: string): Observable<Page<LabResult>> {
    return this.http.get<Page<LabResult>>(`${API_BASE_URL}/labs/${patientId}`, { params: { size: 20 } });
  }

  consents(patientId: string): Observable<Consent[]> {
    return this.http.get<Consent[]>(`${API_BASE_URL}/consents/${patientId}`);
  }

  consentStatus(patientId: string): Observable<ConsentVerification> {
    return this.http.get<ConsentVerification>(`${API_BASE_URL}/consents/${patientId}/verify`);
  }
}
