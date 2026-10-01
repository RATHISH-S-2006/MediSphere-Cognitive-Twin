import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Consent, ConsentVerification, PatientSummary } from '../../core/models/api.models';
import { ConsentService } from '../../core/services/consent.service';
import { PatientService } from '../../core/services/patient.service';

@Component({ selector: 'app-consent', standalone: true, imports: [CommonModule], templateUrl: './consent.component.html', styleUrl: './consent.component.scss' })
export class ConsentComponent {
  private readonly patientsApi = inject(PatientService); private readonly consentApi = inject(ConsentService);
  readonly patients = signal<PatientSummary[]>([]); readonly consents = signal<Consent[]>([]); readonly verification = signal<ConsentVerification | null>(null); readonly selectedPatient = signal(''); readonly loading = signal(true); readonly error = signal('');
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.error.set(''); this.patientsApi.list().subscribe({ next: page => { this.patients.set(page.content); const id = page.content[0]?.id ?? ''; this.selectedPatient.set(id); if (id) this.fetch(id); else this.loading.set(false); }, error: () => { this.error.set('Consent data could not be loaded.'); this.loading.set(false); } }); }
  select(event: Event): void { const id = (event.target as HTMLSelectElement).value; this.selectedPatient.set(id); this.fetch(id); }
  fetch(id: string): void { this.loading.set(true); forkJoin({ consents: this.consentApi.list(id), verification: this.consentApi.verify(id) }).subscribe({ next: data => { this.consents.set(data.consents); this.verification.set(data.verification); this.loading.set(false); }, error: () => { this.error.set('Consent information could not be loaded.'); this.loading.set(false); } }); }
  patientName(): string { const patient = this.patients().find(item => item.id === this.selectedPatient()); return patient ? `${patient.firstName} ${patient.lastName}` : 'Select a patient'; }
}
