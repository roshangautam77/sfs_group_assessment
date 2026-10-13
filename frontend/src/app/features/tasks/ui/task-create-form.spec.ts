import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { TaskCreateForm } from './task-create-form';
describe('TaskCreateForm', () => {
  it('rejects blank and overlong input and emits a trimmed valid task', () => {
    const fixture = TestBed.createComponent(TaskCreateForm);
    const form = fixture.componentInstance;
    const submitted = vi.fn();

    form.submitted.subscribe(submitted);
    form.form.controls.title.setValue('   ');
    form.submit();
    expect(submitted).not.toHaveBeenCalled();

    form.form.controls.title.setValue('x'.repeat(101));
    form.submit();
    expect(submitted).not.toHaveBeenCalled();

    form.form.controls.title.setValue('  Valid title  ');
    form.form.controls.description.setValue('x'.repeat(501));
    form.submit();
    expect(submitted).not.toHaveBeenCalled();
    
    form.form.controls.description.setValue('');
    form.submit();
    expect(submitted).toHaveBeenCalledWith({
      title: 'Valid title',
      priority: 'MEDIUM',
      description: null,
      assignee: null,
    });
  });
  it('disables controls during submission and preserves form data', () => {
    const fixture = TestBed.createComponent(TaskCreateForm);
    fixture.componentInstance.form.controls.title.setValue('Keep this');
    fixture.componentRef.setInput('saving', true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
    fixture.componentRef.setInput('saving', false);
    fixture.componentRef.setInput('error', 'Please retry');
    fixture.detectChanges();
    expect(fixture.componentInstance.form.controls.title.value).toBe('Keep this');
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'Please retry',
    );
  });
});
