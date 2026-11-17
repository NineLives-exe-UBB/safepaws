
import { Routes } from '@angular/router';
import { AnnouncementList } from './pages/announcement-list/announcement-list';
import { guestGuard, loggedInGuard } from './services/auth.guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'announcements' },

  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./pages/login/login').then((module) => module.Login),
    title: 'Log in | SafePaws',
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./pages/register/register').then((module) => module.Register),
    title: 'Create account | SafePaws',
  },
  {
    path: 'announcements',
    component: AnnouncementList,
    title: 'Announcements | SafePaws',
  },
  {
    path: 'announcements/new',
    canActivate: [loggedInGuard],
    loadComponent: () =>
      import('./pages/announcement-form/announcement-form').then(
        (module) => module.AnnouncementForm,
      ),
    title: 'Post announcement | SafePaws',
  },
  {
    path: 'announcements/:id',
    loadComponent: () =>
      import('./pages/announcement-details/announcement-details').then(
        (module) => module.AnnouncementDetails,
      ),
    title: 'Announcement | SafePaws',
  },
  {
    path: 'map',
    loadComponent: () =>
      import('./pages/map-page/map-page').then((module) => module.MapPage),
    title: 'Map | SafePaws',
  },
  { path: '**', redirectTo: 'announcements' },
];
