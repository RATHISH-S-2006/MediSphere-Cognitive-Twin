import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Alert, AlertSeverity, AlertStatus, PatientSummary } from '../../core/models/api.models';
import { AlertService } from '../../core/services/alert.service';
import { PatientService } from '../../core/services/patient.service';

@Component({ selector: 'app-monitoring', standalone: true, imports: [CommonModule], templateUrl: './monitoring.component.html', styleUrl: './monitoring.component.scss' })
export class MonitoringComponent {
  private readonly patientsApi = inject(PatientService); private readonly alertsApi = inject(AlertService);
  readonly patients = signal<PatientSummary[]>([]); readonly alerts = signal<Alert[]>([]); readonly loading = signal(true); readonly error = signal(''); readonly status = signal<'ALL' | AlertStatus>('ALL'); readonly severity = signal<'ALL' | AlertSeverity>('ALL'); readonly patientId = signal(''); readonly vital = signal(''); readonly action = signal('');
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.error.set(''); this.patientsApi.list().subscribe({ next: page => { this.patients.set(page.content); if (!page.content.length) { this.alerts.set([]); this.loading.set(false); return; } forkJoin(page.content.map(patient => this.alertsApi.list(patient.id))).subscribe({ next: pages => { this.alerts.set(pages.flatMap(page => page.content)); this.loading.set(false); }, error: () => { this.error.set('Monitoring data could not be loaded.'); this.loading.set(false); } }); }, error: () => { this.error.set('Patients could not be loaded.'); this.loading.set(false); } }); }
  filtered(): Alert[] { return this.alerts().filter(alert => (this.status() === 'ALL' || alert.status === this.status()) && (this.severity() === 'ALL' || alert.severity === this.severity()) && (!this.patientId() || alert.patientId === this.patientId()) && (!this.vital() || alert.vitalType.toLowerCase().includes(this.vital().toLowerCase()))); }
  count(status: AlertStatus): number { return this.alerts().filter(alert => alert.status === status).length; }
  updateFilter(name: 'status' | 'severity' | 'patientId' | 'vital', event: Event): void { const value = (event.target as HTMLSelectElement | HTMLInputElement).value; if (name === 'status') this.status.set(value as 'ALL' | AlertStatus); if (name === 'severity') this.severity.set(value as 'ALL' | AlertSeverity); if (name === 'patientId') this.patientId.set(value); if (name === 'vital') this.vital.set(value); }
  patientName(id: string): string { const patient = this.patients().find(item => item.id === id); return patient ? `${patient.firstName} ${patient.lastName}` : id; }
  change(alert: Alert, action: 'acknowledge' | 'resolve'): void { this.action.set(alert.id); const request = action === 'acknowledge' ? this.alertsApi.acknowledge(alert.id) : this.alertsApi.resolve(alert.id); request.subscribe({ next: updated => { this.alerts.set(this.alerts().map(item => item.id === updated.id ? updated : item)); this.action.set(''); }, error: () => { this.error.set('The alert lifecycle action could not be completed.'); this.action.set(''); } }); }
}
