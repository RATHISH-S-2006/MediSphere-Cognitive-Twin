import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { AuditEvent, Page } from '../../core/models/api.models';
import { AuditService } from '../../core/services/audit.service';

@Component({ selector: 'app-audit', standalone: true, imports: [CommonModule], templateUrl: './audit.component.html', styleUrl: './audit.component.scss' })
export class AuditComponent {
  private readonly auditApi = inject(AuditService);
  readonly events = signal<AuditEvent[]>([]); readonly loading = signal(true); readonly error = signal('');
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.error.set(''); this.auditApi.list().subscribe({ next: (page: Page<AuditEvent>) => { this.events.set(page.content); this.loading.set(false); }, error: response => { this.error.set(response.status === 403 ? 'Audit trail access is restricted to authorized operators.' : 'Audit events could not be loaded.'); this.loading.set(false); } }); }
}
