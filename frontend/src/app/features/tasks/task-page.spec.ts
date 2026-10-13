import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { apiErrorInterceptor } from '../../core/http/api-error.interceptor';
import { TaskPage } from './task-page';
describe('TaskPage', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('renders loading, empty, and error states', () => {
    const fixture = TestBed.createComponent(TaskPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading tasks');
    http.expectOne('/api/tasks').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No tasks here yet');
    fixture.componentInstance.store.refresh();
    http.expectOne('/api/tasks').flush({}, { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'unavailable',
    );
  });
  it('renders returned tasks and resets the form only after a successful create', () => {
    const fixture = TestBed.createComponent(TaskPage);
    fixture.detectChanges();
    http.expectOne('/api/tasks').flush([]);
    fixture.detectChanges();
    const form = fixture.componentInstance.createForm()!;
    form.form.controls.title.setValue('Keep on failure');
    form.submit();
    http.expectOne('/api/tasks').flush({}, { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    expect(form.form.controls.title.value).toBe('Keep on failure');
    form.submit();
    const task = {
      id: '1',
      title: 'Keep on failure',
      description: null,
      status: 'TODO',
      priority: 'MEDIUM',
      assignee: null,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };
    http.expectOne('/api/tasks').flush(task);
    http.expectOne('/api/tasks').flush([task]);
    fixture.detectChanges();
    expect(form.form.controls.title.value).toBe('');
    expect(fixture.nativeElement.querySelector('tbody').textContent).toContain('Keep on failure');
    expect(fixture.nativeElement.querySelector('tbody').textContent).toContain('To do');
  });
});
