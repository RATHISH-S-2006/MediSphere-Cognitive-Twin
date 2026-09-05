import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ClinicalDataService } from '../../core/services/clinical-data.service';
import { PatientService } from '../../core/services/patient.service';
import { Consent, HealthTwin, LabResult, PatientDetail, Vitals } from '../../core/models/api.models';

@Component({ selector: 'app-patient-360', standalone: true, imports: [CommonModule, RouterLink], templateUrl: './patient-360.component.html', styleUrl: './patient-360.component.scss' })
export class Patient360Component {
  private readonly route = inject(ActivatedRoute); private readonly patientsApi = inject(PatientService); private readonly clinicalApi = inject(ClinicalDataService);
  readonly loading = signal(true); readonly error = signal(''); readonly patient = signal<PatientDetail | null>(null); readonly twin = signal<HealthTwin | null>(null); readonly vitals = signal<Vitals[]>([]); readonly labs = signal<LabResult[]>([]); readonly consents = signal<Consent[]>([]);
  constructor() { this.load(this.route.snapshot.paramMap.get('patientId') ?? ''); }
  load(id: string): void { this.loading.set(true); forkJoin({ patient: this.patientsApi.get(id), twin: this.clinicalApi.twin(id), completeness: this.clinicalApi.completeness(id), vitals: this.clinicalApi.vitals(id), labs: this.clinicalApi.labs(id), consents: this.clinicalApi.consents(id) }).subscribe({ next: data => { this.patient.set(data.patient); this.twin.set({ ...data.twin, completenessPercentage: data.completeness.completenessPercentage, missingDataPoints: data.completeness.missingDataPoints }); this.vitals.set(data.vitals.content); this.labs.set(data.labs.content); this.consents.set(data.consents); this.loading.set(false); }, error: () => { this.error.set('This patient record could not be loaded.'); this.loading.set(false); } }); }
  activeConsent(): Consent | undefined { return this.consents().find(consent => consent.status === 'GRANTED' && (!consent.expiresAt || new Date(consent.expiresAt) > new Date())); }
  fhirType(id: string): string { return id.includes('obs') ? 'Observation' : id.includes('cond') ? 'Condition' : 'Patient'; }
}
