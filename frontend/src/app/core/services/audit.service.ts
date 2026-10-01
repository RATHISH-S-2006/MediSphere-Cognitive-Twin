import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { AuditEvent, Page } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  list(page = 0, size = 50): Observable<Page<AuditEvent>> {
    return this.http.get<Page<AuditEvent>>(`${API_BASE_URL}/audit`, { params: new HttpParams().set('page', page).set('size', size) });
  }
}