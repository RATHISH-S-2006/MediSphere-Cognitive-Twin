import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { Page, RiskPrediction } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class RiskService {
  private readonly http = inject(HttpClient);

  latest(patientId: string, modelType: string): Observable<RiskPrediction> {
    return this.http.get<RiskPrediction>(`${API_BASE_URL}/risk/${patientId}/latest`, {
      params: { modelType },
    });
  }

  history(patientId: string, page = 0, size = 10): Observable<Page<RiskPrediction>> {
    return this.http.get<Page<RiskPrediction>>(`${API_BASE_URL}/risk/${patientId}`, {
      params: { page, size },
    });
  }

  predict(patientId: string, modelType: string): Observable<RiskPrediction> {
    return this.http.post<RiskPrediction>(`${API_BASE_URL}/risk/${patientId}/${this.path(modelType)}/predict`, {
      patientId,
      modelType,
      source: 'angular-patient-360',
    });
  }

  private path(modelType: string): string {
    return modelType === 'DIABETES' ? 'diabetes' : 'cardiovascular';
  }
}
