import { Routes } from '@angular/router';
import { Checkout } from './pages/checkout/checkout';
import { AdminLogin } from './pages/admin-login/admin-login';
import { AdminDashboard } from './pages/admin-dashboard/admin-dashboard';
import { PaymentMethods } from './pages/payment-methods/payment-methods';
import { MerchantDetails } from './pages/merchant-details/merchant-details';
import { adminGuard } from './guards/admin.guard';
import { guestGuard } from './guards/guest.guard';
import { CryptoCheckout } from './crypto-checkout/crypto-checkout';
import { ChangePassword } from './pages/change-password/change-password';

export const routes: Routes = [
    { path: 'checkout/:uuid', component: Checkout },
    { path: 'checkout/:uuid/crypto', component: CryptoCheckout },

    { path: '', redirectTo: 'admin/login', pathMatch: 'full' },
    { path: 'admin', redirectTo: 'admin/dashboard', pathMatch: 'full' },

    { path: 'admin/login', component: AdminLogin, canActivate: [guestGuard] },
    { path: 'admin/change-password', component: ChangePassword },

    {
        path: 'admin/dashboard',
        component: AdminDashboard,
        canActivate: [adminGuard]
    },
    {
        path: 'admin/methods',
        component: PaymentMethods,
        canActivate: [adminGuard]
    },
    {
        path: 'admin/merchant/:id',
        component: MerchantDetails,
        canActivate: [adminGuard]
    }
];

