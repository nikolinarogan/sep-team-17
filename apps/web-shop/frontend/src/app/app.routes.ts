import { Routes } from '@angular/router';

import { RegisterComponent } from '../components/register/register';
import { LoginComponent } from '../components/login/login';
import { LandingComponent } from '../components/landing/landing';
import { ChangePasswordComponent } from '../components/change-password/change-password';

export const routes: Routes = [
  { path: 'home', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'change-password', component: ChangePasswordComponent },
  { path: '', redirectTo: '/register', pathMatch: 'full' },
];