import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TwinService } from '../../core/services/twin.service';
import { VitalsService } from '../../core/services/vitals.service';
import { LabService } from '../../core/services/lab.service';
import { ConsentService } from '../../core/services/consent.service';
import { FhirService } from '../../core/services/fhir.service';
import { PatientService } from '../../core/services/patient.service';
import { Consent, HealthTwin, LabResult, PatientDetail, Vitals } from '../../core/models/api.models';

@Component({ selector: 'app-patient-360', standalone: true, imports: [CommonModule, RouterLink], templateUrl: './patient-360.component.html', styleUrl: './patient-360.component.scss' })
export class Patient360Component {
  private readonly route = inject(ActivatedRoute); private readonly patientsApi = inject(PatientService); private readonly twinApi = inject(TwinService); private readonly vitalsApi = inject(VitalsService); private readonly labApi = inject(LabService); private readonly consentApi = inject(ConsentService); private readonly fhirApi = inject(FhirService);
  readonly loading = signal(true); readonly error = signal(''); readonly patient = signal<PatientDetail | null>(null); readonly twin = signal<HealthTwin | null>(null); readonly vitals = signal<Vitals[]>([]); readonly labs = signal<LabResult[]>([]); readonly consents = signal<Consent[]>([]); readonly fhirResources = signal<import('../../core/models/api.models').FhirResource[]>([]);
  constructor() { this.load(this.route.snapshot.paramMap.get('patientId') ?? ''); }
  load(id: string): void { this.loading.set(true); forkJoin({ patient: this.patientsApi.get(id), twin: this.twinApi.get(id), completeness: this.twinApi.completeness(id), vitals: this.vitalsApi.list(id), labs: this.labApi.list(id), fhir: this.fhirApi.list(id), consents: this.consentApi.list(id) }).subscribe({ next: data => { this.patient.set(data.patient); this.twin.set({ ...data.twin, completenessPercentage: data.completeness.completenessPercentage, missingDataPoints: data.completeness.missingDataPoints }); this.vitals.set(data.vitals.content); this.labs.set(data.labs.content); this.fhirResources.set(data.fhir.content); this.consents.set(data.consents); this.loading.set(false); }, error: () => { this.error.set('This patient record could not be loaded.'); this.loading.set(false); } }); }
  activeConsent(): Consent | undefined { return this.consents().find(consent => consent.status === 'GRANTED' && (!consent.expiresAt || new Date(consent.expiresAt) > new Date())); }
}
