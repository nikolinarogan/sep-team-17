import { Routes } from '@angular/router';
import { Checkout } from './pages/checkout/checkout';
import { AdminLogin } from './pages/admin-login/admin-login';

export const routes: Routes = [
    { path: 'checkout/:uuid', component: Checkout },
    { path: '', redirectTo: 'checkout/test', pathMatch: 'full' },
    { path: 'admin/login', component: AdminLogin},
];
