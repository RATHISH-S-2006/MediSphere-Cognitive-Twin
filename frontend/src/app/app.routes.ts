import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { PatientsComponent } from './features/patients/patients.component';
import { Patient360Component } from './features/patient-360/patient-360.component';

export const routes: Routes = [
	{ path: '', component: DashboardComponent, canActivate: [authGuard] },
	{ path: 'patients', component: PatientsComponent, canActivate: [authGuard] },
	{ path: 'patients/:patientId', component: Patient360Component, canActivate: [authGuard] },
	{ path: '**', redirectTo: '' }
];
