import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
    title: 'Muziekcollectie',
  },
  {
    path: 'collectie',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/collection/collection-page').then((m) => m.CollectionPage),
    title: 'Mijn collectie',
  },
  {
    path: 'toevoegen',
    canActivate: [authGuard],
    loadComponent: () => import('./features/add/add-page').then((m) => m.AddPage),
    title: 'Toevoegen',
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'scannen' },
      {
        path: 'scannen',
        loadComponent: () => import('./features/add/scan/scan-page').then((m) => m.ScanPage),
        title: 'Scannen',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
