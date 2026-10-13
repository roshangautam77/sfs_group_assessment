import { Routes } from '@angular/router';
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'tasks' },
  {
    path: 'tasks',
    loadComponent: () => import('./features/tasks/task-page').then((m) => m.TaskPage),
  },
  { path: '**', redirectTo: 'tasks' },
];
