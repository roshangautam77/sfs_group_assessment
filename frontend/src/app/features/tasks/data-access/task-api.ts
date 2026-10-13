import { inject, Injectable, InjectionToken } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateTask, StatusFilter, Task } from '../domain/task';
export const TASKS_API_URL = new InjectionToken<string>('TASKS_API_URL', {
  providedIn: 'root',
  factory: () => '/api/tasks',
});
@Injectable({ providedIn: 'root' })
export class TaskApi {
  private readonly http = inject(HttpClient);
  private readonly url = inject(TASKS_API_URL);
  list(status: StatusFilter): Observable<readonly Task[]> {
    const params = status ? new HttpParams().set('status', status) : new HttpParams();
    return this.http.get<readonly Task[]>(this.url, { params });
  }
  create(task: CreateTask): Observable<Task> {
    return this.http.post<Task>(this.url, task);
  }
}
