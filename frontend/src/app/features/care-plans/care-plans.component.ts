import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { CarePlan, CarePlanStatus, PatientSummary } from '../../core/models/api.models';
import { CarePlanService } from '../../core/services/care-plan.service';
import { PatientService } from '../../core/services/patient.service';

@Component({ selector: 'app-care-plans', standalone: true, imports: [CommonModule], templateUrl: './care-plans.component.html', styleUrl: './care-plans.component.scss' })
export class CarePlansComponent {
  private readonly patientsApi = inject(PatientService); private readonly plansApi = inject(CarePlanService);
  readonly patients = signal<PatientSummary[]>([]); readonly plans = signal<CarePlan[]>([]); readonly selected = signal<CarePlan | null>(null); readonly loading = signal(true); readonly error = signal(''); readonly action = signal(''); readonly status = signal<'ALL' | CarePlanStatus>('ALL');
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.error.set(''); this.patientsApi.list().subscribe({ next: page => { this.patients.set(page.content); if (!page.content.length) { this.plans.set([]); this.loading.set(false); return; } forkJoin(page.content.map(patient => this.plansApi.list(patient.id))).subscribe({ next: pages => { this.plans.set(pages.flatMap(page => page.content)); this.loading.set(false); }, error: () => { this.error.set('Care plans could not be loaded.'); this.loading.set(false); } }); }, error: () => { this.error.set('Patients could not be loaded.'); this.loading.set(false); } }); }
  filtered(): CarePlan[] { return this.plans().filter(plan => this.status() === 'ALL' || plan.status === this.status()); }
  updateStatus(event: Event): void { this.status.set((event.target as HTMLSelectElement).value as 'ALL' | CarePlanStatus); }
  patientName(id: string): string { const patient = this.patients().find(item => item.id === id); return patient ? `${patient.firstName} ${patient.lastName}` : id; }
  transition(plan: CarePlan, target: 'activate' | 'pause' | 'complete' | 'cancel'): void { this.action.set(plan.id); this.plansApi.transition(plan.id, target).subscribe({ next: updated => { this.plans.set(this.plans().map(item => item.id === updated.id ? updated : item)); this.selected.set(updated); this.action.set(''); }, error: response => { this.error.set(response.status === 409 ? 'That care-plan transition is not allowed.' : 'The care plan could not be updated.'); this.action.set(''); } }); }
  mark(plan: CarePlan, interventionId: string, action: 'complete' | 'miss'): void { this.action.set(interventionId); this.plansApi.mark(plan.id, interventionId, action).subscribe({ next: updated => { this.plans.set(this.plans().map(item => item.id === updated.id ? updated : item)); this.selected.set(updated); this.action.set(''); }, error: () => { this.error.set('The intervention could not be updated.'); this.action.set(''); } }); }
}
