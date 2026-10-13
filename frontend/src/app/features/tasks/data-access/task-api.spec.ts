import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ApiError, apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { TaskApi } from './task-api';
describe('TaskApi and HTTP error handling', () => {
  let api: TaskApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    api = TestBed.inject(TaskApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('omits the filter for all tasks and sends a selected status', () => {
    api.list('').subscribe((tasks) => expect(tasks).toEqual([]));
    http.expectOne('/api/tasks').flush([]);
    api.list('IN_PROGRESS').subscribe();
    const request = http.expectOne('/api/tasks?status=IN_PROGRESS');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
  it('posts the creation payload', () => {
    api.create({ title: 'Build feature', priority: 'HIGH' }).subscribe();
    const request = http.expectOne('/api/tasks');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ title: 'Build feature', priority: 'HIGH' });
    request.flush({});
  });
  it('preserves validation details in a typed error', () => {
    api.create({ title: '', priority: 'LOW' }).subscribe({
      error: (error: ApiError) => {
        expect(error).toBeInstanceOf(ApiError);
        expect(error.message).toBe('Request validation failed');
        expect(error.violations).toEqual([{ field: 'title', message: 'must not be blank' }]);
      },
    });
    http.expectOne('/api/tasks').flush(
      {
        detail: 'Request validation failed',
        errors: [{ field: 'title', message: 'must not be blank' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
  });
  it('does not display internal server error details', () => {
    api.list('').subscribe({
      error: (error: ApiError) =>
        expect(error.message).toBe('The task service is unavailable. Please try again.'),
    });
    http
      .expectOne('/api/tasks')
      .flush({ detail: 'Secret stack trace' }, { status: 500, statusText: 'Server Error' });
  });
  it('handles network errors and unexpected error shapes', () => {
    api.list('').subscribe({ error: (error: ApiError) => expect(error.status).toBe(0) });
    http.expectOne('/api/tasks').error(new ProgressEvent('error'));
    api.list('').subscribe({
      error: (error: ApiError) => expect(error.message).toBe('The request could not be completed.'),
    });
    http.expectOne('/api/tasks').flush('HTML response', { status: 404, statusText: 'Not Found' });
  });
});
