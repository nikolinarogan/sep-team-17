import { Routes } from '@angular/router';

import { RegisterComponent } from '../components/register/register';
import { LoginComponent } from '../components/login/login';
import { LandingComponent } from '../components/landing/landing';
import { ChangePasswordComponent } from '../components/change-password/change-password';
import { VehiclesComponent } from '../components/vehicles/vehicles';
import { VehicleFormComponent } from '../components/vehicle-form/vehicle-form';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: 'home', component: LandingComponent, canActivate: [authGuard] },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'change-password', component: ChangePasswordComponent },
  { path: 'vehicles', component: VehiclesComponent, canActivate: [adminGuard] },
  { path: 'vehicles/new', component: VehicleFormComponent, canActivate: [adminGuard] },
  { path: 'vehicles/edit/:id', component: VehicleFormComponent, canActivate: [adminGuard] },
  { path: '', redirectTo: '/register', pathMatch: 'full' },
];