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
    path: 'collectie/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/collection/collection-item-page').then((m) => m.CollectionItemPage),
    title: 'Release',
  },
  {
    path: 'collectie/:id/bewerken',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/add/manual/manual-release-page').then((m) => m.ManualReleasePage),
    title: 'Release bewerken',
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
      {
        path: 'zoeken',
        loadComponent: () => import('./features/add/search/search-page').then((m) => m.SearchPage),
        title: 'Zoeken',
      },
      {
        path: 'handmatig',
        loadComponent: () =>
          import('./features/add/manual/manual-release-page').then((m) => m.ManualReleasePage),
        title: 'Handmatig toevoegen',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
