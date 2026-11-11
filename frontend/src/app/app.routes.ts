import { Routes } from '@angular/router';
import { AnnouncementList } from './pages/announcement-list/announcement-list';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'announcements' },
  { path: 'announcements', component: AnnouncementList, title: 'Announcements | SafePaws' },
  {
    path: 'announcements/new',
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
    loadComponent: () => import('./pages/map-page/map-page').then((module) => module.MapPage),
    title: 'Map | SafePaws',
  },
  { path: '**', redirectTo: 'announcements' },
];
