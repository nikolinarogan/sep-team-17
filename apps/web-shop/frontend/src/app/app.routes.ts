import { Routes } from '@angular/router';

import { RegisterComponent } from '../components/register/register';
import { LoginComponent } from '../components/login/login';
import { LandingComponent } from '../components/landing/landing';
import { ChangePasswordComponent } from '../components/change-password/change-password';
import { VehiclesComponent } from '../components/vehicles/vehicles';
import { VehicleFormComponent } from '../components/vehicle-form/vehicle-form';
import { EquipmentComponent } from '../components/equipment/equipment';
import { EquipmentFormComponent } from '../components/equipment-form/equipment-form';
import { InsuranceComponent } from '../components/insurance/insurance';
import { InsuranceFormComponent } from '../components/insurance-form/insurance-form';
import { ServicesComponent } from '../components/services/services';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: 'home', component: LandingComponent, canActivate: [authGuard] },
  { path: 'services', component: ServicesComponent, canActivate: [authGuard] },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'change-password', component: ChangePasswordComponent },
  { path: 'vehicles', component: VehiclesComponent, canActivate: [adminGuard] },
  { path: 'vehicles/new', component: VehicleFormComponent, canActivate: [adminGuard] },
  { path: 'vehicles/edit/:id', component: VehicleFormComponent, canActivate: [adminGuard] },
  { path: 'equipment', component: EquipmentComponent, canActivate: [adminGuard] },
  { path: 'equipment/new', component: EquipmentFormComponent, canActivate: [adminGuard] },
  { path: 'equipment/edit/:id', component: EquipmentFormComponent, canActivate: [adminGuard] },
  { path: 'insurance', component: InsuranceComponent, canActivate: [adminGuard] },
  { path: 'insurance/new', component: InsuranceFormComponent, canActivate: [adminGuard] },
  { path: 'insurance/edit/:id', component: InsuranceFormComponent, canActivate: [adminGuard] },
  { path: '', redirectTo: '/register', pathMatch: 'full' },
];