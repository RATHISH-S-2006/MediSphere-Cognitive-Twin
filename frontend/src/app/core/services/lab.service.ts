import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { LabResult, Page } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class LabService {
  private readonly http = inject(HttpClient);
  list(patientId: string): Observable<Page<LabResult>> { return this.http.get<Page<LabResult>>(`${API_BASE_URL}/labs/${patientId}`, { params: { size: 20 } }); }
}
