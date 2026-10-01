import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { PatientsComponent } from './features/patients/patients.component';
import { Patient360Component } from './features/patient-360/patient-360.component';
import { RiskComponent } from './features/risk/risk.component';
import { MonitoringComponent } from './features/monitoring/monitoring.component';
import { CarePlansComponent } from './features/care-plans/care-plans.component';
import { FhirComponent } from './features/fhir/fhir.component';
import { ConsentComponent } from './features/consent/consent.component';
import { AuditComponent } from './features/audit/audit.component';

export const routes: Routes = [
	{ path: '', component: DashboardComponent, canActivate: [authGuard] },
	{ path: 'patients', component: PatientsComponent, canActivate: [authGuard] },
	{ path: 'patients/:patientId', component: Patient360Component, canActivate: [authGuard] },
	{ path: 'risk', component: RiskComponent, canActivate: [authGuard] },
	{ path: 'monitoring', component: MonitoringComponent, canActivate: [authGuard] },
	{ path: 'care-plans', component: CarePlansComponent, canActivate: [authGuard] },
	{ path: 'fhir', component: FhirComponent, canActivate: [authGuard] },
	{ path: 'consent', component: ConsentComponent, canActivate: [authGuard] },
	{ path: 'audit', component: AuditComponent, canActivate: [authGuard] },
	{ path: '**', redirectTo: '' }
];
