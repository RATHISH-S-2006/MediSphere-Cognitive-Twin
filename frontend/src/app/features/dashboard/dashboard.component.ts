import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { TwinService } from '../../core/services/twin.service';
import { VitalsService } from '../../core/services/vitals.service';
import { LabService } from '../../core/services/lab.service';
import { ConsentService } from '../../core/services/consent.service';
import { FhirService } from '../../core/services/fhir.service';
import { PatientService } from '../../core/services/patient.service';
import { HealthTwin, LabResult, PatientSummary, Vitals } from '../../core/models/api.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly patientsApi = inject(PatientService);
  private readonly twinApi = inject(TwinService);
  private readonly vitalsApi = inject(VitalsService);
  private readonly labApi = inject(LabService);
  private readonly consentApi = inject(ConsentService);
  private readonly fhirApi = inject(FhirService);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly patients = signal<PatientSummary[]>([]);
  readonly twins = signal<HealthTwin[]>([]);
  readonly recentVitals = signal<Vitals[]>([]);
  readonly recentLabs = signal<LabResult[]>([]);
  readonly fhirResourceCount = signal(0);
  readonly consented = signal(0);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.patientsApi.list().subscribe({
      next: page => {
        this.patients.set(page.content);
        if (!page.content.length) {
          this.loading.set(false);
          return;
        }
        forkJoin(page.content.map(patient => forkJoin({
          twin: this.twinApi.get(patient.id),
          vitals: this.vitalsApi.list(patient.id),
          labs: this.labApi.list(patient.id),
          fhir: this.fhirApi.list(patient.id),
          consent: this.consentApi.verify(patient.id)
        }))).subscribe({
          next: data => {
            this.twins.set(data.map(item => item.twin));
            this.recentVitals.set(data.flatMap(item => item.vitals.content).slice(0, 5));
            this.recentLabs.set(data.flatMap(item => item.labs.content).slice(0, 5));
            this.fhirResourceCount.set(data.reduce((total, item) => total + item.fhir.totalElements, 0));
            this.consented.set(data.filter(item => item.consent.active).length);
            this.loading.set(false);
          },
          error: () => { this.error.set('Clinical data could not be loaded.'); this.loading.set(false); }
        });
      },
      error: () => { this.error.set('Dashboard data could not be loaded.'); this.loading.set(false); }
    });
  }

  patientName(patientId: string): string {
    const patient = this.patients().find(item => item.id === patientId);
    return patient ? `${patient.firstName} ${patient.lastName}` : patientId;
  }
}
