import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { FhirResource, PatientSummary } from '../../core/models/api.models';
import { FhirService } from '../../core/services/fhir.service';
import { PatientService } from '../../core/services/patient.service';

@Component({ selector: 'app-fhir', standalone: true, imports: [CommonModule], templateUrl: './fhir.component.html', styleUrl: './fhir.component.scss' })
export class FhirComponent {
  private readonly patientsApi = inject(PatientService); private readonly fhirApi = inject(FhirService);
  readonly patients = signal<PatientSummary[]>([]); readonly resources = signal<FhirResource[]>([]); readonly selectedPatient = signal(''); readonly expanded = signal(''); readonly loading = signal(true); readonly error = signal('');
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.error.set(''); this.patientsApi.list().subscribe({ next: page => { this.patients.set(page.content); const id = page.content[0]?.id ?? ''; this.selectedPatient.set(id); if (!id) { this.resources.set([]); this.loading.set(false); return; } this.fetch(id); }, error: () => { this.error.set('FHIR data could not be loaded.'); this.loading.set(false); } }); }
  select(event: Event): void { const id = (event.target as HTMLSelectElement).value; this.selectedPatient.set(id); this.fetch(id); }
  fetch(id: string): void { this.loading.set(true); this.fhirApi.list(id).subscribe({ next: page => { this.resources.set(page.content); this.loading.set(false); }, error: () => { this.error.set('FHIR resources could not be loaded.'); this.loading.set(false); } }); }
  resourceTypeCount(): number { return new Set(this.resources().map(resource => resource.resourceType)).size; }
  patientName(id: string): string { const patient = this.patients().find(item => item.id === id); return patient ? `${patient.firstName} ${patient.lastName}` : id; }
}
