import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { Page, Vitals } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class VitalsService {
  private readonly http = inject(HttpClient);
  list(patientId: string): Observable<Page<Vitals>> { return this.http.get<Page<Vitals>>(`${API_BASE_URL}/vitals/${patientId}`, { params: { size: 20 } }); }
}
