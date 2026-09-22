import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { AdherenceSummary, CarePlan, CarePlanOutcome, Page } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class CarePlanService {
  private readonly http = inject(HttpClient);

  list(patientId: string): Observable<Page<CarePlan>> { return this.http.get<Page<CarePlan>>(`${API_BASE_URL}/care-plans/${patientId}`, { params: { size: 50 } }); }
  active(patientId: string): Observable<CarePlan> { return this.http.get<CarePlan>(`${API_BASE_URL}/care-plans/${patientId}/active`); }
  generate(patientId: string): Observable<CarePlan> { return this.http.post<CarePlan>(`${API_BASE_URL}/care-plans/${patientId}/generate`, {}); }
  transition(id: string, action: 'activate' | 'pause' | 'complete' | 'cancel'): Observable<CarePlan> { return this.http.post<CarePlan>(`${API_BASE_URL}/care-plans/by-id/${id}/${action}`, {}); }
  mark(id: string, interventionId: string, action: 'complete' | 'miss'): Observable<CarePlan> { return this.http.post<CarePlan>(`${API_BASE_URL}/care-plans/by-id/${id}/interventions/${interventionId}/${action}`, {}); }
  adherence(id: string): Observable<AdherenceSummary> { return this.http.get<AdherenceSummary>(`${API_BASE_URL}/care-plans/by-id/${id}/adherence`); }
  outcomes(id: string): Observable<CarePlanOutcome[]> { return this.http.get<CarePlanOutcome[]>(`${API_BASE_URL}/care-plans/by-id/${id}/outcomes`); }
  recordOutcome(id: string, outcome: { goalId: string; measuredValue: number; unit?: string; notes?: string; source?: string; status: string }): Observable<CarePlanOutcome> { return this.http.post<CarePlanOutcome>(`${API_BASE_URL}/care-plans/by-id/${id}/outcomes`, outcome); }
}