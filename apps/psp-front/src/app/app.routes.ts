import { Routes } from '@angular/router';
import { Checkout } from './pages/checkout/checkout';
import { AdminLogin } from './pages/admin-login/admin-login';
import { AdminDashboard } from './pages/admin-dashboard/admin-dashboard';

export const routes: Routes = [
    { path: 'checkout/:uuid', component: Checkout },
    { path: '', redirectTo: 'checkout/test', pathMatch: 'full' },
    { path: 'admin/login', component: AdminLogin},
    { path: 'admin/dashboard', component: AdminDashboard },
];
