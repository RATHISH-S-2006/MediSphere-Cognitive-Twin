import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PatientService } from '../../core/services/patient.service';
import { TwinService } from '../../core/services/twin.service';
import { ConsentService } from '../../core/services/consent.service';
import { PatientSummary, HealthTwin, ConsentVerification } from '../../core/models/api.models';

interface PatientRow extends PatientSummary { twin?: HealthTwin; consent?: ConsentVerification; }

@Component({ selector: 'app-patients', standalone: true, imports: [CommonModule, RouterLink], templateUrl: './patients.component.html', styleUrl: './patients.component.scss' })
export class PatientsComponent {
  private readonly patientApi = inject(PatientService);
  private readonly twinApi = inject(TwinService);
  private readonly consentApi = inject(ConsentService);
  readonly loading = signal(true); readonly error = signal(''); readonly query = signal(''); readonly rows = signal<PatientRow[]>([]);

  constructor() { this.load(); }
  load(): void {
    this.loading.set(true); this.error.set('');
    this.patientApi.list().subscribe({ next: page => {
      const rows: PatientRow[] = page.content.map(patient => ({ ...patient })); this.rows.set(rows);
      Promise.all(rows.map(async row => { try { row.twin = await this.twinApi.get(row.id).toPromise(); row.consent = await this.consentApi.verify(row.id).toPromise(); } catch { /* row-level data can remain unavailable */ } })).then(() => { this.rows.set([...rows]); this.loading.set(false); });
    }, error: () => { this.error.set('Patients could not be loaded.'); this.loading.set(false); } });
  }
  filtered(): PatientRow[] { const query = this.query().trim().toLowerCase(); return this.rows().filter(row => !query || `${row.firstName} ${row.lastName} ${row.id} ${row.mrn ?? ''}`.toLowerCase().includes(query)); }
}
