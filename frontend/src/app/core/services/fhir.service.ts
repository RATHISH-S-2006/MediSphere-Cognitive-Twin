import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { FhirResource, FhirValidationResponse, Page } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class FhirService {
  private readonly http = inject(HttpClient);
  list(patientId: string): Observable<Page<FhirResource>> { return this.http.get<Page<FhirResource>>(`${API_BASE_URL}/fhir/${patientId}`); }
  validate(resourceJson: string): Observable<FhirValidationResponse> { return this.http.post<FhirValidationResponse>(`${API_BASE_URL}/fhir/validate`, { resourceJson }); }
}
