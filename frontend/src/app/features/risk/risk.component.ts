import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { PatientService } from '../../core/services/patient.service';
import { RiskService } from '../../core/services/risk.service';
import { PatientSummary, RiskPrediction } from '../../core/models/api.models';

@Component({ selector: 'app-risk', standalone: true, imports: [CommonModule], templateUrl: './risk.component.html', styleUrl: './risk.component.scss' })
export class RiskComponent {
  private readonly patientsApi = inject(PatientService); private readonly riskApi = inject(RiskService);
  readonly patients = signal<PatientSummary[]>([]); readonly selectedPatient = signal(''); readonly cardiovascular = signal<RiskPrediction | null>(null); readonly diabetes = signal<RiskPrediction | null>(null); readonly history = signal<RiskPrediction[]>([]); readonly loading = signal(true); readonly error = signal('');
  constructor() { this.loadPatients(); }
  loadPatients(): void { this.loading.set(true); this.patientsApi.list().subscribe({ next: page => { this.patients.set(page.content); const first = page.content[0]?.id ?? ''; this.selectedPatient.set(first); if (first) this.load(first); else this.loading.set(false); }, error: () => { this.error.set('Risk intelligence could not be loaded.'); this.loading.set(false); } }); }
  selectPatient(event: Event): void { const id = (event.target as HTMLSelectElement).value; this.selectedPatient.set(id); this.load(id); }
  load(id: string): void { if (!id) { this.loading.set(false); return; } this.loading.set(true); this.error.set(''); forkJoin({ cardiovascular: this.riskApi.latest(id, 'CARDIOVASCULAR').pipe(catchError(() => of(null))), diabetes: this.riskApi.latest(id, 'DIABETES').pipe(catchError(() => of(null))), history: this.riskApi.history(id).pipe(catchError(() => of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }))) }).subscribe({ next: data => { this.cardiovascular.set(data.cardiovascular); this.diabetes.set(data.diabetes); this.history.set(data.history.content); this.loading.set(false); }, error: () => { this.error.set('Risk predictions could not be loaded.'); this.loading.set(false); } }); }
  patientName(): string { const patient = this.patients().find(item => item.id === this.selectedPatient()); return patient ? `${patient.firstName} ${patient.lastName}` : 'Select a patient'; }
}
