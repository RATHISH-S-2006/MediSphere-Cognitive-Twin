import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { Consent, ConsentVerification } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ConsentService {
  private readonly http = inject(HttpClient);
  list(patientId: string): Observable<Consent[]> { return this.http.get<Consent[]>(`${API_BASE_URL}/consents/${patientId}`); }
  verify(patientId: string): Observable<ConsentVerification> { return this.http.get<ConsentVerification>(`${API_BASE_URL}/consents/${patientId}/verify`); }
}
