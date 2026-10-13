import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { TaskStore } from './task-store';
const task = {
  id: '1',
  title: 'Task',
  description: null,
  priority: 'HIGH',
  status: 'TODO',
  assignee: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};
describe('TaskStore', () => {
  let store: TaskStore;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TaskStore,
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    store = TestBed.inject(TaskStore);
  });
  afterEach(() => http.verify());
  it('loads tasks and cancels obsolete filter requests', () => {
    expect(store.list().loading).toBe(true);
    const initial = http.expectOne('/api/tasks');
    store.setFilter('DONE');
    expect(initial.cancelled).toBe(true);
    const done = http.expectOne('/api/tasks?status=DONE');
    store.setFilter('IN_PROGRESS');
    expect(done.cancelled).toBe(true);
    http.expectOne('/api/tasks?status=IN_PROGRESS').flush([]);
    expect(store.list()).toEqual({ tasks: [], loading: false, error: null });
  });
  it('keeps the stream alive after failure and supports retry', () => {
    http.expectOne('/api/tasks').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(store.list().error).toContain('unavailable');
    store.refresh();
    expect(store.list().loading).toBe(true);
    http.expectOne('/api/tasks').flush([task]);
    expect(store.list().tasks).toEqual([task]);
    expect(store.list().error).toBeNull();
  });
  it('prevents concurrent duplicate creates and reloads the current filter', () => {
    http.expectOne('/api/tasks').flush([]);
    store.setFilter('DONE');
    http.expectOne('/api/tasks?status=DONE').flush([]);
    store.create({ title: 'Task', priority: 'HIGH' });
    store.create({ title: 'Task', priority: 'HIGH' });
    expect(store.saving()).toBe(true);
    const requests = http.match((request) => request.method === 'POST');
    expect(requests).toHaveLength(1);
    requests[0].flush(task);
    expect(store.saving()).toBe(false);
    expect(store.createSuccess()).toBe(1);
    http.expectOne('/api/tasks?status=DONE').flush([]);
  });
  it('recovers from a failed create without reporting success', () => {
    http.expectOne('/api/tasks').flush([]);
    store.create({ title: 'Task', priority: 'LOW' });
    http.expectOne('/api/tasks').flush({}, { status: 500, statusText: 'Error' });
    expect(store.saving()).toBe(false);
    expect(store.createError()).toBeTruthy();
    expect(store.createSuccess()).toBe(0);
    store.create({ title: 'Retry', priority: 'LOW' });
    http.expectOne('/api/tasks').flush(task);
    http.expectOne('/api/tasks').flush([task]);
    expect(store.createError()).toBeNull();
    expect(store.createSuccess()).toBe(1);
  });
  it('cancels pending requests when the feature is destroyed', () => {
    const list = http.expectOne('/api/tasks');
    store.create({ title: 'Task', priority: 'LOW' });
    const create = http.expectOne('/api/tasks');
    TestBed.resetTestingModule();
    expect(list.cancelled).toBe(true);
    expect(create.cancelled).toBe(true);
  });
});
