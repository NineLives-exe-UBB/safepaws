import { Routes } from '@angular/router';
import { AnnouncementForm } from './pages/announcement-form/announcement-form';
import { AnnouncementList } from './pages/announcement-list/announcement-list';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'announcements' },
  { path: 'announcements', component: AnnouncementList, title: 'Announcements | SafePaws' },
  { path: 'announcements/new', component: AnnouncementForm, title: 'Post announcement | SafePaws' },
  {
    path: 'map',
    loadComponent: () => import('./pages/map-page/map-page').then((module) => module.MapPage),
    title: 'Map | SafePaws',
  },
  { path: '**', redirectTo: 'announcements' },
];
