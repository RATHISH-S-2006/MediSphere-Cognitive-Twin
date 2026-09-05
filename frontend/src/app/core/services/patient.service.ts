import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { Page, PatientDetail, PatientSummary } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);

  list(page = 0, size = 100): Observable<Page<PatientSummary>> {
    return this.http.get<Page<PatientSummary>>(`${API_BASE_URL}/patients`, {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  get(patientId: string): Observable<PatientDetail> {
    return this.http.get<PatientDetail>(`${API_BASE_URL}/patients/${patientId}`);
  }
}
