import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CreateTask, TASK_PRIORITIES, TaskPriority } from '../domain/task';
@Component({
  selector: 'app-task-create-form',
  imports: [ReactiveFormsModule],
  templateUrl: './task-create-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskCreateForm {
  readonly saving = input(false);
  readonly error = input<string | null>(null);
  readonly submitted = output<CreateTask>();
  readonly priorities = TASK_PRIORITIES;
  readonly form = new FormGroup({
    title: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)],
    }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(500)],
    }),
    priority: new FormControl<TaskPriority>('MEDIUM', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    assignee: new FormControl('', { nonNullable: true }),
  });
  submit(): void {
    if (this.saving()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.submitted.emit({
      ...value,
      title: value.title.trim(),
      description: value.description || null,
      assignee: value.assignee.trim() || null,
    });
  }
  reset(): void {
    this.form.reset({ title: '', description: '', priority: 'MEDIUM', assignee: '' });
  }
}
